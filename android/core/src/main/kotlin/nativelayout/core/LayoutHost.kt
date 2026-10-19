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
        if (node.id == target) {
            clickedIndex = -1
            return true
        }
        return false
    }

    fun clickIndex(): Int = clickedIndex

    /** Forgets the tree on screen: the next render builds everything from scratch (used when the screen changes). */
    fun reset() {
        tree = null
    }

    fun fieldText(id: String): String? = if (id in byId) host.fieldText(id) else null
    fun fieldChecked(id: String): Boolean? = if (id in byId) host.fieldChecked(id) else null
    fun setValue(id: String, text: String) { if (id in byId) host.setValue(id, text) }

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
