/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout.core

data class LayoutWarning(val message: String, val line: Int)

class LayoutResult(
    val root: Native,
    val warnings: List<LayoutWarning>,
    val byId: Map<String, Native>
)

object LayoutEngine {
    fun build(
        html: String,
        startLine: Int = 1,
        variables: Map<String, String> = emptyMap()
    ): LayoutResult = LayoutBuilder(html, startLine, variables).build()
}

private val SUPPORTED_TAGS = setOf("div", "span", "p", "h1", "h2", "h3", "button", "input", "textarea", "img", "ul", "li")
private const val TAGS_SUGGESTION = "supported tags: div, span, p, h1, h2, h3, button, input, textarea, img, ul, li"
private val BORDER_STYLES = setOf("solid", "dashed", "dotted", "double", "groove", "ridge", "inset", "outset")
private val REGEX_VAR = Regex("""var\(\s*(--[\w-]+)\s*\)""")

private class LayoutBuilder(
    private val html: String,
    private val startLine: Int,
    private val variables: Map<String, String>
) {
    private val source = "\n".repeat(maxOf(startLine - 1, 0)) + html
    private val warnings = mutableListOf<LayoutWarning>()
    private val warned = HashSet<Pair<String, Int>>()
    private val ids = linkedMapOf<String, Native>()

    private fun err(message: String, line: Int, word: String? = null, suggestion: String? = null) =
        MarkupError(message, line, source, word, suggestion)

    private fun warn(message: String, line: Int) {
        if (warned.add(Pair(message, line))) warnings.add(LayoutWarning(message, line))
    }

    fun build(): LayoutResult {
        val nodes = HtmlParser(html, startLine, source).parse()
        val relevant = nodes.filter { !(it is HtmlText && it.text.isBlank()) }
        val rootEl = relevant.singleOrNull() as? HtmlElement
            ?: throw err(
                "must have a single root tag",
                relevant.getOrNull(1)?.line ?: relevant.firstOrNull()?.line ?: startLine
            )
        val root = convertElement(rootEl, NativeStyle())
        assignKeys(root, "")
        return LayoutResult(root, warnings.toList(), ids.toMap())
    }

    private fun assignKeys(n: Native, path: String) {
        n.key = n.id?.let { "#$it" } ?: (if (path.isEmpty()) "/" else path)
        val seen = HashSet<String>()
        n.nativeChildren().forEachIndexed { i, f ->
            val hint = f.keyHint
            if (hint != null && !seen.add(hint)) throw err("duplicate key '$hint'", f.line, hint)
            assignKeys(f, "$path/${if (hint != null) "~$hint" else i.toString()}")
        }
    }

    private fun keyOf(el: HtmlElement): String? =
        el.attributes["key"]?.trim()?.takeIf { it.isNotEmpty() }

    private fun substituteVariables(text: String, line: Int): String =
        if ("var(" !in text) text
        else REGEX_VAR.replace(text) { m ->
            val name = m.groupValues[1]
            variables[name] ?: throw err(
                "the variable '$name' is not defined", line, name
            )
        }

    private fun declarationsOf(el: HtmlElement): List<CssDeclaration> {
        val s = el.attributes["style"] ?: return emptyList()
        return CssParser.declarations(substituteVariables(s, el.line), el.line, ::warn)
    }

    private fun inherit(p: NativeStyle) =
        NativeStyle(textColor = p.textColor, fontSize = p.fontSize, bold = p.bold)

    private fun vertical(rem: Float) = Sides(top = Length.rem(rem), bottom = Length.rem(rem))

    private fun tagDefaults(el: HtmlElement, b: NativeStyle): NativeStyle = when (el.tag) {
        "h1" -> b.copy(fontSize = Length.rem(2f), bold = true, margin = vertical(1.3f))
        "h2" -> b.copy(fontSize = Length.rem(1.5f), bold = true, margin = vertical(1.2f))
        "h3" -> b.copy(fontSize = Length.rem(1.17f), bold = true, margin = vertical(1.1f))
        "p" -> b.copy(margin = vertical(1f))
        "ul" -> b.copy(margin = vertical(1f), padding = Sides(left = Length.rem(1.5f)))
        "button" -> b.copy(
            padding = Sides(Length.rem(0.5f), Length.rem(1f), Length.rem(0.5f), Length.rem(1f)),
            backgroundColor = Colors.BUTTON_GRAY, borderRadius = Length.px(4f),
            textAlign = TextAlign.CENTER, textColor = Colors.BLACK, bold = false
        )
        "input", "textarea" ->
            if ((el.attributes["type"] ?: "text").trim().lowercase() == "checkbox") b
            else b.copy(
                padding = Sides(Length.rem(0.5f), Length.rem(0.5f), Length.rem(0.5f), Length.rem(0.5f)),
                borderWidth = Length.px(1f), borderColor = Colors.BORDER_GRAY, borderRadius = Length.px(2f),
                backgroundColor = Colors.WHITE, textColor = Colors.BLACK
            )
        else -> b
    }

    private fun computeStyle(el: HtmlElement, parentStyle: NativeStyle): NativeStyle {
        var e = tagDefaults(el, inherit(parentStyle))
        for (d in declarationsOf(el)) e = applyDeclaration(e, d)
        return e
    }

    private fun sidesOf(v: String): Sides? {
        val p = CssParser.splitTopLevel(v, ' ').filter { it.isNotBlank() }
            .map { CssValues.length(it) ?: return null }
        return when (p.size) {
            1 -> Sides(p[0], p[0], p[0], p[0])
            2 -> Sides(p[0], p[1], p[0], p[1])
            3 -> Sides(p[0], p[1], p[2], p[1])
            4 -> Sides(p[0], p[1], p[2], p[3])
            else -> null
        }
    }

    private fun applyDeclaration(e: NativeStyle, d: CssDeclaration): NativeStyle {
        val v = d.value.trim()
        val vl = v.lowercase()
        fun invalid(): NativeStyle {
            warn("CSS: invalid value in '${d.name}: ${d.value}', ignored", d.line)
            return e
        }
        fun len(): Length? = CssValues.length(v)

        return when (d.name) {
            "color" -> CssValues.color(v)?.let { e.copy(textColor = it) } ?: invalid()
            "background", "background-color" -> CssValues.color(v)?.let { e.copy(backgroundColor = it) } ?: invalid()
            "font-size" -> len()?.takeIf { it.value > 0f }?.let { e.copy(fontSize = it) } ?: invalid()
            "font-weight" -> {
                val n = vl.toIntOrNull()
                when {
                    vl == "bold" || vl == "bolder" -> e.copy(bold = true)
                    vl == "normal" || vl == "lighter" -> e.copy(bold = false)
                    n != null -> e.copy(bold = n >= 600)
                    else -> invalid()
                }
            }
            "padding" -> sidesOf(v)?.let { e.copy(padding = it) } ?: invalid()
            "margin" -> sidesOf(v)?.let { e.copy(margin = it) } ?: invalid()
            "border-radius" -> len()?.let { e.copy(borderRadius = it) } ?: invalid()
            "border" -> {
                if (vl == "none" || vl == "0") {
                    e.copy(borderWidth = Length.ZERO)
                } else {
                    var width: Length? = null
                    var color: Int? = null
                    var noBorder = false
                    var ok = true
                    for (tok in CssParser.splitTopLevel(v, ' ').filter { it.isNotBlank() }) {
                        val tl = tok.lowercase()
                        val w = CssValues.length(tok)
                        when {
                            w != null -> width = w
                            tl == "none" || tl == "hidden" -> noBorder = true
                            tl in BORDER_STYLES -> {}
                            else -> {
                                val c = CssValues.color(tok)
                                if (c != null) color = c else ok = false
                            }
                        }
                    }
                    if (!ok) invalid() else {
                        var r = e.copy(borderWidth = if (noBorder) Length.ZERO else (width ?: Length.px(3f)))
                        if (color != null) r = r.copy(borderColor = color)
                        r
                    }
                }
            }

            "display" -> when (vl) {
                "flex", "block" -> e
                else -> {
                    warn("CSS: 'display: $vl' is not supported in v1 (use display:flex)", d.line)
                    e
                }
            }
            "flex-direction" -> when (vl) {
                "row" -> e.copy(direction = Direction.ROW)
                "column" -> e.copy(direction = Direction.COLUMN)
                else -> {
                    warn("CSS: 'flex-direction: $vl' is not supported in v1 (use row or column)", d.line)
                    e
                }
            }
            "flex" -> {
                val g = when (vl) {
                    "none" -> 0f
                    "auto" -> 1f
                    else -> vl.split(' ').first().toFloatOrNull()
                }
                if (g != null && g >= 0f) e.copy(flex = g) else invalid()
            }
            "justify-content" -> when (vl) {
                "flex-start", "start" -> e.copy(justify = Justify.START)
                "center" -> e.copy(justify = Justify.CENTER)
                "flex-end", "end" -> e.copy(justify = Justify.END)
                else -> {
                    warn("CSS: 'justify-content: $vl' is not supported in v1 (use flex-start, center or flex-end)", d.line)
                    e
                }
            }
            "align-items" -> when (vl) {
                "flex-start", "start" -> e.copy(alignItems = Align.START)
                "center" -> e.copy(alignItems = Align.CENTER)
                "flex-end", "end" -> e.copy(alignItems = Align.END)
                "stretch" -> e.copy(alignItems = Align.STRETCH)
                else -> {
                    warn("CSS: 'align-items: $vl' is not supported in v1 (use flex-start, center or flex-end)", d.line)
                    e
                }
            }
            "gap" -> len()?.let { e.copy(gap = it) } ?: invalid()
            "width" -> CssValues.size(v)?.let { e.copy(width = it) } ?: invalid()
            "height" -> CssValues.size(v)?.let { e.copy(height = it) } ?: invalid()
            else -> {
                warn("CSS: property '${d.name}' is not supported in v1 and was ignored", d.line)
                e
            }
        }
    }

    private fun isInline(el: HtmlElement): Boolean =
        el.tag == "span" && el.id == null && "class" !in el.attributes &&
            el.children.none { it is HtmlElement && !isInline(it) }

    private fun span(text: String, e: NativeStyle) = Span(text, e.bold, e.textColor, e.fontSize)

    private fun collectInline(el: HtmlElement, parentStyle: NativeStyle, inlineRun: MutableList<Span>) {
        val style = computeStyle(el, parentStyle)
        for (f in el.children) {
            when (f) {
                is HtmlText -> inlineRun.add(span(f.text, style))
                is HtmlElement -> collectInline(f, style, inlineRun)
            }
        }
    }

    private fun normalize(inlineRun: List<Span>): List<Span> {
        val result = mutableListOf<Span>()
        var lastSpace = true
        for (t in inlineRun) {
            val sb = StringBuilder()
            for (ch in t.text) {
                if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                    if (!lastSpace) { sb.append(' '); lastSpace = true }
                } else {
                    sb.append(ch)
                    lastSpace = false
                }
            }
            if (sb.isNotEmpty()) result.add(t.copy(text = sb.toString()))
        }
        if (result.isNotEmpty()) {
            val u = result[result.size - 1]
            val cleaned = u.text.trimEnd(' ')
            if (cleaned.isEmpty()) result.removeAt(result.size - 1)
            else if (cleaned.length != u.text.length) result[result.size - 1] = u.copy(text = cleaned)
        }
        return result
    }

    private fun textOf(el: HtmlElement, style: NativeStyle): String {
        val inlineRun = mutableListOf<Span>()
        for (f in el.children) {
            when (f) {
                is HtmlText -> inlineRun.add(span(f.text, style))
                is HtmlElement ->
                    if (isInline(f)) collectInline(f, style, inlineRun)
                    else throw err("<${el.tag}> can only contain text", f.line, "<${f.tag}")
            }
        }
        return normalize(inlineRun).joinToString("") { it.text }
    }

    private fun convertChildren(nodes: List<HtmlNode>, parentStyle: NativeStyle): List<Native> {
        val result = mutableListOf<Native>()
        val inlineRun = mutableListOf<Span>()
        var runLine = -1

        fun flush() {
            val spans = normalize(inlineRun)
            if (spans.isNotEmpty()) {
                result.add(NativeText(null, emptyList(), inherit(parentStyle), runLine, spans))
            }
            inlineRun.clear()
            runLine = -1
        }

        for (n in nodes) {
            when (n) {
                is HtmlText -> {
                    if (runLine < 0) runLine = n.line
                    inlineRun.add(span(n.text, parentStyle))
                }
                is HtmlElement -> {
                    if (isInline(n)) {
                        if (runLine < 0) runLine = n.line
                        collectInline(n, parentStyle, inlineRun)
                    } else {
                        flush()
                        result.add(convertElement(n, parentStyle))
                    }
                }
            }
        }
        flush()
        return result
    }

    private fun registerId(el: HtmlElement): String? {
        val id = el.id ?: return null
        if (id in ids || id in reservedIds) throw err("duplicate id '$id'", el.line, id)
        reservedIds.add(id)
        return id
    }

    private val reservedIds = mutableSetOf<String>()

    private fun convertElement(el: HtmlElement, parentStyle: NativeStyle): Native {
        val tag = el.tag
        if (tag !in SUPPORTED_TAGS) {
            throw err("tag '$tag' is not supported", el.line, "<$tag", TAGS_SUGGESTION)
        }
        val style = computeStyle(el, parentStyle)
        val id = registerId(el)
        val classes = el.classes
        val disabled = "disabled" in el.attributes

        val node: Native = when (tag) {
            "button" -> NativeButton(id, classes, style, el.line, textOf(el, style), disabled)
            "input" -> convertInput(el, style, id, classes, disabled)
            "textarea" -> {
                val content = el.children.filterIsInstance<HtmlText>().joinToString("") { it.text }
                    .removePrefix("\n")
                val rows = el.attributes["rows"]?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 3
                NativeField(
                    id, classes, style, el.line, FieldType.AREA,
                    el.attributes["placeholder"] ?: "", content,
                    disabled, "readonly" in el.attributes, rows
                )
            }
            "img" -> {
                val src = el.attributes["src"]?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw err("<img> requires a src attribute", el.line, "<img")
                NativeImage(id, classes, style, el.line, src, el.attributes["alt"] ?: "")
            }
            "ul" -> convertList(el, style, id, classes)
            "li" -> throw err("<li> can only be inside <ul>", el.line, "<li")
            else -> {
                val children = convertChildren(el.children, style)
                val virtual = "virtual" in el.attributes
                if (virtual) {
                    for (c in children) {
                        if (c.keyHint == null) {
                            throw err(
                                "every child of a 'virtual' container needs a key", c.line, "virtual",
                                "add key=\"{item.id}\" to each item inside the for"
                            )
                        }
                    }
                }
                val only = children.singleOrNull()
                if (!virtual && only is NativeText) {
                    NativeText(id, classes, style, el.line, only.spans)
                } else {
                    NativeBox(id, classes, style, el.line, children, virtual)
                }
            }
        }
        node.keyHint = keyOf(el)
        if (id != null) ids[id] = node
        return node
    }

    private fun convertInput(
        el: HtmlElement, style: NativeStyle, id: String?, classes: List<String>, disabled: Boolean
    ): Native {
        val type = (el.attributes["type"] ?: "text").trim().lowercase()
        return when (type) {
            "checkbox" -> NativeCheckbox(id, classes, style, el.line, "checked" in el.attributes, disabled)
            "text", "password", "number" -> NativeField(
                id, classes, style, el.line,
                when (type) { "password" -> FieldType.PASSWORD; "number" -> FieldType.NUMBER; else -> FieldType.TEXT },
                el.attributes["placeholder"] ?: "", el.attributes["value"] ?: "",
                disabled, "readonly" in el.attributes, 1
            )
            else -> throw err(
                "<input type=\"$type\"> is not supported", el.line, "type",
                "supported types: text, password, number, checkbox"
            )
        }
    }

    private fun convertList(el: HtmlElement, style: NativeStyle, id: String?, classes: List<String>): NativeList {
        val items = mutableListOf<NativeItem>()
        for (f in el.children) {
            when (f) {
                is HtmlText -> if (f.text.isNotBlank()) throw err("<ul> can only contain <li>", f.line)
                is HtmlElement -> {
                    if (f.tag != "li") {
                        throw err("<ul> can only contain <li>, found <${f.tag}>", f.line, "<${f.tag}")
                    }
                    val liStyle = computeStyle(f, style)
                    val liId = registerId(f)
                    val item = NativeItem(
                        liId, f.classes, liStyle, f.line, "\u2022", convertChildren(f.children, liStyle)
                    )
                    item.keyHint = keyOf(f)
                    if (liId != null) ids[liId] = item
                    items.add(item)
                }
            }
        }
        return NativeList(id, classes, style, el.line, items)
    }
}
