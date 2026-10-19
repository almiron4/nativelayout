/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package co.adilson889.nativelayout

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan

class MarkupHighlighter(
    private val editor: CodeEditor,
    private val onCheck: (String) -> Unit
) : TextWatcher {

    private val tagColor = Color.parseColor("#0A3C9E")
    private val attributeColor = Color.parseColor("#0A5588")
    private val stringColor = Color.parseColor("#8A3A10")
    private val numberColor = Color.parseColor("#0A5C36")
    private val commentColor = Color.parseColor("#7A7462")
    private val keywordColor = Color.parseColor("#9A1B48")
    private val selectorColor = Color.parseColor("#6B4E0A")
    private val valueColor = Color.parseColor("#B3261E")

    private var updating = false
    private var lengthBefore = 0
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    companion object {
        private const val DEBOUNCE_MS = 350L
        private val TAG = Regex("""</?[A-Za-z][^<>]*>""")
        private val TAG_NAME = Regex("""^</?[A-Za-z][A-Za-z0-9-]*""")
        private val ATTRIBUTE = Regex("""([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)""")
        private val COMMENT = Regex("""<!--[\s\S]*?-->""")
        private val DECLARATION = Regex("""([-a-zA-Z]+)\s*:\s*([^;"']*)""")
        private val NUMBER = Regex("""#[0-9a-fA-F]{3,8}|-?\d+(?:\.\d+)?[a-zA-Z%]*""")
        private val OPEN_TAG = Regex("""<([A-Za-z][A-Za-z0-9-]*)(?:\s[^<>]*)?>""")
        private val CLOSE_TAG = Regex("""</[A-Za-z][A-Za-z0-9-]*>""")
        private val STYLE_BLOCK = Regex("""<style[^>]*>([\s\S]*?)</style>""")
        private val RULE = Regex("""([^{}]+)\{([^{}]*)\}""")
        private val CSS_COMMENT = Regex("""/\*[\s\S]*?\*/""")
        private val CONTROL = Regex("""^[ \t]*(?:if\s.*:|elif\s.*:|else:|for\s.*:|end)[ \t]*$""", RegexOption.MULTILINE)
        private val CONTROL_OPEN = Regex("""(?:if|elif|for)\s.*:|else:""")
        private val CONTROL_DEDENT = Regex("""end|else:|elif\s.*:""")
        private val VALUE = Regex("""\{[^{}]*\}""")
        private val VOID_TAGS = setOf("br", "hr", "img", "input", "meta", "link")
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        lengthBefore = s?.length ?: 0
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(editable: Editable) {
        if (updating) return
        updating = true
        try {
            val haviaTexto = lengthBefore > 0
            if (haviaTexto && editable.length > lengthBefore) {
                autoDedent(editable)
                autoIndent(editable)
            }
            highlight(editable)
            editor.updateGutter()
            schedule(editable.toString())
        } finally {
            updating = false
        }
        editor.post { editor.markOutdated() }
    }

    private fun schedule(text: String) {
        pending?.let { handler.removeCallbacks(it) }
        val task = Runnable { onCheck(text) }
        pending = task
        handler.postDelayed(task, DEBOUNCE_MS)
    }

    private fun opensBlock(line: String): Boolean {
        if (CONTROL_OPEN.matches(line)) return true
        val m = OPEN_TAG.matchEntire(line) ?: return false
        return m.groupValues[1].lowercase() !in VOID_TAGS && !line.endsWith("/>")
    }

    private fun autoIndent(editable: Editable) {
        val cursor = editor.selectionStart
        if (cursor < 1 || cursor > editable.length) return
        if (editable[cursor - 1] != '\n') return

        var lineStart = cursor - 1
        while (lineStart > 0 && editable[lineStart - 1] != '\n') lineStart--
        if (lineStart >= cursor) return
        val previous = editable.substring(lineStart, cursor - 1)
        var indent = Regex("^[ ]*").find(previous)?.value ?: ""
        if (opensBlock(previous.trim())) indent += "    "

        if (indent.isNotEmpty()) {
            editable.insert(cursor, indent)
            editor.setSelection((cursor + indent.length).coerceIn(0, editable.length))
        }
    }

    private fun autoDedent(editable: Editable) {
        val cursor = editor.selectionStart
        if (cursor < 1 || cursor > editable.length) return
        val typed = editable[cursor - 1]
        if (typed != '>' && typed != ':' && typed != 'd') return

        var lineStart = cursor - 1
        while (lineStart > 0 && editable[lineStart - 1] != '\n') lineStart--
        if (lineStart >= cursor) return
        val line = editable.substring(lineStart, cursor)
        val trimmed = line.trim()
        val dedent = if (typed == '>') CLOSE_TAG.matches(trimmed) else CONTROL_DEDENT.matches(trimmed)
        if (!dedent) return
        val remove = minOf(4, line.length - line.trimStart().length)
        if (remove > 0) {
            val novoCursor = (cursor - remove).coerceIn(0, editable.length - remove)
            editable.delete(lineStart, lineStart + remove)
            editor.setSelection(novoCursor.coerceIn(0, editable.length))
        }
    }

    private fun paint(editable: Editable, start: Int, end: Int, color: Int) {
        if (end > start && end <= editable.length && start >= 0) {
            editable.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun highlightStyle(editable: Editable, offset: Int, raw: String) {
        for (d in DECLARATION.findAll(raw)) {
            val name = d.groups[1]!!.range
            val value = d.groups[2]!!
            paint(editable, offset + name.first, offset + name.last + 1, attributeColor)
            if (value.value.isNotEmpty()) {
                val base = offset + value.range.first
                paint(editable, base, base + value.value.length, stringColor)
                for (n in NUMBER.findAll(value.value)) {
                    paint(editable, base + n.range.first, base + n.range.last + 1, numberColor)
                }
            }
        }
    }

    private fun highlight(editable: Editable) {
        val text = editable.toString()
        editable.getSpans(0, editable.length, ForegroundColorSpan::class.java).forEach {
            editable.removeSpan(it)
        }

        for (m in TAG.findAll(text)) {
            val start = m.range.first
            if (start > editable.length) continue
            val name = TAG_NAME.find(m.value) ?: continue
            paint(editable, start, start + name.value.length, tagColor)
            for (a in ATTRIBUTE.findAll(m.value, name.value.length)) {
                val key = a.groups[1]!!
                val value = a.groups[2]!!
                paint(editable, start + key.range.first, start + key.range.last + 1, attributeColor)
                paint(editable, start + value.range.first, start + value.range.last + 1, stringColor)
                if (key.value.equals("style", ignoreCase = true)) {
                    highlightStyle(editable, start + value.range.first, value.value)
                }
            }
            val tail = if (m.value.endsWith("/>")) 2 else 1
            paint(editable, m.range.last + 1 - tail, m.range.last + 1, tagColor)
        }
        for (c in COMMENT.findAll(text)) {
            paint(editable, c.range.first, c.range.last + 1, commentColor)
        }

        val styleRanges = ArrayList<IntRange>()
        for (b in STYLE_BLOCK.findAll(text)) {
            val css = b.groups[1]!!
            styleRanges.add(css.range)
            val base = css.range.first
            for (r in RULE.findAll(css.value)) {
                val selector = r.groups[1]!!.range
                paint(editable, base + selector.first, base + selector.last + 1, selectorColor)
                val declarations = r.groups[2]!!
                highlightStyle(editable, base + declarations.range.first, declarations.value)
            }
            for (c in CSS_COMMENT.findAll(css.value)) {
                paint(editable, base + c.range.first, base + c.range.last + 1, commentColor)
            }
        }

        for (c in CONTROL.findAll(text)) {
            if (styleRanges.none { c.range.first in it }) {
                paint(editable, c.range.first, c.range.last + 1, keywordColor)
            }
        }
        for (v in VALUE.findAll(text)) {
            if (styleRanges.none { v.range.first in it }) {
                paint(editable, v.range.first, v.range.last + 1, valueColor)
            }
        }
    }
}