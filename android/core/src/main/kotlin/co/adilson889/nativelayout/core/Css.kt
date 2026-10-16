/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

package co.adilson889.nativelayout.core

import kotlin.math.roundToInt

data class CssDeclaration(val name: String, val value: String, val line: Int)

object CssParser {
    fun splitTopLevel(text: String, sep: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        var quote: Char? = null
        for (c in text) {
            when {
                quote != null -> { current.append(c); if (c == quote) quote = null }
                c == '"' || c == '\'' -> { quote = c; current.append(c) }
                c == '(' -> { depth++; current.append(c) }
                c == ')' -> { if (depth > 0) depth--; current.append(c) }
                c == sep && depth == 0 -> { parts.add(current.toString()); current.setLength(0) }
                else -> current.append(c)
            }
        }
        parts.add(current.toString())
        return parts
    }

    fun declarations(text: String, line: Int, warn: (String, Int) -> Unit): List<CssDeclaration> {
        val result = mutableListOf<CssDeclaration>()
        for (part in splitTopLevel(text, ';')) {
            val p = part.trim()
            if (p.isEmpty()) continue
            val colon = p.indexOf(':')
            if (colon <= 0) {
                warn("CSS: declaration without ':' was ignored ('$p')", line)
                continue
            }
            val name = p.substring(0, colon).trim().lowercase()
            val value = p.substring(colon + 1).trim()
                .replace(Regex("\\s*!important\\s*$", RegexOption.IGNORE_CASE), "")
            if (value.isEmpty()) {
                warn("CSS: property '$name' has no value and was ignored", line)
                continue
            }
            result.add(CssDeclaration(name, value, line))
        }
        return result
    }
}

object CssValues {
    private val OPAQUE: Int = 0xFF000000.toInt()

    private val NAMES: Map<String, Int> = mapOf(
        "black" to 0x000000, "white" to 0xFFFFFF, "red" to 0xFF0000, "green" to 0x008000,
        "blue" to 0x0000FF, "yellow" to 0xFFFF00, "orange" to 0xFFA500, "purple" to 0x800080,
        "pink" to 0xFFC0CB, "gray" to 0x808080, "grey" to 0x808080, "silver" to 0xC0C0C0,
        "maroon" to 0x800000, "navy" to 0x000080, "teal" to 0x008080, "lime" to 0x00FF00,
        "aqua" to 0x00FFFF, "cyan" to 0x00FFFF, "magenta" to 0xFF00FF, "fuchsia" to 0xFF00FF,
        "olive" to 0x808000, "brown" to 0xA52A2A, "gold" to 0xFFD700, "indigo" to 0x4B0082,
        "violet" to 0xEE82EE, "coral" to 0xFF7F50, "crimson" to 0xDC143C, "tomato" to 0xFF6347,
        "salmon" to 0xFA8072, "khaki" to 0xF0E68C, "beige" to 0xF5F5DC, "ivory" to 0xFFFFF0,
        "lavender" to 0xE6E6FA, "turquoise" to 0x40E0D0, "darkgray" to 0xA9A9A9,
        "darkgrey" to 0xA9A9A9, "lightgray" to 0xD3D3D3, "lightgrey" to 0xD3D3D3,
        "darkblue" to 0x00008B, "darkgreen" to 0x006400, "darkred" to 0x8B0000,
        "lightblue" to 0xADD8E6, "lightgreen" to 0x90EE90, "whitesmoke" to 0xF5F5F5
    )

    private fun pack(r: Int, g: Int, b: Int, a: Int): Int = (a shl 24) or (r shl 16) or (g shl 8) or b

    fun color(text: String): Int? {
        val t = text.trim().lowercase()
        if (t.isEmpty()) return null
        if (t == "transparent") return 0
        NAMES[t]?.let { return it or OPAQUE }

        if (t.startsWith("#")) {
            val h = t.substring(1)
            if (h.isEmpty() || !h.all { it in '0'..'9' || it in 'a'..'f' }) return null
            fun dbl(p: Int) = h[p].digitToInt(16) * 17
            fun pair(p: Int) = h.substring(p, p + 2).toInt(16)
            return when (h.length) {
                3 -> pack(dbl(0), dbl(1), dbl(2), 255)
                4 -> pack(dbl(0), dbl(1), dbl(2), dbl(3))
                6 -> pack(pair(0), pair(2), pair(4), 255)
                8 -> pack(pair(0), pair(2), pair(4), pair(6))
                else -> null
            }
        }

        val m = Regex("""^rgba?\(([^)]*)\)$""").matchEntire(t) ?: return null
        val parts = m.groupValues[1].split(',', ' ', '/').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size !in 3..4) return null
        fun channel(s: String): Int? =
            if (s.endsWith("%")) s.dropLast(1).toFloatOrNull()?.let { (it * 2.55f).roundToInt() }
            else s.toFloatOrNull()?.roundToInt()
        val r = channel(parts[0]) ?: return null
        val g = channel(parts[1]) ?: return null
        val b = channel(parts[2]) ?: return null
        var a = 255
        if (parts.size == 4) {
            val s = parts[3]
            val v = (if (s.endsWith("%")) s.dropLast(1).toFloatOrNull()?.div(100f) else s.toFloatOrNull())
                ?: return null
            a = (v.coerceIn(0f, 1f) * 255f).roundToInt()
        }
        return pack(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), a)
    }

    private val REGEX_LENGTH = Regex("""^(-?\d*\.?\d+)(px|rem)$""")

    fun length(text: String): Length? {
        val t = text.trim().lowercase()
        if (t == "0") return Length.ZERO
        val m = REGEX_LENGTH.matchEntire(t) ?: return null
        val n = m.groupValues[1].toFloatOrNull() ?: return null
        return if (m.groupValues[2] == "rem") Length.rem(n) else Length.px(n)
    }

    fun size(text: String): Size? {
        val t = text.trim().lowercase()
        if (t == "auto") return Size.Auto
        if (t.endsWith("%")) return t.dropLast(1).toFloatOrNull()?.let { Size.Percent(it) }
        return length(t)?.let { Size.Dim(it) }
    }
}
