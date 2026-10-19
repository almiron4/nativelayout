/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package co.adilson889.nativelayout

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import nativelayout.NativeView
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

        val dados: Map<String, Any?> = try {
            val nomeDados = intent.getStringExtra("dados")
            val jsonFile = nomeDados?.let { File(raiz, it) }?.takeIf { it.isFile }
                ?: File(pasta, "data.json").takeIf { it.isFile }
                ?: File(raiz, "data.json").takeIf { it.isFile }
            if (jsonFile == null) emptyMap() else DataJson.parse(jsonFile.readText())
        } catch (e: Exception) {
            area.addView(TextView(this).apply {
                text = "Data: JSON inválido (" + (e.message ?: "") + ")"
                setTextColor(Color.parseColor("#F85149"))
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
            }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            return
        }

        ui = NativeView(this, area, { nome -> ler(raiz, pasta, nome) }) {
            val tela = screen(ficheiro.name)

            fun copiar(lista: Any?): MutableList<MutableMap<String, Any?>> {
                val out = ArrayList<MutableMap<String, Any?>>()
                if (lista is List<*>) for (e in lista) {
                    if (e is Map<*, *>) {
                        val m = LinkedHashMap<String, Any?>()
                        for ((k, v) in e) m[k.toString()] = v
                        out.add(m)
                    }
                }
                return out
            }

            val todos = copiar(dados["itens"])
            val tamanhos = copiar(dados["tamanhos"])
            val totalJson = (dados["total"] as? Number)?.toDouble()
            val nosJson = (dados["nos"] as? Number)?.toDouble()
            val nosPorItem = if (totalJson != null && totalJson > 0 && nosJson != null) nosJson / totalJson else 8.0

            var n = tamanhos.firstOrNull { it["ativo"] == true }
                ?.get("nome")?.toString()?.toIntOrNull() ?: todos.size

            fun montar(): Map<String, Any?> {
                val vis = todos.subList(0, minOf(n, todos.size))
                val m = LinkedHashMap<String, Any?>(dados)
                m["itens"] = vis
                m["tamanhos"] = tamanhos
                m["total"] = vis.size
                m["nos"] = Math.round(vis.size * nosPorItem).toInt()
                return m
            }

            fun somar(i: Int) {
                val item = todos.getOrNull(i) ?: return
                item["qtd"] = ((item["qtd"] as? Number)?.toInt() ?: 0) + 1
            }

            while (render(tela, montar())) {
                if (click(".mais")) somar(clickIndex())
                if (click("mudar")) somar(0)
                if (click(".chip")) {
                    val i = clickIndex()
                    val novo = tamanhos.getOrNull(i)?.get("nome")?.toString()?.toIntOrNull()
                    if (novo != null) {
                        n = novo
                        for ((k, t) in tamanhos.withIndex()) t["ativo"] = (k == i)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        ui?.close()
        super.onDestroy()
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
