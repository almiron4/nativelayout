/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

package co.adilson889.nativelayout.core

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

class LayoutSession(private val host: LayoutHost) {
    private var tree: Native? = null
    private var byKey: Map<String, Native> = emptyMap()
    private var byId: Map<String, Native> = emptyMap()
    private var lastEvent: LayoutEvent? = null
    private var clickedIndex = -1
    private var closed = false

    val warnings = mutableListOf<LayoutWarning>()

    suspend fun render(m: FilledMarkup): Boolean {
        if (closed) return false
        val r = LayoutEngine.build(m.html, m.line, m.variables)
        for (a in r.warnings) if (warnings.none { it.message == a.message && it.line == a.line }) warnings.add(a)

        val changes = LayoutDiffer.diff(tree, r.root)
        if (changes.isNotEmpty()) host.applyChanges(changes)

        tree = r.root
        val map = linkedMapOf<String, Native>()
        index(r.root, map)
        byKey = map
        byId = r.byId
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
