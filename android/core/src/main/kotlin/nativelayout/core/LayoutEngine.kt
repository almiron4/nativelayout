/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout.core

import kotlin.math.roundToInt

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

private val SUPPORTED_TAGS = setOf(
    "div", "span", "p", "h1", "h2", "h3", "button", "input", "textarea", "img", "ul", "li",
    "fieldset", "legend", "dialog"
)
private const val TAGS_SUGGESTION =
    "supported tags: div, span, p, h1, h2, h3, button, input, textarea, img, ul, li, fieldset, legend, dialog"
private val FIELDSET_BORDER: Int = 0xFFBDBDBD.toInt()
private val DIALOG_BACKDROP: Int = 0x80000000.toInt()
private val BORDER_STYLES = setOf("solid", "dashed", "dotted", "double", "groove", "ridge", "inset", "outset")
private val REGEX_VAR = Regex("""var\(\s*(--[\w-]+)\s*\)""")
private val REGEX_DEG = Regex("""^(-?\d*\.?\d+)deg$""")

private class LayoutBuilder(
    private val html: String,
    private val startLine: Int,
    private val variables: Map<String, String>
) {
    private val source = "\n".repeat(maxOf(startLine - 1, 0)) + html
    private val warnings = mutableListOf<LayoutWarning>()

    /** How deep the element being converted is (the root is 0) and the tag of its parent. */
    private var depth = 0
    private var parentTag: String? = null

    /** True while converting the children of a <fieldset disabled>. */
    private var groupDisabled = false
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

    private fun inherit(p: NativeStyle) = NativeStyle(
        textColor = p.textColor, fontSize = p.fontSize, bold = p.bold, textAlign = p.textAlign,
        italic = p.italic, underline = p.underline, strike = p.strike,
        letterSpacing = p.letterSpacing, lineHeight = p.lineHeight,
        textTransform = p.textTransform, fontFamily = p.fontFamily
    )

    private fun vertical(rem: Float) = Sides(top = Length.rem(rem), bottom = Length.rem(rem))

    private fun tagDefaults(el: HtmlElement, b: NativeStyle): NativeStyle = when (el.tag) {
        "h1" -> b.copy(fontSize = Length.rem(2f), bold = true, margin = vertical(1.3f))
        "h2" -> b.copy(fontSize = Length.rem(1.5f), bold = true, margin = vertical(1.2f))
        "h3" -> b.copy(fontSize = Length.rem(1.17f), bold = true, margin = vertical(1.1f))
        "p" -> b.copy(margin = vertical(1f))
        "fieldset" -> b.copy(
            padding = Sides(Length.rem(0.75f), Length.rem(0.75f), Length.rem(0.75f), Length.rem(0.75f)),
            margin = vertical(0.5f),
            borderWidth = Length.px(1f), borderColor = FIELDSET_BORDER, borderRadius = Length.px(6f)
        )
        "legend" -> b.copy(bold = true, margin = Sides(bottom = Length.rem(0.5f)))
        "dialog" -> b.copy(backgroundColor = DIALOG_BACKDROP, justify = Justify.CENTER, alignItems = Align.CENTER)
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
        val pressed = stateOf(el, "style-pressed", false)
        val disabled = stateOf(el, "style-disabled", true)
        val focused = stateOf(el, "style-focus", false)
        if (pressed != null || disabled != null || focused != null) {
            e = e.copy(states = StateStyles(pressed, disabled, focused))
        }
        return e
    }

    /** Declarations that the expander wrote for :pressed, :disabled and :focus. */
    private fun stateOf(el: HtmlElement, attribute: String, allowOpacity: Boolean): StateStyle? {
        val text = el.attributes[attribute] ?: return null
        var background: Int? = null
        var color: Int? = null
        var border: Int? = null
        var opacity: Float? = null
        for (d in CssParser.declarations(text, el.line, ::warn)) {
            val v = d.value.trim()
            fun bad() = warn("CSS: invalid value in '${d.name}: ${d.value}', ignored", d.line)
            when (d.name) {
                "background", "background-color" -> CssValues.color(v)?.let { background = it } ?: bad()
                "color" -> CssValues.color(v)?.let { color = it } ?: bad()
                "border-color" -> CssValues.color(v)?.let { border = it } ?: bad()
                "opacity" ->
                    if (!allowOpacity) warn("CSS: 'opacity' is only supported in :disabled", d.line)
                    else CssValues.fraction(v)?.let { opacity = it } ?: bad()
                else -> warn(
                    "CSS: '${d.name}' is not supported in :pressed, :disabled or :focus " +
                        "(use background, color, border-color; opacity only in :disabled)", d.line
                )
            }
        }
        if (background == null && color == null && border == null && opacity == null) return null
        return StateStyle(background, color, border, opacity)
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

    /** border-radius with one to four lengths, in the CSS order. The slash form and percentages are not supported. */
    private fun cornersOf(v: String): Corners? {
        if ('/' in v) return null
        val p = CssParser.splitTopLevel(v, ' ').filter { it.isNotBlank() }
            .map { CssValues.length(it)?.takeIf { l -> l.value >= 0f } ?: return null }
        return when (p.size) {
            1 -> Corners(p[0], p[0], p[0], p[0])
            2 -> Corners(p[0], p[1], p[0], p[1])
            3 -> Corners(p[0], p[1], p[2], p[1])
            4 -> Corners(p[0], p[1], p[2], p[3])
            else -> null
        }
    }

    /** Equal corners stay a single radius; different ones go to [NativeStyle.corners]. */
    private fun withCorners(e: NativeStyle, c: Corners): NativeStyle =
        if (c.topLeft == c.topRight && c.topRight == c.bottomRight && c.bottomRight == c.bottomLeft)
            e.copy(borderRadius = c.topLeft, corners = null)
        else e.copy(borderRadius = Length.ZERO, corners = c)

    private val DECORATION_STYLES = setOf("solid", "double", "dotted", "dashed", "wavy")

    private fun directionAngle(words: String): Int? = when (words.split(' ').filter { it.isNotEmpty() }.toSet()) {
        setOf("top") -> 0
        setOf("top", "right") -> 45
        setOf("right") -> 90
        setOf("bottom", "right") -> 135
        setOf("bottom") -> 180
        setOf("bottom", "left") -> 225
        setOf("left") -> 270
        setOf("top", "left") -> 315
        else -> null
    }

    private fun gradientOf(v: String): Gradient? {
        val open = v.indexOf('(')
        val close = v.lastIndexOf(')')
        if (open < 0 || close <= open) return null
        val args = CssParser.splitTopLevel(v.substring(open + 1, close), ',').map { it.trim() }.filter { it.isNotEmpty() }
        if (args.size < 2) return null
        var angle = 180
        var from = 0
        val first = args[0].lowercase()
        val degrees = REGEX_DEG.matchEntire(first)
        when {
            degrees != null -> {
                angle = ((degrees.groupValues[1].toFloat().roundToInt() % 360) + 360) % 360
                from = 1
            }
            first.startsWith("to ") -> {
                angle = directionAngle(first.removePrefix("to ").trim()) ?: return null
                from = 1
            }
        }
        val colors = ArrayList<Int>()
        for (i in from until args.size) {
            val tokens = CssParser.splitTopLevel(args[i], ' ').filter { it.isNotBlank() }
            colors.add(tokens.firstNotNullOfOrNull { CssValues.color(it) } ?: return null)
        }
        return if (colors.size >= 2) Gradient(angle, colors) else null
    }

    /** box-shadow becomes an elevation: half of the blur (or of the vertical offset when there is no blur). */
    private fun elevationOf(v: String): Length? {
        val first = CssParser.splitTopLevel(v, ',').first().trim()
        if (first.lowercase() == "none") return Length.ZERO
        val lengths = ArrayList<Length>()
        for (tok in CssParser.splitTopLevel(first, ' ').filter { it.isNotBlank() }) {
            if (tok.lowercase() == "inset") return null
            val len = CssValues.length(tok)
            if (len != null) lengths.add(len) else if (CssValues.color(tok) == null) return null
        }
        if (lengths.size !in 2..4) return null
        val ref = if (lengths.size >= 3 && lengths[2].value > 0f) lengths[2] else lengths[1]
        return Length(maxOf(ref.value, 0f) / 2f, ref.scalable)
    }

    private fun fontFamilyOf(v: String): FontFamily? {
        for (raw in CssParser.splitTopLevel(v, ',')) {
            val name = raw.trim().trim('"', '\'').lowercase()
            when (name) {
                "sans-serif", "system-ui", "roboto", "arial", "helvetica", "default" -> return FontFamily.SANS
                "serif", "georgia", "times", "times new roman" -> return FontFamily.SERIF
                "monospace", "courier", "courier new", "consolas", "menlo" -> return FontFamily.MONOSPACE
            }
        }
        return null
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
            "background", "background-color", "background-image" -> {
                val isGradient = vl.startsWith("linear-gradient(") && vl.endsWith(")")
                when {
                    isGradient -> gradientOf(v)?.let { e.copy(gradient = it) } ?: invalid()
                    d.name == "background-image" -> if (vl == "none") e.copy(gradient = null) else invalid()
                    else -> CssValues.color(v)?.let { e.copy(backgroundColor = it, gradient = null) } ?: invalid()
                }
            }
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
            "padding-top" -> len()?.let { e.copy(padding = e.padding.copy(top = it)) } ?: invalid()
            "padding-right" -> len()?.let { e.copy(padding = e.padding.copy(right = it)) } ?: invalid()
            "padding-bottom" -> len()?.let { e.copy(padding = e.padding.copy(bottom = it)) } ?: invalid()
            "padding-left" -> len()?.let { e.copy(padding = e.padding.copy(left = it)) } ?: invalid()
            "margin-top" -> len()?.let { e.copy(margin = e.margin.copy(top = it)) } ?: invalid()
            "margin-right" -> len()?.let { e.copy(margin = e.margin.copy(right = it)) } ?: invalid()
            "margin-bottom" -> len()?.let { e.copy(margin = e.margin.copy(bottom = it)) } ?: invalid()
            "margin-left" -> len()?.let { e.copy(margin = e.margin.copy(left = it)) } ?: invalid()
            "border-width" -> len()?.takeIf { it.value >= 0f }?.let { e.copy(borderWidth = it) } ?: invalid()
            "border-color" -> CssValues.color(v)?.let { e.copy(borderColor = it) } ?: invalid()
            "border-style" -> when {
                vl == "none" || vl == "hidden" -> e.copy(borderWidth = Length.ZERO)
                vl in BORDER_STYLES -> e
                else -> invalid()
            }
            "text-align" -> when (vl) {
                "left", "start" -> e.copy(textAlign = TextAlign.START)
                "center" -> e.copy(textAlign = TextAlign.CENTER)
                "right", "end" -> e.copy(textAlign = TextAlign.END)
                "justify" -> {
                    warn("CSS: 'text-align: justify' is not supported, using start", d.line)
                    e.copy(textAlign = TextAlign.START)
                }
                else -> invalid()
            }
            "opacity" -> CssValues.fraction(v)?.let { e.copy(opacity = it) } ?: invalid()
            "font-style" -> when (vl) {
                "italic", "oblique" -> e.copy(italic = true)
                "normal" -> e.copy(italic = false)
                else -> invalid()
            }
            "text-decoration", "text-decoration-line" -> {
                var underline = false
                var strike = false
                var none = false
                var ok = true
                for (tok in CssParser.splitTopLevel(vl, ' ').filter { it.isNotBlank() }) {
                    when {
                        tok == "none" -> none = true
                        tok == "underline" -> underline = true
                        tok == "line-through" -> strike = true
                        tok == "overline" -> warn("CSS: 'text-decoration: overline' is not supported, ignored", d.line)
                        tok in DECORATION_STYLES || CssValues.color(tok) != null || CssValues.length(tok) != null -> {}
                        else -> ok = false
                    }
                }
                if (!ok) invalid()
                else if (none && !underline && !strike) e.copy(underline = false, strike = false)
                else e.copy(underline = underline, strike = strike)
            }
            "letter-spacing" ->
                if (vl == "normal") e.copy(letterSpacing = Length.ZERO)
                else len()?.let { e.copy(letterSpacing = it) } ?: invalid()
            "line-height" -> {
                val number = vl.toFloatOrNull()
                val percent = if (vl.endsWith("%")) vl.dropLast(1).toFloatOrNull() else null
                val length = len()
                when {
                    vl == "normal" -> e.copy(lineHeight = null)
                    number != null && number > 0f -> e.copy(lineHeight = LineHeight.Multiple(number))
                    percent != null && percent > 0f -> e.copy(lineHeight = LineHeight.Multiple(percent / 100f))
                    length != null && length.value > 0f -> e.copy(lineHeight = LineHeight.Fixed(length))
                    else -> invalid()
                }
            }
            "text-transform" -> when (vl) {
                "none" -> e.copy(textTransform = TextTransform.NONE)
                "uppercase" -> e.copy(textTransform = TextTransform.UPPERCASE)
                "lowercase" -> e.copy(textTransform = TextTransform.LOWERCASE)
                "capitalize" -> e.copy(textTransform = TextTransform.CAPITALIZE)
                else -> invalid()
            }
            "font-family" -> {
                val family = fontFamilyOf(v)
                if (family != null) e.copy(fontFamily = family) else {
                    warn("CSS: no known family in 'font-family: $v' (use sans-serif, serif or monospace), ignored", d.line)
                    e
                }
            }
            "overflow", "overflow-x", "overflow-y" -> when (vl) {
                "hidden", "clip" -> e.copy(overflowHidden = true)
                "visible" -> e.copy(overflowHidden = false)
                else -> {
                    warn("CSS: 'overflow: $vl' is not supported (use hidden or visible)", d.line)
                    e
                }
            }
            "box-shadow" -> elevationOf(v)?.let { e.copy(elevation = it) } ?: invalid()
            "min-width" -> if (vl == "0" || vl == "auto") e.copy(minWidth = null) else len()?.let { e.copy(minWidth = it) } ?: invalid()
            "min-height" -> if (vl == "0" || vl == "auto") e.copy(minHeight = null) else len()?.let { e.copy(minHeight = it) } ?: invalid()
            "max-width" -> if (vl == "none") e.copy(maxWidth = null) else len()?.let { e.copy(maxWidth = it) } ?: invalid()
            "max-height" -> if (vl == "none") e.copy(maxHeight = null) else len()?.let { e.copy(maxHeight = it) } ?: invalid()
            "align-self" -> when (vl) {
                "auto" -> e.copy(alignSelf = null)
                "flex-start", "start" -> e.copy(alignSelf = Align.START)
                "center" -> e.copy(alignSelf = Align.CENTER)
                "flex-end", "end" -> e.copy(alignSelf = Align.END)
                "stretch" -> e.copy(alignSelf = Align.STRETCH)
                else -> invalid()
            }
            "flex-wrap" -> when (vl) {
                "wrap" -> e.copy(wrap = true)
                "nowrap" -> e.copy(wrap = false)
                else -> {
                    warn("CSS: 'flex-wrap: $vl' is not supported (use wrap or nowrap)", d.line)
                    e
                }
            }
            "border-radius" -> cornersOf(v)?.let { withCorners(e, it) } ?: invalid()
            "border-top-left-radius", "border-top-right-radius",
            "border-bottom-right-radius", "border-bottom-left-radius" ->
                len()?.takeIf { it.value >= 0f }?.let { l ->
                    val c = e.corners ?: Corners(e.borderRadius, e.borderRadius, e.borderRadius, e.borderRadius)
                    withCorners(
                        e, when (d.name) {
                            "border-top-left-radius" -> c.copy(topLeft = l)
                            "border-top-right-radius" -> c.copy(topRight = l)
                            "border-bottom-right-radius" -> c.copy(bottomRight = l)
                            else -> c.copy(bottomLeft = l)
                        }
                    )
                } ?: invalid()
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
                "space-between" -> e.copy(justify = Justify.BETWEEN)
                "space-around" -> e.copy(justify = Justify.AROUND)
                "space-evenly" -> e.copy(justify = Justify.EVENLY)
                else -> {
                    warn(
                        "CSS: 'justify-content: $vl' is not supported " +
                            "(use flex-start, center, flex-end, space-between, space-around or space-evenly)", d.line
                    )
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

    private fun span(text: String, e: NativeStyle) =
        Span(transformed(text, e.textTransform), e.bold, e.textColor, e.fontSize, e.italic, e.underline, e.strike)

    private fun transformed(text: String, t: TextTransform): String = when (t) {
        TextTransform.NONE -> text
        TextTransform.UPPERCASE -> text.uppercase()
        TextTransform.LOWERCASE -> text.lowercase()
        TextTransform.CAPITALIZE -> {
            val sb = StringBuilder(text.length)
            var start = true
            for (ch in text) {
                sb.append(if (start && ch.isLetter()) ch.uppercaseChar() else ch)
                start = ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r'
            }
            sb.toString()
        }
    }

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

    private fun convertChildren(nodes: List<HtmlNode>, parentStyle: NativeStyle, owner: String): List<Native> {
        val savedParent = parentTag
        parentTag = owner
        depth++
        try {
            return convertChildrenOf(nodes, parentStyle)
        } finally {
            depth--
            parentTag = savedParent
        }
    }

    private fun convertChildrenOf(nodes: List<HtmlNode>, parentStyle: NativeStyle): List<Native> {
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
        val myDepth = depth
        val myParent = parentTag
        val style = computeStyle(el, parentStyle)
        val id = registerId(el)
        val classes = el.classes
        val disabled = "disabled" in el.attributes || groupDisabled

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
                if (tag == "legend" && myParent != "fieldset") {
                    throw err("<legend> can only be inside <fieldset>", el.line, "<legend")
                }
                if (tag == "dialog" && myDepth != 1) {
                    throw err(
                        "<dialog> must be a direct child of the root tag", el.line, "<dialog",
                        "put the <dialog> at the first level of the screen, next to the other blocks"
                    )
                }
                val savedGroup = groupDisabled
                if (tag == "fieldset" && "disabled" in el.attributes) groupDisabled = true
                val found = try {
                    convertChildren(el.children, style, tag)
                } finally {
                    groupDisabled = savedGroup
                }
                // the dialogs of the screen are layers: they come last, in the order they were written
                val children =
                    if (myDepth == 0 && found.any { it is NativeBox && it.overlay })
                        found.filter { !(it is NativeBox && it.overlay) } + found.filter { it is NativeBox && it.overlay }
                    else found
                val virtual = "virtual" in el.attributes
                if (virtual) {
                    for (c in children) {
                        if (c is NativeBox && c.overlay) {
                            throw err("<dialog> cannot be inside a 'virtual' container", c.line, "<dialog")
                        }
                        if (c.keyHint == null) {
                            throw err(
                                "every child of a 'virtual' container needs a key", c.line, "virtual",
                                "add key=\"{item.id}\" to each item inside the for"
                            )
                        }
                    }
                }
                val only = children.singleOrNull()
                val isGroup = tag == "dialog" || tag == "fieldset"
                if (!virtual && !isGroup && only is NativeText) {
                    NativeText(id, classes, style, el.line, only.spans)
                } else {
                    NativeBox(id, classes, style, el.line, children, virtual, overlay = tag == "dialog")
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
                        liId, f.classes, liStyle, f.line, "\u2022", convertChildren(f.children, liStyle, "li")
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
