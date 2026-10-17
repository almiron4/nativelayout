package com.example.testelayou


import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView

import nativelayout.NativeView
import nativelayout.core.Expander
import nativelayout.core.LayoutDiffer
import nativelayout.core.LayoutEngine
import nativelayout.core.Native
import nativelayout.core.nativeChildren



class MainActivity : Activity() {
    private lateinit var ui: NativeView
    private lateinit var area: FrameLayout
    private lateinit var hud: TextView
    private val lines = ArrayDeque<String>()

    private val sizes = listOf(10, 100, 500, 1000, 2000)

    // ---------- HUD ----------
    private fun say(s: String) {
        Log.d("NLBench", s)
        runOnUiThread {
            lines.addLast(s)
            while (lines.size > 14) lines.removeFirst()
            hud.text = lines.joinToString("\n")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        area = FrameLayout(this)
        root.addView(area, FrameLayout.LayoutParams(-1, -1))
        hud = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 10f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xDD000000.toInt())
            setPadding(16, 8, 16, 8)
        }
        root.addView(hud, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)

        ui = NativeView(
            this,
            area,
            { name -> runCatching { assets.open(name).bufferedReader().use { it.readText() } }.getOrNull() }
        ) {
            val screen = screen("stress.nl")
            var n = 100
            var qtd = IntArray(n)
            var changed = true
            var label = "abrir N=$n"

            while (true) {
                if (changed) armDraw(label)
                changed = false
                if (!render(screen, stressData(n, qtd))) break

                when {
                    click(".chip") -> { n = sizes[clickIndex()]; qtd = IntArray(n); changed = true; label = "trocar para N=$n" }
                    click(".mais") -> { qtd[clickIndex()]++; changed = true; label = "+1 num item" }
                    click("mudar") -> { qtd[n / 2]++; changed = true; label = "mudar o item do meio" }
                    click("rolar") -> runOnUiThread { fpsScroll() }
                    click("bench") -> Thread { benchCore() }.start()
                }
            }
        }
    }

    override fun onDestroy() {
        ui.close()
        super.onDestroy()
    }

    // ---------- Dados ----------
    private fun stressData(n: Int, qtd: IntArray): Map<String, Any?> = mapOf(
        "total" to n,
        "nos" to n * 8,
        "tamanhos" to sizes.map { mapOf("nome" to it.toString(), "ativo" to (it == n)) },
        "itens" to List(n) { i ->
            mapOf(
                "inicial" to ('A' + i % 26).toString(),
                "nome" to "Item ${i + 1}",
                "desc" to "Descrição do item número ${i + 1}",
                "preco" to "${1 + i % 9},${"%02d".format((i * 7) % 100)} €",
                "qtd" to qtd[i]
            )
        }
    )

    private val menuItems = listOf(
        arrayOf("Espresso", "Intenso e curto", "1,50 €"),
        arrayOf("Cappuccino", "Leite vaporizado e cacau", "2,80 €"),
        arrayOf("Cold brew", "Infusão a frio, 12 horas", "3,20 €"),
        arrayOf("Iced latte", "Com gelo e leite", "3,40 €"),
        arrayOf("Pastel de nata", "Quente, com canela", "1,30 €"),
        arrayOf("Cheesecake", "Frutos vermelhos", "3,90 €")
    )

    private fun menuData(q: Int): Map<String, Any?> = mapOf(
        "saudacao" to "Bom dia",
        "usuario" to mapOf("inicial" to "A"),
        "categorias" to listOf("Todos", "Quentes", "Frios", "Doces")
            .mapIndexed { i, nome -> mapOf("nome" to nome, "ativo" to (i == 0)) },
        "destaque" to mapOf("nome" to "Cappuccino", "descricao" to "Leite vaporizado e cacau", "preco" to "2,80 €"),
        "secao" to "Todo o menu",
        "n_itens" to 6,
        "produtos" to menuItems.mapIndexed { i, p ->
            mapOf("inicial" to p[0].take(1), "nome" to p[0], "descricao" to p[1], "preco" to p[2], "qtd" to (if (i == 1) q else 0))
        },
        "itens" to q,
        "itens_txt" to "$q itens",
        "total" to "0,00 €",
        "enviado" to false
    )

    // ---------- 1) Pipeline do core, sem UI ----------
    private inline fun <T> timed(block: () -> T): Pair<T, Double> {
        val t = System.nanoTime()
        val r = block()
        return r to (System.nanoTime() - t) / 1e6
    }

    private fun count(n: Native): Int = 1 + n.nativeChildren().sumOf { count(it) }

    private fun benchCore() {
        say("[core] a medir (mediana de 5 corridas, sem UI)...")
        fun load(f: String) = assets.open(f).bufferedReader().use { it.readText() }
        val resolve = { name: String -> runCatching { load(name) }.getOrNull() }
        try {
            bench("menu.nl", load("menu.nl"), menuData(0), menuData(2), resolve)
            val stress = load("stress.nl")
            for (n in sizes) {
                val q2 = IntArray(n).also { it[n / 2] = 1 }
                bench("stress N=$n", stress, stressData(n, IntArray(n)), stressData(n, q2), resolve)
            }
        } catch (e: Throwable) {
            say("[core] erro: ${e.message}")
        }
        say("[core] fim")
    }

    private fun bench(
        label: String, src: String,
        d1: Map<String, Any?>, d2: Map<String, Any?>,
        resolve: (String) -> String?
    ) {
        val ex = ArrayList<Double>()
        val bu = ArrayList<Double>()
        val df = ArrayList<Double>()
        var nodes = 0
        var kb = 0
        var changes = 0
        repeat(7) { i ->
            val (e1, te) = timed { Expander.expand(src, d1, resolve) }
            val (r1, tb) = timed { LayoutEngine.build(e1.html) }
            val r2 = LayoutEngine.build(Expander.expand(src, d2, resolve).html)
            val (ch, td) = timed { LayoutDiffer.diff(r1.root, r2.root) }
            if (i >= 2) { ex += te; bu += tb; df += td }
            nodes = count(r1.root); kb = e1.html.length / 1024; changes = ch.size
        }
        val m = { l: List<Double> -> l.sorted()[l.size / 2] }
        say(
            "%-12s nós=%d %dKB | expand %.0f + build %.0f = %.0f ms | diff 1 mud. %.1f ms (%d)"
                .format(label, nodes, kb, m(ex), m(bu), m(ex) + m(bu), m(df), changes)
        )
    }

    // ---------- 2) Tempo até ao 1.º frame depois de um toque ----------
    private fun armDraw(label: String) {
        val t0 = System.nanoTime()
        runOnUiThread {
            area.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    area.viewTreeObserver.removeOnPreDrawListener(this)
                    val ms = (System.nanoTime() - t0) / 1e6
                    say("[ui] %-22s %.0f ms até ao 1.º frame | views=%d".format(label, ms, countViews(area)))
                    return true
                }
            })
        }
    }

    private fun countViews(v: View): Int =
        1 + if (v is ViewGroup) (0 until v.childCount).sumOf { countViews(v.getChildAt(it)) } else 0

    // ---------- 3) FPS durante um scroll automático ----------
    private fun findScroll(v: View): ScrollView? {
        if (v is ScrollView) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) findScroll(v.getChildAt(i))?.let { return it }
        return null
    }

    @Suppress("DEPRECATION")
    private fun fpsScroll() {
        val sv = findScroll(area)
        if (sv == null) { say("[fps] ScrollView não encontrado"); return }
        val max = sv.getChildAt(0).height - sv.height
        if (max <= 0) { say("[fps] nada para rolar: aumenta o N"); return }

        val hz = windowManager.defaultDisplay.refreshRate
        val budget = 1000f / hz
        val deltas = ArrayList<Float>()
        var last = 0L
        val cb = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (last != 0L) deltas.add((frameTimeNanos - last) / 1e6f)
                last = frameTimeNanos
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        say("[fps] a rolar %d px (a 2000 px/s)...".format(max))
        sv.scrollTo(0, 0)
        Choreographer.getInstance().postFrameCallback(cb)
        ValueAnimator.ofInt(0, max).apply {
            duration = (max / 2L).coerceIn(2000L, 20000L)
            interpolator = LinearInterpolator()
            addUpdateListener { sv.scrollTo(0, it.animatedValue as Int) }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    Choreographer.getInstance().removeFrameCallback(cb)
                    if (deltas.isEmpty()) { say("[fps] sem frames medidos"); return }
                    val s = deltas.sorted()
                    val avg = s.average()
                    val p95 = s[(s.size * 0.95).toInt().coerceAtMost(s.size - 1)]
                    val jank = s.count { it > budget * 1.5f }
                    val severe = s.count { it > budget * 3f }
                    say(
                        "[fps] %d frames | média %.0f fps | p95 %.1f ms | pior %.0f ms | lentos %d | graves %d | ecrã %.0f Hz"
                            .format(s.size, 1000.0 / avg, p95, s.last(), jank, severe, hz)
                    )
                }
            })
            start()
        }
    }
}
