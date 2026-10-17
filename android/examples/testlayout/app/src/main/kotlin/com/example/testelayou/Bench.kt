package com.example.testelayou

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.util.Log
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import nativelayout.core.Expander
import nativelayout.core.LayoutDiffer
import nativelayout.core.LayoutEngine
import nativelayout.core.Native
import nativelayout.core.nativeChildren

/** Ferramentas de medição do teste de carga (HUD, pipeline do core, 1.º frame, FPS). */
class Bench(
    private val activity: Activity,
    private val area: FrameLayout,
    private val hud: TextView
) {
    private val lines = ArrayDeque<String>()

    private fun say(s: String) {
        Log.d("NLBench", s)
        activity.runOnUiThread {
            lines.addLast(s)
            while (lines.size > 14) lines.removeFirst()
            hud.text = lines.joinToString("\n")
        }
    }

    // ---------- 1) Pipeline do core, sem UI ----------
    private inline fun <T> timed(block: () -> T): Pair<T, Double> {
        val t = System.nanoTime()
        val r = block()
        return r to (System.nanoTime() - t) / 1e6
    }

    private fun count(n: Native): Int = 1 + n.nativeChildren().sumOf { count(it) }

    fun benchCore() {
        say("[core] a medir (mediana de 5 corridas, sem UI)...")
        fun load(f: String) = activity.assets.open(f).bufferedReader().use { it.readText() }
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
    fun armDraw(label: String) {
        val t0 = System.nanoTime()
        activity.runOnUiThread {
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
    fun fpsScroll() {
        activity.runOnUiThread {
            val sv = findScroll(area)
            if (sv == null) { say("[fps] ScrollView não encontrado"); return@runOnUiThread }
            val max = sv.getChildAt(0).height - sv.height
            if (max <= 0) { say("[fps] nada para rolar: aumenta o N"); return@runOnUiThread }

            val hz = activity.windowManager.defaultDisplay.refreshRate
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
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: Animator) {
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
}
