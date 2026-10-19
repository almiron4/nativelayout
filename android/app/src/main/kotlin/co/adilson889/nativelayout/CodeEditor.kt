/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package co.adilson889.nativelayout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.EditText
import kotlin.math.abs

class CodeEditor(context: Context) : EditText(context) {

    private val numberColor = Color.parseColor("#A89F8C")
    private val numberErrorColor = Color.parseColor("#C62828")
    private val numberWarningColor = Color.parseColor("#B26A00")

    private val gutterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = numberColor
        textAlign = Paint.Align.RIGHT
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val wavyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val wavyPath = Path()

    private var gutterWidth = 0
    private var gutterSpan: LeadingMarginSpan.Standard? = null
    private val visibleArea = Rect()

    var diagnostics: List<Diagnostic> = emptyList()
        private set
    private var byLine: Map<Int, List<Diagnostic>> = emptyMap()

    var outdated: Boolean = false
        private set

    var onDiagnosticsChanged: ((List<Diagnostic>, Boolean) -> Unit)? = null

    var onLineChanged: ((Int) -> Unit)? = null

    var onMarkerTapped: ((List<Diagnostic>, Int, Int, Int) -> Unit)? = null

    private var lastCursorLine = -1
    private var touchOnGutter = false
    private var touchX = 0f
    private var touchY = 0f
    private val tolerance = ViewConfiguration.get(context).scaledTouchSlop

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    init {
        wavyPaint.strokeWidth = dp(1).toFloat().coerceAtLeast(2f)
    }

    fun updateGutter() {
        gutterPaint.textSize = textSize
        val digits = lineCount.toString().length.coerceAtLeast(2)
        val fresh = (gutterPaint.measureText("0".repeat(digits)) + dp(34)).toInt()
        val content = text ?: return

        val current = gutterSpan
        val coversAll = current != null &&
            content.getSpanStart(current) == 0 &&
            content.getSpanEnd(current) == content.length
        if (fresh != gutterWidth || !coversAll) {
            gutterWidth = fresh
            if (current != null) content.removeSpan(current)
            val span = LeadingMarginSpan.Standard(gutterWidth)
            content.setSpan(span, 0, content.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            gutterSpan = span
        }
    }

    fun setDiagnostics(list: List<Diagnostic>) {
        diagnostics = list
        byLine = list.groupBy { it.line }
        outdated = false
        invalidate()
        onDiagnosticsChanged?.invoke(list, false)
    }

    fun clearDiagnostics() {
        if (diagnostics.isEmpty() && !outdated) return
        diagnostics = emptyList()
        byLine = emptyMap()
        outdated = false
        invalidate()
        onDiagnosticsChanged?.invoke(emptyList(), false)
    }

    fun markOutdated() {
        if (diagnostics.isEmpty() || outdated) return
        outdated = true
        invalidate()
        onDiagnosticsChanged?.invoke(diagnostics, true)
    }

    fun goToLine(line1: Int, start: Int = -1) {
        val t = text ?: return
        var target = start
        if (target < 0 || target > t.length) {
            var idx = 0
            var n = 1
            while (n < line1) {
                val p = t.indexOf('\n', idx)
                if (p < 0) break
                idx = p + 1
                n++
            }
            target = idx
        }
        requestFocus()
        setSelection(target.coerceIn(0, t.length))
    }

    private fun worstSeverity(list: List<Diagnostic>?): Severity? {
        if (list == null || list.isEmpty()) return null
        return if (list.any { it.severity == Severity.ERROR }) Severity.ERROR else Severity.WARNING
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        val l = layout ?: return
        val t = text ?: return
        if (selStart < 0) return
        val line1 = l.getLineForOffset(selStart.coerceAtMost(t.length)) + 1
        if (line1 != lastCursorLine) {
            lastCursorLine = line1
            onLineChanged?.invoke(line1)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
                touchOnGutter = gutterWidth > 0 && (event.x + scrollX) < paddingLeft + gutterWidth
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchOnGutter && (abs(event.x - touchX) > tolerance || abs(event.y - touchY) > tolerance)) {
                    touchOnGutter = false
                }
            }
            MotionEvent.ACTION_UP -> {
                if (touchOnGutter) {
                    touchOnGutter = false
                    handleGutterTap(event)
                }
            }
            MotionEvent.ACTION_CANCEL -> touchOnGutter = false
        }
        return super.onTouchEvent(event)
    }

    private fun handleGutterTap(event: MotionEvent) {
        if (outdated || byLine.isEmpty()) return
        val l = layout ?: return
        val index = l.getLineForVertical((event.y + scrollY - paddingTop).toInt().coerceAtLeast(0))
        val list = byLine[index + 1] ?: return
        val position = IntArray(2)
        getLocationOnScreen(position)
        val top = position[1] + (l.getLineTop(index) + paddingTop - scrollY)
        val bottom = position[1] + (l.getLineBottom(index) + paddingTop - scrollY)
        onMarkerTapped?.invoke(list, position[0] + dp(8), bottom, top)
    }

    private fun drawWavy(canvas: Canvas, l: Layout, i: Int, baseline: Int, d: Diagnostic) {
        val t = text ?: return
        val lineStart = l.getLineStart(i)
        var lineEnd = l.getLineEnd(i)
        if (lineEnd > lineStart && t[lineEnd - 1] == '\n') lineEnd--

        val a: Int
        val b: Int
        if (d.isWord) {
            a = maxOf(d.start, lineStart)
            b = minOf(d.end, lineEnd)
        } else {
            var p = lineStart
            while (p < lineEnd && t[p].isWhitespace()) p++
            a = p
            b = lineEnd
        }
        if (b <= a) return

        val x1 = paddingLeft + l.getPrimaryHorizontal(a)
        val x2 = paddingLeft + l.getPrimaryHorizontal(b)
        val y = baseline + dp(3).toFloat()
        val amplitude = dp(3) / 2f
        val step = dp(3).toFloat().coerceAtLeast(3f)

        wavyPaint.color = colorOf(d.severity)
        wavyPath.reset()
        wavyPath.moveTo(x1, y)
        var x = x1
        var up = true
        while (x < x2) {
            x = minOf(x + step, x2)
            wavyPath.lineTo(x, if (up) y - amplitude else y + amplitude)
            up = !up
        }
        canvas.drawPath(wavyPath, wavyPaint)
    }

    private var drawing = false

    override fun getExtendedPaddingBottom(): Int =
        if (drawing) 0 else super.getExtendedPaddingBottom()

    override fun getCompoundPaddingRight(): Int =
        if (drawing) 0 else super.getCompoundPaddingRight()

    override fun onDraw(canvas: Canvas) {
        val currentLayout = layout
        if (currentLayout != null && gutterWidth > 0 && lineCount > 0) {
            gutterPaint.textSize = textSize
            canvas.getClipBounds(visibleArea)
            val first = currentLayout.getLineForVertical((visibleArea.top - paddingTop).coerceAtLeast(0))
            val last = currentLayout.getLineForVertical((visibleArea.bottom - paddingTop).coerceAtLeast(0))

            val x = (paddingLeft + gutterWidth - dp(16)).toFloat()
            val iconX = (paddingLeft + gutterWidth - dp(8)).toFloat()
            val iconRadius = dp(5).toFloat()
            val visible = !outdated

            for (i in first..last.coerceAtMost(lineCount - 1)) {
                val baseline = currentLayout.getLineBaseline(i) + paddingTop
                val line1 = i + 1
                val onLine = if (visible) byLine[line1] else null
                val severity = worstSeverity(onLine)

                gutterPaint.color = when (severity) {
                    Severity.ERROR -> numberErrorColor
                    Severity.WARNING -> numberWarningColor
                    null -> numberColor
                }
                canvas.drawText(line1.toString(), x, baseline.toFloat(), gutterPaint)

                if (onLine != null && severity != null) {
                    drawIcon(canvas, severity, iconX, baseline - dp(5).toFloat(), iconRadius, iconPaint)
                    for (d in onLine) drawWavy(canvas, currentLayout, i, baseline, d)
                }
            }
        }
        drawing = true
        try {
            super.onDraw(canvas)
        } finally {
            drawing = false
        }
    }
}
