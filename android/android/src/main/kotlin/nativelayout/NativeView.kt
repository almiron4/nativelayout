/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.TextView
import nativelayout.core.Expander
import nativelayout.core.FilledMarkup
import nativelayout.core.LayoutSession
import nativelayout.core.NativeLayoutError
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

/** What a screen function returns: a screen to show ([NativeView.view]) or a jump to another one ([NativeView.go]). */
sealed class Page

internal class ViewPage(val screen: NativeView.Screen, val data: Map<String, Any?>) : Page()
internal class GoPage(val next: NativeView.() -> Page) : Page()

class NativeView(
    context: Context,
    private val area: FrameLayout,
    private val loader: (String) -> String?,
    body: suspend NativeView.() -> Unit
) {
    class Screen internal constructor(val name: String, internal val source: String)

    /**
     * Screen-function mode: [app] is called, what it returns is shown, and it is called
     * again after every tap, until the window closes. Use named arguments.
     */
    constructor(
        context: Context,
        area: FrameLayout,
        app: NativeView.() -> Page,
        loader: (String) -> String?
    ) : this(context, area, loader, { runApp(app) })

    private val host = AndroidLayoutHost(context, { area })
    private val session = LayoutSession(host)

    // One worker, so the heavy steps of two renders never overlap.
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "nativelayout-prepare").apply { isDaemon = true }
    }
    private val ui = Handler(Looper.getMainLooper())

    @Volatile
    private var closed = false

    // Screen-function mode. Declared before init, which may run the first call at once.
    private val screens = HashMap<String, Screen>()
    private val slots = ArrayList<Any?>()
    private var slotIndex = 0
    private var fresh = false

    fun screen(name: String): Screen {
        val source = loader(name) ?: throw IllegalArgumentException("screen not found: $name")
        return Screen(name, source)
    }

    /** Runs [block] on the worker thread and resumes the caller on the UI thread. */
    private suspend fun <T> inBackground(block: () -> T): T = suspendCoroutine { cont ->
        worker.execute {
            val result = runCatching(block)
            ui.post { cont.resumeWith(result) }
        }
    }

    /**
     * expand + build + diff run off the UI thread; only applyChanges runs on it.
     * Note: [loader] is now called from the worker thread.
     */
    suspend fun render(screen: Screen, data: Map<String, Any?> = emptyMap()): Boolean {
        if (closed) return false
        val prepared = inBackground {
            val expanded = Expander.expand(screen.source, data) { name -> loader(name) }
            session.prepare(FilledMarkup(expanded.html, 1))
        }
        return session.commit(prepared)
    }

    /**
     * State of the active screen function: the same object on every call, a new one after [go].
     * Call it at the top, always in the same order, never inside if or for.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> state(initial: () -> T): T {
        val i = slotIndex++
        if (i == slots.size) slots.add(initial())
        return slots[i] as T
    }

    /** The screen to show with its data. The screen file is read once and kept. */
    fun view(name: String, data: Map<String, Any?> = emptyMap()): Page =
        ViewPage(screens.getOrPut(name) { screen(name) }, data)

    /** Jump to another screen function. The state of the current one is dropped. */
    fun go(next: NativeView.() -> Page): Page = GoPage(next)

    private suspend fun runApp(app: NativeView.() -> Page) {
        var current = app
        var hops = 0
        while (!closed) {
            slotIndex = 0
            when (val page = current()) {
                is GoPage -> {
                    hops++
                    if (hops > 8) throw IllegalStateException("go: too many jumps without showing a screen")
                    current = page.next
                    slots.clear()
                    fresh = true
                    session.reset()
                }
                is ViewPage -> {
                    hops = 0
                    if (!render(page.screen, page.data)) return
                    fresh = false
                }
            }
        }
    }

    // After a jump there is no tap for the new screen yet, so these answer "nothing" until it renders.
    fun click(target: String): Boolean = !fresh && session.click(target)
    fun clickIndex(): Int = if (fresh) -1 else session.clickIndex()
    fun value(id: String): String = if (fresh) "" else session.fieldText(id) ?: ""
    fun checked(id: String): Boolean = if (fresh) false else session.fieldChecked(id) ?: false
    fun setValue(id: String, text: String) = session.setValue(id, text)

    fun save(key: String, text: String) = session.save(key, text)
    fun load(key: String): String? = session.load(key)
    fun exists(key: String): Boolean = session.exists(key)
    fun delete(key: String) = session.delete(key)

    fun close() {
        closed = true
        worker.shutdown()
        host.close()
    }

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
