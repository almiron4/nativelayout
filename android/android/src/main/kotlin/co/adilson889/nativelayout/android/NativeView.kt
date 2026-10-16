/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

package co.adilson889.nativelayout.android

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.widget.FrameLayout
import android.widget.TextView
import co.adilson889.nativelayout.core.Expander
import co.adilson889.nativelayout.core.FilledMarkup
import co.adilson889.nativelayout.core.LayoutSession
import co.adilson889.nativelayout.core.NativeLayoutError
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class NativeView(
    context: Context,
    private val area: FrameLayout,
    private val loader: (String) -> String?,
    body: suspend NativeView.() -> Unit
) {
    class Screen internal constructor(val name: String, internal val source: String)

    private val host = AndroidLayoutHost(context, { area })
    private val session = LayoutSession(host)

    fun screen(name: String): Screen {
        val source = loader(name) ?: throw IllegalArgumentException("screen not found: $name")
        return Screen(name, source)
    }

    suspend fun render(screen: Screen, data: Map<String, Any?> = emptyMap()): Boolean {
        val expanded = Expander.expand(screen.source, data) { name -> loader(name) }
        return session.render(FilledMarkup(expanded.html, 1))
    }

    fun click(target: String): Boolean = session.click(target)
    fun clickIndex(): Int = session.clickIndex()
    fun value(id: String): String = session.fieldText(id) ?: ""
    fun checked(id: String): Boolean = session.fieldChecked(id) ?: false
    fun setValue(id: String, text: String) = session.setValue(id, text)

    fun save(key: String, text: String) = session.save(key, text)
    fun load(key: String): String? = session.load(key)
    fun exists(key: String): Boolean = session.exists(key)
    fun delete(key: String) = session.delete(key)

    fun close() = host.close()

    private fun showError(text: String) {
        area.removeAllViews()
        val padding = (16 * area.resources.displayMetrics.density).toInt()
        area.addView(TextView(area.context).apply {
            this.text = text
            setTextColor(Color.parseColor("#F85149"))
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setPadding(padding, padding, padding, padding)
        })
    }

    init {
        body.startCoroutine(this, object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                val error = result.exceptionOrNull() ?: return
                error.printStackTrace()
                showError(if (error is NativeLayoutError) error.format() else (error.message ?: error.toString()))
            }
        })
    }
}
