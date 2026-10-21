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
import android.content.res.Configuration
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
internal class GoPage(val next: NativeView.() -> Page, val root: Boolean = false) : Page()
internal object BackPage : Page()

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

    // @media (prefers-color-scheme: dark) follows the system theme, read on every render.
    private val isDark: () -> Boolean = {
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

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

    // Screens left behind by go(): goBack() returns to the last one. Each one starts again with a new state.
    private val history = ArrayList<NativeView.() -> Page>()
    private var backHandled = false
    private var lastShown: ViewPage? = null

    /** Called when the system back button is pressed on the first screen and the program did not handle it. */
    var onExit: (() -> Unit)? = null

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
        val (prepared, expanded) = inBackground {
            val ex = Expander.expand(screen.source, data, { name -> loader(name) }, isDark())
            Pair(session.prepare(FilledMarkup(ex.html, 1)), ex)
        }
        val expandWarnings = expanded.warnings
        session.setScopeItems(expanded.items)
        for (w in expandWarnings) {
            if (session.warnings.none { it.message == w.message && it.line == w.line }) session.warnings.add(w)
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

    /** Jump to another screen function. The state of the current one is dropped, but [goBack] can return to it. */
    fun go(next: NativeView.() -> Page): Page = GoPage(next)

    /** Jump to another screen function and forget the screens behind (after a login or a logout, for example). */
    fun goRoot(next: NativeView.() -> Page): Page = GoPage(next, root = true)

    /** The opposite of [go]: returns to the screen that jumped here. On the first screen it does nothing. */
    fun goBack(): Page = BackPage

    /** True when there is a screen to return to with [goBack]. */
    fun canGoBack(): Boolean = history.isNotEmpty()

    /**
     * True when the system back button woke the program. Ask it to handle the button yourself
     * (to close a dialog, for example). A screen that never asks gets the default: [goBack],
     * or [onExit] on the first screen.
     */
    fun back(): Boolean {
        if (fresh || !session.isBack()) return false
        backHandled = true
        return true
    }

    /** Call it from the activity when the system back button is pressed. Must run on the UI thread. */
    fun pressBack() {
        if (!closed) host.postBack()
    }

    private suspend fun runApp(app: NativeView.() -> Page) {
        var current = app
        var hops = 0
        while (!closed) {
            slotIndex = 0
            backHandled = false
            var page = current()
            if (!fresh && !backHandled && session.isBack()) {
                if (history.isNotEmpty()) page = BackPage else onExit?.invoke()
            }
            if (page is BackPage && history.isEmpty()) {
                // nothing to return to: the screen stays as it is
                onExit?.invoke()
                page = lastShown ?: throw IllegalStateException("goBack: there is no screen to return to")
            }
            when (page) {
                is GoPage -> {
                    hops++
                    if (hops > 8) throw IllegalStateException("go: too many jumps without showing a screen")
                    if (page.root) history.clear() else history.add(current)
                    current = page.next
                    lastShown = null
                    slots.clear()
                    fresh = true
                    session.reset()
                }
                is BackPage -> {
                    hops++
                    if (hops > 8) throw IllegalStateException("go: too many jumps without showing a screen")
                    current = history.removeAt(history.size - 1)
                    lastShown = null
                    slots.clear()
                    fresh = true
                    session.reset()
                }
                is ViewPage -> {
                    hops = 0
                    lastShown = page
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

    /** The item of the `for [name] in ...` that holds the element that was just tapped (section 5.7). */
    fun clicked(name: String): Any? = if (fresh) null else session.clickedItem(name)

    /** [value] of the element [id] of the item(s) given, one for each enclosing `for`, outermost first. */
    fun value(id: String, vararg items: Any?): String =
        if (fresh) "" else session.fieldText(id, items.toList()) ?: ""

    fun checked(id: String, vararg items: Any?): Boolean =
        if (fresh) false else session.fieldChecked(id, items.toList()) ?: false

    /** Fills the element [id] of the item(s) given: `setValue("qty", p, "1")`. The last argument is the text. */
    fun setValue(id: String, first: Any?, vararg rest: Any?) {
        val all = listOf(first) + rest.toList()
        val text = all.last()
        require(text is String) { "setValue(id, item..., text): the last argument must be the text" }
        session.setValue(id, text, all.dropLast(1))
    }

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
