/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

package co.adilson889.nativelayout.core

class MarkupError(
    message: String, line: Int, source: String,
    word: String? = null, suggestion: String? = null
) : NativeLayoutError("markup: $message", line, extractLine(source, line), word, suggestion)

sealed class HtmlNode {
    abstract val line: Int
}

class HtmlElement(
    val tag: String,
    val attributes: Map<String, String>,
    val children: List<HtmlNode>,
    override val line: Int
) : HtmlNode() {
    val id: String? get() = attributes["id"]?.trim()?.takeIf { it.isNotEmpty() }
    val classes: List<String>
        get() = attributes["class"]?.split(Regex("\\s+"))?.filter { it.isNotEmpty() } ?: emptyList()
}

class HtmlText(val text: String, override val line: Int) : HtmlNode()

class HtmlParser(
    private val html: String,
    startLine: Int,
    private val source: String
) {
    private var i = 0
    private var line = startLine

    private class Open(val tag: String, val attributes: Map<String, String>, val line: Int) {
        val children = mutableListOf<HtmlNode>()
    }

    private val stack = mutableListOf<Open>()
    private val root = mutableListOf<HtmlNode>()

    private companion object {
        val VOID_TAGS = setOf("br", "hr", "img", "input", "meta", "link")
    }

    private fun err(message: String, at: Int = line, word: String? = null) =
        MarkupError(message, at, source, word)

    private fun target(): MutableList<HtmlNode> = if (stack.isEmpty()) root else stack.last().children

    private fun advanceTo(end: Int) {
        for (k in i until end) if (html[k] == '\n') line++
        i = end
    }

    fun parse(): List<HtmlNode> {
        while (i < html.length) {
            val c = html[i]
            when {
                html.startsWith("<!--", i) -> {
                    val end = html.indexOf("-->", i + 4)
                    if (end < 0) throw err("unclosed HTML comment: missing '-->'")
                    advanceTo(end + 3)
                }
                html.startsWith("<!", i) -> {
                    val end = html.indexOf('>', i)
                    if (end < 0) throw err("unclosed '<!': missing '>'")
                    advanceTo(end + 1)
                }
                html.startsWith("</", i) -> readClose()
                c == '<' && i + 1 < html.length && html[i + 1].isLetter() -> readOpen()
                else -> readText()
            }
        }
        if (stack.isNotEmpty()) {
            val a = stack.last()
            throw err("<${a.tag}> is never closed (opened on line ${a.line})", a.line, "<${a.tag}")
        }
        return root
    }

    private fun readText() {
        var j = i + 1
        while (j < html.length) {
            if (html[j] == '<' && j + 1 < html.length &&
                (html[j + 1].isLetter() || html[j + 1] == '/' || html[j + 1] == '!')
            ) break
            j++
        }
        val raw = html.substring(i, j)
        val l = line
        advanceTo(j)
        if (raw.isNotEmpty()) target().add(HtmlText(decode(raw), l))
    }

    private fun readClose() {
        val tagLine = line
        val end = html.indexOf('>', i)
        if (end < 0) throw err("unclosed '</': missing '>'")
        val tag = html.substring(i + 2, end).trim().lowercase()
        advanceTo(end + 1)
        if (tag in VOID_TAGS) return
        if (stack.isEmpty()) {
            throw err("</$tag> has no matching opening tag", tagLine, "</$tag")
        }
        val top = stack.last()
        if (top.tag != tag) {
            throw err("expected </${top.tag}> (opened on line ${top.line}) before </$tag>", tagLine, "</$tag")
        }
        stack.removeAt(stack.size - 1)
        target().add(HtmlElement(top.tag, top.attributes, top.children.toList(), top.line))
    }

    private fun readOpen() {
        val tagLine = line
        var j = i + 1
        val nameStart = j
        while (j < html.length && (html[j].isLetterOrDigit() || html[j] == '-')) j++
        val tag = html.substring(nameStart, j).lowercase()
        val attributes = linkedMapOf<String, String>()
        var selfClose = false

        while (true) {
            while (j < html.length && html[j].isWhitespace()) j++
            if (j >= html.length) throw err("unclosed tag <$tag>: missing '>'", tagLine, "<$tag")
            val ch = html[j]
            if (ch == '>') { j++; break }
            if (ch == '/' && j + 1 < html.length && html[j + 1] == '>') { selfClose = true; j += 2; break }
            if (ch == '/') { j++; continue }

            val attrStart = j
            while (j < html.length && !html[j].isWhitespace() && html[j] != '=' && html[j] != '>' && html[j] != '/') j++
            if (j == attrStart) throw err("invalid attribute in tag <$tag>", tagLine, "<$tag")
            val name = html.substring(attrStart, j).lowercase()
            while (j < html.length && html[j].isWhitespace()) j++

            var value = ""
            if (j < html.length && html[j] == '=') {
                j++
                while (j < html.length && html[j].isWhitespace()) j++
                if (j < html.length && (html[j] == '"' || html[j] == '\'')) {
                    val quote = html[j]
                    val end = html.indexOf(quote, j + 1)
                    if (end < 0) throw err("attribute '$name' has an unclosed quote", tagLine, name)
                    value = html.substring(j + 1, end)
                    j = end + 1
                } else {
                    val valStart = j
                    while (j < html.length && !html[j].isWhitespace() && html[j] != '>') j++
                    value = html.substring(valStart, j)
                }
            }
            if (name !in attributes) attributes[name] = decode(value)
        }
        advanceTo(j)

        when {
            tag == "script" ->
                throw err("<script> is not allowed", tagLine, "<script")
            tag == "style" ->
                throw err("<style> is not supported yet; use style=\"...\"", tagLine, "<style")
            tag in VOID_TAGS || selfClose ->
                target().add(HtmlElement(tag, attributes, emptyList(), tagLine))
            else -> stack.add(Open(tag, attributes, tagLine))
        }
    }

    private fun decode(t: String): String {
        if ('&' !in t) return t
        val sb = StringBuilder()
        var k = 0
        while (k < t.length) {
            val c = t[k]
            if (c == '&') {
                val end = t.indexOf(';', k)
                if (end in (k + 2)..(k + 10)) {
                    val name = t.substring(k + 1, end)
                    val r: String? = when {
                        name == "amp" -> "&"
                        name == "lt" -> "<"
                        name == "gt" -> ">"
                        name == "quot" -> "\""
                        name == "apos" -> "'"
                        name == "nbsp" -> "\u00A0"
                        name.startsWith("#x") || name.startsWith("#X") ->
                            name.substring(2).toIntOrNull(16)
                                ?.takeIf { Character.isValidCodePoint(it) }
                                ?.let { String(Character.toChars(it)) }
                        name.startsWith("#") ->
                            name.substring(1).toIntOrNull()
                                ?.takeIf { Character.isValidCodePoint(it) }
                                ?.let { String(Character.toChars(it)) }
                        else -> null
                    }
                    if (r != null) { sb.append(r); k = end + 1; continue }
                }
            }
            sb.append(c)
            k++
        }
        return sb.toString()
    }
}
