/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package co.adilson889.nativelayout

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import nativelayout.AndroidLayoutHost
import nativelayout.core.ExpandError
import nativelayout.core.Expander
import nativelayout.core.LayoutDiffer
import nativelayout.core.LayoutEngine
import nativelayout.core.Native
import nativelayout.core.NativeLayoutError
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

private enum class TipoIcone { MENU, PLAY, EXPLORAR }

private class IconeView(contexto: android.content.Context, private val tipo: TipoIcone, corIcone: Int = Color.WHITE) : View(contexto) {
    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = corIcone
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 2f
    }
    private val caminho = Path()

    override fun onDraw(canvas: Canvas) {
        val escala = minOf(width, height) / 24f
        canvas.save()
        canvas.translate((width - 24f * escala) / 2f, (height - 24f * escala) / 2f)
        canvas.scale(escala, escala)
        pincel.style = Paint.Style.STROKE
        caminho.reset()
        when (tipo) {
            TipoIcone.MENU -> {
                canvas.drawLine(4f, 6f, 20f, 6f, pincel)
                canvas.drawLine(4f, 12f, 20f, 12f, pincel)
                canvas.drawLine(4f, 18f, 20f, 18f, pincel)
            }
            TipoIcone.PLAY -> {
                caminho.moveTo(8f, 5f)
                caminho.lineTo(19f, 12f)
                caminho.lineTo(8f, 19f)
                caminho.close()
                pincel.style = Paint.Style.FILL_AND_STROKE
                canvas.drawPath(caminho, pincel)
            }
            TipoIcone.EXPLORAR -> {
                canvas.drawRoundRect(RectF(4f, 4f, 10.5f, 10.5f), 1.8f, 1.8f, pincel)
                canvas.drawRoundRect(RectF(13.5f, 4f, 20f, 10.5f), 1.8f, 1.8f, pincel)
                canvas.drawRoundRect(RectF(4f, 13.5f, 10.5f, 20f), 1.8f, 1.8f, pincel)
                canvas.drawRoundRect(RectF(13.5f, 13.5f, 20f, 20f), 1.8f, 1.8f, pincel)
            }
        }
        canvas.restore()
    }
}

class MainActivity : Activity() {

    private class Shortcut(val label: String, val text: String, val back: Int, val move: Int = 0)

    private lateinit var editor: CodeEditor
    private lateinit var problemsBar: ProblemsBar
    private lateinit var codeScreen: View
    private lateinit var previewScreen: FrameLayout
    private lateinit var codeTab: LinearLayout
    private lateinit var previewTab: LinearLayout
    private lateinit var dataTab: LinearLayout
    private lateinit var dataScreen: View
    private lateinit var dataEditor: EditText
    private val uiHandler = Handler(Looper.getMainLooper())
    private var dataCheck: Runnable? = null
    private lateinit var shortcutsBar: View

    private lateinit var painelLateral: LinearLayout
    private lateinit var scrimLateral: View
    private lateinit var arvoreWeb: WebView
    private var arvorePronta = false
    private var sidebarAberta = false
    private var arquivoAtual: String? = null
    private var arquivoCodigo: String? = null
    private var arquivoDados: String? = null
    private var ultimoNl: String? = null

    private var host: AndroidLayoutHost? = null
    private var lastTree: Native? = null
    private var cursorLine = 1
    private var editorFontSize = 14f

    private var pastaImportacaoAtual: String? = null
    private val CODIGO_IMPORTACAO = 4711

    private val paperColor = Color.parseColor("#F8F4EA")
    private val inkColor = Color.parseColor("#2B2822")
    private val lightColor = Color.parseColor("#F6F8FC")
    private val lightTextColor = Color.parseColor("#1F1F1F")
    private val blueColor = Color.parseColor("#0B57D0")
    private val blueLightColor = Color.parseColor("#C2E7FF")
    private val panelColor = Color.parseColor("#EFE9D8")
    private val drawerColor = Color.parseColor("#FCFBF6")
    private val codeFont: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    private val shortcuts = listOf(
        Shortcut("Tab", "    ", 0),
        Shortcut("<", "<", 0),
        Shortcut(">", ">", 0),
        Shortcut("/", "/", 0),
        Shortcut("\"", "\"\"", 1),
        Shortcut("=", "=", 0),
        Shortcut(":", ":", 0),
        Shortcut(";", ";", 0),
        Shortcut("#", "#", 0),
        Shortcut("{", "{}", 1),
        Shortcut("\u25C0", "", 0, -1),
        Shortcut("\u25B6", "", 0, 1),
        Shortcut("div", "<div></div>", 6),
        Shortcut("p", "<p></p>", 4),
        Shortcut("h1", "<h1></h1>", 5),
        Shortcut("span", "<span></span>", 7),
        Shortcut("button", "<button></button>", 9),
        Shortcut("input", "<input id=\"\">", 2),
        Shortcut("img", "<img src=\"\">", 2),
        Shortcut("ul", "<ul></ul>", 5),
        Shortcut("li", "<li></li>", 5)
    )

    companion object {
        private val EXTENSOES_ACEITES = Regex(
            "\\.(nl|css|json|md|txt|png|jpg|jpeg|gif|webp|bmp|svg)$",
            RegexOption.IGNORE_CASE
        )
        private val EXTENSOES_IMAGEM = Regex(
            "\\.(png|jpg|jpeg|gif|webp|bmp|svg)$",
            RegexOption.IGNORE_CASE
        )

    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(lightColor)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = lightColor
        window.navigationBarColor = lightColor
        var flags = window.decorView.systemUiVisibility
        if (Build.VERSION.SDK_INT >= 23) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.decorView.systemUiVisibility = flags

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }

        header.addView(botaoIcone(TipoIcone.MENU, "Menu") { alternarSidebar() })

        codeTab = createTab("Code")
        dataTab = createTab("Data")
        previewTab = createTab("Preview")
        header.addView(codeTab, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT))
        header.addView(dataTab, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT))
        header.addView(previewTab, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT))

        header.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(botaoIcone(TipoIcone.PLAY, "Executar", blueLightColor) {
            salvarAutomatico()
            val nl = if (arquivoCodigo?.endsWith(".nl", ignoreCase = true) == true) arquivoCodigo else ultimoNl
            startActivity(Intent(this@MainActivity, DemoActivity::class.java).putExtra("nl", nl))
        })

        root.addView(header, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))

        codeTab.setOnClickListener { showTab(0) }
        dataTab.setOnClickListener { showTab(1) }
        previewTab.setOnClickListener { showTab(2) }

        val body = FrameLayout(this)
        codeScreen = buildCodeScreen()
        previewScreen = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        dataScreen = buildDataScreen()
        body.addView(codeScreen)
        body.addView(dataScreen)
        body.addView(previewScreen)
        root.addView(body, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        shortcutsBar = buildShortcuts()
        root.addView(shortcutsBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40)))

        setContentView(buildRootWithSidebar(root))

        dataEditor.setText(loadData() ?: "")
        arquivoDados = if (File(projectDir(), "data.json").isFile) "data.json" else null
        val inicial = ficheiroInicial()
        arquivoCodigo = inicial
        ultimoNl = inicial
        arquivoAtual = inicial ?: arquivoDados
        editor.setText(inicial?.let { readProjectText(it) } ?: "")
        showTab(0)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buildRootWithSidebar(content: View): View {
        val outerRoot = FrameLayout(this)
        outerRoot.addView(content)

        scrimLateral = View(this).apply {
            setBackgroundColor(Color.parseColor("#66000000"))
            visibility = View.GONE
            setOnClickListener { fecharSidebar() }
        }
        outerRoot.addView(scrimLateral, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        painelLateral = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(drawerColor)
                val r = dp(16).toFloat()
                cornerRadii = floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
            }
            clipToOutline = true
            visibility = View.GONE
        }

        arvoreWeb = criarArvoreWeb()
        painelLateral.addView(arvoreWeb, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        outerRoot.addView(painelLateral, FrameLayout.LayoutParams(larguraSidebar(), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START))

        return outerRoot
    }

    private fun larguraSidebar(): Int = minOf(dp(304), (resources.displayMetrics.widthPixels * 0.86f).toInt())

    private fun alternarSidebar() {
        if (sidebarAberta) fecharSidebar() else abrirSidebar()
    }

    private fun abrirSidebar() {
        salvarAutomatico()
        carregarListaArquivos(true)
        val largura = larguraSidebar().toFloat()
        scrimLateral.animate().cancel()
        painelLateral.animate().cancel()
        scrimLateral.alpha = 0f
        scrimLateral.visibility = View.VISIBLE
        painelLateral.visibility = View.VISIBLE
        painelLateral.translationX = -largura
        scrimLateral.animate().alpha(1f).setDuration(300).start()
        painelLateral.animate().translationX(0f).setDuration(300).setInterpolator(DecelerateInterpolator(2f)).start()
        sidebarAberta = true
    }

    private fun fecharSidebar() {
        val largura = larguraSidebar().toFloat()
        sidebarAberta = false
        scrimLateral.animate().cancel()
        painelLateral.animate().cancel()
        scrimLateral.animate().alpha(0f).setDuration(220).start()
        painelLateral.animate().translationX(-largura).setDuration(220)
            .setInterpolator(AccelerateInterpolator(1.5f)).withEndAction {
                if (!sidebarAberta) {
                    painelLateral.visibility = View.GONE
                    scrimLateral.visibility = View.GONE
                }
            }.start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun criarArvoreWeb(): WebView {
        return WebView(this).apply {
            setBackgroundColor(drawerColor)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(PonteArvore(), "Ponte")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    arvorePronta = true
                    enviarEstadoArvore(true)
                }
            }
            loadUrl("file:///android_asset/editor/arvore.html")
        }
    }

    private fun carregarListaArquivos(reabrir: Boolean = false) {
        enviarEstadoArvore(reabrir)
    }

    private fun enviarEstadoArvore(reabrir: Boolean) {
        if (!arvorePronta) return
        thread(isDaemon = true) {
            try {
                val raiz = projectDir()
                val ficheiros = JSONObject()
                raiz.walkTopDown()
                    .filter { it.isFile && (it.name == ".keep" || EXTENSOES_ACEITES.containsMatchIn(it.name)) }
                    .forEach { ficheiro ->
                        try {
                            val rel = ficheiro.relativeTo(raiz).invariantSeparatorsPath
                            val conteudo = if (EXTENSOES_IMAGEM.containsMatchIn(ficheiro.name)) {
                                if (ficheiro.length() > 3_000_000L) "" else Base64.encodeToString(ficheiro.readBytes(), Base64.NO_WRAP)
                            } else {
                                ficheiro.readText()
                            }
                            ficheiros.put(rel, conteudo)
                        } catch (e: Exception) {}
                    }
                val estado = JSONObject()
                    .put("ficheiros", ficheiros)
                    .put("atual", arquivoAtual ?: JSONObject.NULL)
                    .put("reabrir", reabrir)
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        arvoreWeb.evaluateJavascript("definirEstado(" + JSONObject.quote(estado.toString()) + ")", null)
                    }
                }
            } catch (t: Throwable) {}
        }
    }

    inner class PonteArvore {

        @JavascriptInterface
        fun abrirFicheiro(caminho: String) {
            runOnUiThread {
                val f = resolverCaminho(caminho) ?: return@runOnUiThread
                if (!f.isFile) return@runOnUiThread
                salvarAutomatico()
                arquivoAtual = caminho
                val texto = try { f.readText() } catch (e: Exception) { "" }
                if (caminho.endsWith(".json")) {
                    arquivoDados = caminho
                    dataEditor.setText(texto)
                    showTab(1)
                } else {
                    arquivoCodigo = caminho
                    if (caminho.endsWith(".nl", ignoreCase = true)) ultimoNl = caminho
                    editor.setText(texto)
                    showTab(0)
                }
            }
        }

        @JavascriptInterface
        fun aplicarDiff(json: String) {
            runOnUiThread { aplicarDiffArvore(json) }
        }

        @JavascriptInterface
        fun salvar(): String {
            val nome = arquivoAtual
            if (nome.isNullOrBlank()) return "nome"
            salvarAutomatico()
            return "ok"
        }

        @JavascriptInterface
        fun salvarComo(caminho: String) {
            runOnUiThread {
                val f = resolverCaminho(caminho) ?: return@runOnUiThread
                f.parentFile?.mkdirs()
                try {
                    if (caminho.endsWith(".json")) {
                        f.writeText(dataEditor.text.toString())
                    } else {
                        f.writeText(editor.text.toString())
                    }
                    arquivoAtual = caminho
                    if (caminho.endsWith(".json")) arquivoDados = caminho else {
                        arquivoCodigo = caminho
                        if (caminho.endsWith(".nl", ignoreCase = true)) ultimoNl = caminho
                    }
                    enviarEstadoArvore(true)
                    Toast.makeText(this@MainActivity, "Salvo: " + f.name, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Falha ao salvar.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun importarDoDisco(pasta: String) {
            runOnUiThread { abrirSeletorImportacao(pasta) }
        }

        @JavascriptInterface
        fun fecharSidebar() {
            runOnUiThread { if (sidebarAberta) this@MainActivity.fecharSidebar() }
        }
    }

    private fun aplicarDiffArvore(json: String) {
        val diff = try { JSONObject(json) } catch (e: Exception) { return }
        val raiz = projectDir().canonicalFile
        val escrever = diff.optJSONObject("escrever")
        val apagar = diff.optJSONArray("apagar")
        val atualNovo = if (diff.has("atual") && !diff.isNull("atual")) diff.getString("atual") else null
        val escritos = mutableListOf<String>()

        escrever?.keys()?.forEach { k ->
            val f = resolverCaminho(k) ?: return@forEach
            f.parentFile?.mkdirs()
            val conteudo = escrever.getString(k)
            if (EXTENSOES_IMAGEM.containsMatchIn(k)) {
                try { f.writeBytes(Base64.decode(conteudo, Base64.DEFAULT)) } catch (e: Exception) {}
            } else {
                try { f.writeText(conteudo) } catch (e: Exception) {}
            }
            escritos.add(k)
        }

        for (i in 0 until (apagar?.length() ?: 0)) {
            val f = resolverCaminho(apagar!!.getString(i)) ?: continue
            if (f.isDirectory) f.deleteRecursively() else f.delete()
        }

        limparPastasVazias(raiz)

        val codigo = arquivoCodigo
        if (codigo != null && resolverCaminho(codigo)?.exists() != true) {
            if (atualNovo != null && !atualNovo.endsWith(".json") && resolverCaminho(atualNovo)?.isFile == true) {
                arquivoCodigo = atualNovo
            } else {
                arquivoCodigo = null
                editor.setText("")
            }
        }
        val dados = arquivoDados
        if (dados != null && resolverCaminho(dados)?.exists() != true) {
            arquivoDados = if (atualNovo != null && atualNovo.endsWith(".json") && resolverCaminho(atualNovo)?.isFile == true) atualNovo else null
        }
        arquivoAtual = if (atualNovo != null && resolverCaminho(atualNovo)?.exists() == true) atualNovo else (arquivoCodigo ?: arquivoDados)
    }

    private fun limparPastasVazias(raiz: File) {
        raiz.walkBottomUp()
            .filter { it.isDirectory && it != raiz && (it.listFiles()?.isEmpty() != false) }
            .forEach { it.delete() }
    }

    private fun abrirSeletorImportacao(pasta: String) {
        pastaImportacaoAtual = pasta
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, CODIGO_IMPORTACAO)
        } catch (e: Exception) {
            Toast.makeText(this, "Seletor indisponível.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun nomeDoUri(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx < 0 || !it.moveToFirst()) return null
            return it.getString(idx)
        }
    }

    private fun nomeUnicoNoDisco(pasta: File, nome: String): File {
        var alvo = File(pasta, nome)
        if (!alvo.exists()) return alvo
        val ponto = nome.lastIndexOf('.')
        val base = if (ponto > 0) nome.substring(0, ponto) else nome
        val ext = if (ponto > 0) nome.substring(ponto) else ""
        var n = 2
        while (alvo.exists()) {
            alvo = File(pasta, "$base ($n)$ext")
            n++
        }
        return alvo
    }

    private fun resolverCaminho(caminho: String): File? {
        if (caminho.isBlank()) return null
        return try {
            val raiz = projectDir().canonicalFile
            val f = File(raiz, caminho).canonicalFile
            if (f.path == raiz.path || f.path.startsWith(raiz.path + File.separator)) f else null
        } catch (e: Exception) {
            null
        }
    }

    private fun salvarAutomatico() {
        arquivoCodigo?.let { nome ->
            resolverCaminho(nome)?.let { f ->
                try { f.writeText(editor.text.toString()) } catch (e: Exception) {}
            }
        }
        arquivoDados?.let { nome ->
            resolverCaminho(nome)?.let { f ->
                try { f.writeText(dataEditor.text.toString()) } catch (e: Exception) {}
            }
        }
    }

    private fun createTab(title: String): LinearLayout {
        val tab = LinearLayout(this)
        tab.orientation = LinearLayout.VERTICAL
        val label = TextView(this).apply {
            text = title
            setTextColor(lightTextColor)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
        }
        tab.addView(label, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0, 1f))
        val indicator = View(this).apply { setBackgroundColor(blueColor) }
        tab.addView(indicator, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)).apply {
            setMargins(dp(10), 0, dp(10), 0)
        })
        return tab
    }

    private fun markTab(tab: LinearLayout, active: Boolean) {
        tab.getChildAt(0).alpha = if (active) 1f else 0.6f
        tab.getChildAt(1).visibility = if (active) View.VISIBLE else View.INVISIBLE
    }

    private fun showTab(index: Int) {
        codeScreen.visibility = if (index == 0) View.VISIBLE else View.GONE
        dataScreen.visibility = if (index == 1) View.VISIBLE else View.GONE
        previewScreen.visibility = if (index == 2) View.VISIBLE else View.GONE
        shortcutsBar.visibility = if (index == 0) View.VISIBLE else View.GONE
        markTab(codeTab, index == 0)
        markTab(dataTab, index == 1)
        markTab(previewTab, index == 2)
    }

    private fun buildDataScreen(): View {
        dataEditor = EditText(this).apply {
            setTextColor(inkColor)
            setBackgroundColor(paperColor)
            textSize = 13f
            typeface = codeFont
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
            setPadding(dp(16), dp(16), dp(16), dp(16))
            gravity = Gravity.TOP or Gravity.START
            isSingleLine = false
            maxLines = Integer.MAX_VALUE
        }
        dataEditor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                scheduleDataCheck()
            }
        })
        return dataEditor
    }

    private fun scheduleDataCheck() {
        dataCheck?.let { uiHandler.removeCallbacks(it) }
        val task = Runnable { atualizarPreview() }
        dataCheck = task
        uiHandler.postDelayed(task, 350L)
    }

    private fun buildCodeScreen(): View {
        val screen = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        editor = CodeEditor(this).apply {
            setTextColor(inkColor)
            setBackgroundColor(paperColor)
            textSize = editorFontSize
            typeface = codeFont
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
            setPadding(16, 32, 32, 32)
            gravity = Gravity.TOP or Gravity.START
            isSingleLine = false
            maxLines = Integer.MAX_VALUE
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            setHorizontallyScrolling(true)
            setTextIsSelectable(true)
        }
        editor.addTextChangedListener(MarkupHighlighter(editor) { text -> executarAnalise(text) })

        editor.addOnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
            val extraBottom = maxOf(dp(96), (bottom - top) / 4)
            val extraRight = maxOf(dp(96), (right - left) / 4)
            if (v.paddingBottom != extraBottom || v.paddingRight != extraRight) {
                v.setPadding(v.paddingLeft, v.paddingTop, extraRight, extraBottom)
            }
        }

        val zoomDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                editorFontSize = (editorFontSize * detector.scaleFactor).coerceIn(3f, 30f)
                editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, editorFontSize)
                return true
            }
        })
        editor.setOnTouchListener { _, event ->
            zoomDetector.onTouchEvent(event)
            zoomDetector.isInProgress
        }

        screen.addView(editor, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        problemsBar = ProblemsBar(this).apply {
            onTap = { current?.let { editor.goToLine(it.line, it.start) } }
        }
        screen.addView(problemsBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        editor.onDiagnosticsChanged = { list, outdated ->
            problemsBar.show(list, cursorLine, outdated)
        }
        editor.onLineChanged = { line ->
            cursorLine = line
            problemsBar.show(editor.diagnostics, line, editor.outdated)
        }
        editor.onMarkerTapped = { list, x, yBelow, yAbove ->
            DiagnosticBalloon.show(editor, list, x, yBelow, yAbove)
        }

        return screen
    }

    private fun buildShortcuts(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(panelColor)
        }
        for (item in shortcuts) {
            val button = TextView(this).apply {
                text = item.label
                textSize = 14f
                typeface = Typeface.MONOSPACE
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(lightTextColor)
                gravity = Gravity.CENTER
                minWidth = dp(40)
                setPadding(dp(10), 0, dp(10), 0)
                isClickable = true
                isFocusable = false
                setOnClickListener { applyShortcut(item) }
            }
            row.addView(button, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT))
        }
        return HorizontalScrollView(this).apply {
            isFillViewport = true
            isHorizontalScrollBarEnabled = false
            isFocusable = false
            addView(row)
        }
    }

    private fun applyShortcut(item: Shortcut) {
        val content = editor.text ?: return
        if (item.move != 0) {
            val target = (editor.selectionEnd + item.move).coerceIn(0, content.length)
            editor.setSelection(target)
            return
        }
        val start = minOf(editor.selectionStart, editor.selectionEnd).coerceAtLeast(0)
        val end = maxOf(editor.selectionStart, editor.selectionEnd).coerceAtLeast(0)
        content.replace(start, end, item.text)
        editor.setSelection((start + item.text.length - item.back).coerceIn(0, content.length))
        if (!editor.hasFocus()) editor.requestFocus()
    }

    private fun ensureHost(): AndroidLayoutHost {
        host?.let { return it }
        val created = AndroidLayoutHost(this, { previewScreen }, { source -> loadProjectImage(source) })
        host = created
        listenToTaps(created)
        return created
    }

    private fun listenToTaps(target: AndroidLayoutHost) {
        val body: suspend () -> Unit = {
            while (true) {
                val event = target.nextEvent() ?: break
                Toast.makeText(this@MainActivity, "Tapped " + event.key, Toast.LENGTH_SHORT).show()
            }
        }
        body.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                result.exceptionOrNull()?.printStackTrace()
            }
        })
    }

    private fun sourceLine(map: List<Int>?, line: Int): Int = map?.getOrNull(line - 1) ?: line

    private fun atualizarPreview() {
        if (arquivoCodigo?.endsWith(".nl", ignoreCase = true) == true) {
            executarAnalise(editor.text.toString())
            return
        }
        val nl = ultimoNl?.takeIf { resolverCaminho(it)?.isFile == true } ?: return
        salvarAutomatico()
        val texto = readProjectText(nl) ?: return
        executarAnalise(texto, true)
    }

    private fun executarAnalise(text: String, externo: Boolean = false) {
        if (!externo && arquivoCodigo?.endsWith(".nl", ignoreCase = true) != true) {
            editor.clearDiagnostics()
            return
        }
        val found = ArrayList<Diagnostic>()
        var lineMap: List<Int>? = null
        try {
            val html: String
            if (text.trimStart().startsWith("<nativelayout")) {
                val pasta = (if (externo) ultimoNl else arquivoCodigo)?.substringBeforeLast('/', "") ?: ""
                val expanded = Expander.expand(text, DataJson.parse(dataEditor.text.toString())) { name ->
                    (if (pasta.isEmpty()) null else readProjectText(pasta + "/" + name)) ?: readProjectText(name)
                }
                html = expanded.html
                lineMap = expanded.lineMap
            } else {
                html = text
            }
            val result = LayoutEngine.build(html)
            val changes = LayoutDiffer.diff(lastTree, result.root)
            ensureHost().applyChanges(changes)
            lastTree = result.root
            for (w in result.warnings) {
                found.add(Diagnostic(sourceLine(lineMap, w.line), Severity.WARNING, w.message))
            }
        } catch (e: ExpandError) {
            found.add(toDiagnostic(text, e, null))
        } catch (e: NativeLayoutError) {
            found.add(toDiagnostic(text, e, lineMap))
        } catch (e: JSONException) {
            found.add(Diagnostic(1, Severity.ERROR, "Data tab: invalid JSON (" + (e.message ?: "") + ")"))
        } catch (e: Exception) {
            found.add(Diagnostic(1, Severity.ERROR, e.message ?: "Unexpected error"))
        }
        if (externo) {
            found.firstOrNull { it.severity == Severity.ERROR }?.let {
                Toast.makeText(this, "L" + it.line + ": " + it.message, Toast.LENGTH_LONG).show()
            }
        } else if (editor.text.toString() == text) {
            editor.setDiagnostics(found.sortedWith(compareBy({ it.line }, { it.severity != Severity.ERROR })))
        }
    }

    private fun toDiagnostic(text: String, e: NativeLayoutError, map: List<Int>?): Diagnostic {
        val line = sourceLine(map, e.line)
        val range = wordRange(text, line, e.word)
        return Diagnostic(line, Severity.ERROR, e.message, range?.first ?: -1, range?.second ?: -1, e.suggestion)
    }

    private fun wordRange(text: String, line: Int, word: String?): Pair<Int, Int>? {
        if (word.isNullOrEmpty() || line < 1) return null
        var lineStart = 0
        var n = 1
        while (n < line) {
            val p = text.indexOf('\n', lineStart)
            if (p < 0) return null
            lineStart = p + 1
            n++
        }
        var lineEnd = text.indexOf('\n', lineStart)
        if (lineEnd < 0) lineEnd = text.length
        val pos = text.indexOf(word, lineStart)
        return if (pos in lineStart until lineEnd && pos + word.length <= lineEnd) Pair(pos, pos + word.length) else null
    }

    private fun projectDir(): File {
        val base = getExternalFilesDir(null) ?: filesDir
        val dir = File(base, "nativelayout")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun ficheiroInicial(): String? {
        val raiz = projectDir()
        return raiz.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".nl", ignoreCase = true) }
            .map { it.relativeTo(raiz).invariantSeparatorsPath }
            .sorted()
            .firstOrNull()
    }

    private fun loadData(): String? {
        return try {
            File(projectDir(), "data.json").takeIf { it.isFile }?.readText()
        } catch (e: Exception) {
            null
        }
    }

    private fun readProjectText(name: String): String? {
        return try {
            val root = projectDir().canonicalFile
            val file = File(root, name).canonicalFile
            if (!file.path.startsWith(root.path + File.separator) || !file.isFile) null else file.readText()
        } catch (e: Exception) {
            null
        }
    }

    private fun loadProjectImage(source: String): Bitmap? {
        val root = projectDir().canonicalFile
        val file = File(root, source).canonicalFile
        if (!file.path.startsWith(root.path + File.separator) || !file.isFile) return null
        return BitmapFactory.decodeFile(file.path)
    }

    private fun botaoIcone(tipo: TipoIcone, descricao: String, corFundo: Int? = null, aoClicar: () -> Unit): FrameLayout {
        val caixa = FrameLayout(this).apply {
            contentDescription = descricao
            isClickable = true
            isFocusable = true
            setOnClickListener { aoClicar() }
        }
        caixa.addView(IconeView(this, tipo, if (corFundo != null) Color.parseColor("#001D35") else Color.parseColor("#444746")), FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
        if (corFundo != null) {
            caixa.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(corFundo)
            }
            caixa.layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(dp(4), 0, dp(4), 0) }
        } else {
            caixa.layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { setMargins(dp(3), 0, dp(3), 0) }
        }
        return caixa
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != CODIGO_IMPORTACAO) return
        if (resultCode != RESULT_OK) return
        val dados = data ?: return
        val destinoRel = pastaImportacaoAtual
        val destino = if (destinoRel.isNullOrBlank()) projectDir() else resolverCaminho(destinoRel) ?: projectDir()
        val uris = mutableListOf<Uri>()
        val clip = dados.clipData
        if (clip != null) {
            for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
        }
        dados.data?.let { uris.add(it) }
        if (uris.isEmpty()) return
        thread(isDaemon = true) {
            var copiados = 0
            uris.forEach { uri ->
                try {
                    val nome = nomeDoUri(uri) ?: return@forEach
                    val alvo = nomeUnicoNoDisco(destino, nome)
                    contentResolver.openInputStream(uri)?.use { entrada ->
                        alvo.outputStream().use { saida -> entrada.copyTo(saida) }
                        copiados++
                    }
                } catch (e: Exception) {}
            }
            runOnUiThread {
                if (copiados > 0) {
                    Toast.makeText(this@MainActivity, "$copiados ficheiro(s) importado(s).", Toast.LENGTH_SHORT).show()
                    enviarEstadoArvore(true)
                } else {
                    Toast.makeText(this@MainActivity, "Nada foi importado.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (sidebarAberta) {
            arvoreWeb.evaluateJavascript("window.voltarNaArvore && window.voltarNaArvore()") { r ->
                if (r != "true") fecharSidebar()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarPreview()
    }

    override fun onPause() {
        salvarAutomatico()
        DiagnosticBalloon.dismiss()
        super.onPause()
    }
}