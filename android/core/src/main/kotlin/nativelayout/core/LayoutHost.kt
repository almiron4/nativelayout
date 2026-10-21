/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout.core

class LayoutEvent(val key: String)

/** Key of the event the host sends when the system back button is pressed. It belongs to no element. */
const val BACK_EVENT_KEY = "\u0000back"

interface LayoutHost {
    fun applyChanges(changes: List<Change>)

    suspend fun nextEvent(): LayoutEvent?

    fun fieldText(id: String): String?

    fun fieldChecked(id: String): Boolean?
    fun setValue(id: String, text: String)

    fun save(key: String, text: String)
    fun load(key: String): String?
    fun exists(key: String): Boolean
    fun delete(key: String)

    fun close()
}

class FilledMarkup(
    val html: String,
    val line: Int,
    val variables: Map<String, String> = emptyMap()
)

/**
 * Result of [LayoutSession.prepare]: the new tree, its indexes and the list of
 * changes against the tree that is currently on screen. It is plain data, so it
 * can be built on any thread and handed to [LayoutSession.commit].
 */
class PreparedRender internal constructor(
    internal val root: Native,
    internal val byKey: Map<String, Native>,
    internal val byId: Map<String, Native>,
    internal val changes: List<Change>,
    internal val warnings: List<LayoutWarning>
)

class LayoutSession(private val host: LayoutHost) {
    private var tree: Native? = null
    private var byKey: Map<String, Native> = emptyMap()
    private var byId: Map<String, Native> = emptyMap()
    private var lastEvent: LayoutEvent? = null
    private var clickedIndex = -1
    private var closed = false

    val warnings = mutableListOf<LayoutWarning>()

    /**
     * Heavy part: build the tree, index it and diff it against the tree on screen.
     * It does not touch the host, so it may run on any thread. Calls to prepare and
     * commit must alternate (one prepare, then its commit), never overlap.
     */
    fun prepare(m: FilledMarkup): PreparedRender {
        val r = LayoutEngine.build(m.html, m.line, m.variables)
        val changes = LayoutDiffer.diff(tree, r.root)
        val map = linkedMapOf<String, Native>()
        index(r.root, map)
        return PreparedRender(r.root, map, r.byId, changes, r.warnings)
    }

    /**
     * Light part: apply the changes to the views and wait for the next event.
     * Must run on the thread that owns the views (the UI thread on Android).
     */
    suspend fun commit(p: PreparedRender): Boolean {
        if (closed) return false
        for (a in p.warnings) if (warnings.none { it.message == a.message && it.line == a.line }) warnings.add(a)

        if (p.changes.isNotEmpty()) host.applyChanges(p.changes)

        tree = p.root
        byKey = p.byKey
        byId = p.byId
        lastEvent = null
        clickedIndex = -1

        val event = host.nextEvent()
        if (event == null) {
            closed = true
            tree = null
            host.close()
            return false
        }
        lastEvent = event
        return true
    }

    /** Same behaviour as before: prepare and commit on the calling thread. */
    suspend fun render(m: FilledMarkup): Boolean = commit(prepare(m))

    // ---- identity inside a `for` (section 5.7)
    private var items: Map<String, Any?> = emptyMap()

    /** The items of the repetitions of the screen on show, by path (from [ExpandedScreen.items]). */
    fun setScopeItems(map: Map<String, Any?>) { items = map }

    /** The path of the repetition that holds the tapped element, or null. */
    private fun tappedScope(): String? = lastEvent?.let { byKey[it.key]?.scope }

    /**
     * The item of the `for [name] in ...` that holds the tapped element.
     * Throws when the tap was not inside that `for`.
     */
    fun clickedItem(name: String): Any? {
        val path = tappedScope()
            ?: throw IllegalStateException("clicked('$name'): the tapped element is not inside a 'for'")
        val segments = path.split("/")
        for (i in segments.indices.reversed()) {
            if (Scopes.variableOf(segments[i]) == name) {
                val prefix = segments.subList(0, i + 1).joinToString("/")
                if (!items.containsKey(prefix)) break
                return items[prefix]
            }
        }
        throw IllegalStateException("clicked('$name'): the tapped element is not inside 'for $name in ...'")
    }

    /**
     * The id of the element [id] of the repetition given by [given] (one item for each enclosing `for`, outermost
     * first). Without items, [id] itself when it is not repeated. Throws when the items do not name a repetition.
     */
    fun scopedId(id: String, given: List<Any?>): String? {
        if (given.isEmpty()) {
            if (id in byId) return id
            val prefix = id + Scopes.SEP
            val sample = byId.keys.firstOrNull { it.startsWith(prefix) } ?: return null
            val depth = sample.substring(prefix.length).split("/").size
            val vars = sample.substring(prefix.length).split("/").joinToString(", ") { Scopes.variableOf(it) }
            throw IllegalStateException("'$id' is repeated: give $depth item(s), one for each of: $vars")
        }
        val prefix = id + Scopes.SEP
        val candidates = byId.keys.filter { it.startsWith(prefix) }
        if (candidates.isEmpty()) return null
        for (c in candidates) {
            val segments = c.substring(prefix.length).split("/")
            if (segments.size != given.size) continue
            var ok = true
            var path = ""
            for (k in segments.indices) {
                path = if (k == 0) segments[0] else path + "/" + segments[k]
                if (!items.containsKey(path) || !Scopes.sameItem(given[k], items[path], Scopes.tokenOf(segments[k]))) {
                    ok = false
                    break
                }
            }
            if (ok) return c
        }
        return null
    }

    fun click(target: String): Boolean {
        val ev = lastEvent ?: return false
        val node = byKey[ev.key] ?: return false
        if (target.startsWith(".")) {
            val cls = target.substring(1)
            if (cls !in node.classes) return false
            val all = mutableListOf<Native>()
            tree?.let { collectClass(it, cls, all) }
            clickedIndex = all.indexOfFirst { it.key == node.key }
            return true
        }
        val nid = node.id
        if (nid == target || (nid != null && node.scope != null && nid.substringBefore(Scopes.SEP) == target)) {
            clickedIndex = -1
            return true
        }
        return false
    }

    fun clickIndex(): Int = clickedIndex

    /** True when the event that woke the program is the system back button. */
    fun isBack(): Boolean = lastEvent?.key == BACK_EVENT_KEY

    /** Forgets the tree on screen: the next render builds everything from scratch (used when the screen changes). */
    fun reset() {
        tree = null
    }

    fun fieldText(id: String, given: List<Any?> = emptyList()): String? =
        scopedId(id, given)?.let { host.fieldText(it) }

    fun fieldChecked(id: String, given: List<Any?> = emptyList()): Boolean? =
        scopedId(id, given)?.let { host.fieldChecked(it) }

    fun setValue(id: String, text: String, given: List<Any?> = emptyList()) {
        scopedId(id, given)?.let { host.setValue(it, text) }
    }

    fun save(key: String, text: String) = host.save(key, text)
    fun load(key: String): String? = host.load(key)
    fun exists(key: String): Boolean = host.exists(key)
    fun delete(key: String) = host.delete(key)

    private fun index(n: Native, result: MutableMap<String, Native>) {
        result[n.key] = n
        for (f in n.nativeChildren()) index(f, result)
    }

    private fun collectClass(n: Native, cls: String, result: MutableList<Native>) {
        if (cls in n.classes) result.add(n)
        for (f in n.nativeChildren()) collectClass(f, cls, result)
    }
}
