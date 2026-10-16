/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

package co.adilson889.nativelayout

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import co.adilson889.nativelayout.android.NativeView
import java.io.File

class DemoActivity : Activity() {

    private var ui: NativeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val area = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#0D1117")) }
        setContentView(area)

        val raiz = projectDir()
        val pedido = intent.getStringExtra("nl")
        val ficheiro = pedido?.let { File(raiz, it) }?.takeIf { it.isFile }
            ?: raiz.walkTopDown().firstOrNull { it.isFile && it.name == "tela.nl" }
        if (ficheiro == null) {
            area.addView(TextView(this).apply {
                text = "Cria o ficheiro tela.nl no editor e volta a abrir a demonstração."
                setTextColor(Color.parseColor("#C9D1D9"))
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
            }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            return
        }
        val pasta = ficheiro.parentFile ?: raiz

        ui = NativeView(this, area, { nome -> ler(raiz, pasta, nome) }) {
            val tela = screen(ficheiro.name)

            val tarefas = mutableListOf<MutableMap<String, Any?>>(
                mutableMapOf<String, Any?>("titulo" to "Testar o editor", "feito" to true),
                mutableMapOf<String, Any?>("titulo" to "Ligar o CSS", "feito" to true),
                mutableMapOf<String, Any?>("titulo" to "Testar toques e listas", "feito" to false)
            )
            var escuro = if (exists("escuro")) load("escuro") == "1" else true
            var notificar = load("notificar") == "1"
            var guardado = false

            while (render(tela, dados(tarefas, escuro, notificar, guardado))) {
                if (click("adicionar")) {
                    val titulo = value("titulo").trim()
                    if (titulo.isNotEmpty()) {
                        tarefas.add(mutableMapOf<String, Any?>("titulo" to titulo, "feito" to false))
                        setValue("titulo", "")
                        guardado = false
                    }
                }
                if (click(".toque")) {
                    tarefas.getOrNull(clickIndex())?.let { it["feito"] = it["feito"] != true }
                    guardado = false
                }
                if (click("escuro")) escuro = checked("escuro")
                if (click("guardar")) {
                    escuro = checked("escuro")
                    notificar = checked("notificar")
                    save("escuro", if (escuro) "1" else "0")
                    save("notificar", if (notificar) "1" else "0")
                    guardado = true
                }
            }
        }
    }

    override fun onDestroy() {
        ui?.close()
        super.onDestroy()
    }

    private fun dados(
        tarefas: List<Map<String, Any?>>,
        escuro: Boolean,
        notificar: Boolean,
        guardado: Boolean
    ): Map<String, Any?> {
        val feitas = tarefas.count { it["feito"] == true }
        val total = tarefas.size
        return mapOf(
            "usuario" to mapOf("inicial" to "A", "nome" to "Adilson", "cargo" to "CEO, Chorty Studios"),
            "estatisticas" to listOf(
                mapOf("rotulo" to "Total", "valor" to total),
                mapOf("rotulo" to "Por fazer", "valor" to total - feitas),
                mapOf("rotulo" to "Concluídas", "valor" to feitas)
            ),
            "progresso" to (if (total == 0) 0 else feitas * 100 / total),
            "pendentes" to total - feitas,
            "tarefas" to tarefas,
            "modo_escuro" to escuro,
            "notificacoes" to notificar,
            "guardado" to guardado
        )
    }

    private fun projectDir(): File {
        val base = getExternalFilesDir(null) ?: filesDir
        val dir = File(base, "nativelayout")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun ler(raiz: File, pasta: File, nome: String): String? {
        val base = raiz.canonicalFile
        for (origem in listOf(pasta, raiz)) {
            try {
                val f = File(origem, nome).canonicalFile
                if (f.isFile && f.path.startsWith(base.path + File.separator)) return f.readText()
            } catch (e: Exception) {
            }
        }
        return null
    }
}
