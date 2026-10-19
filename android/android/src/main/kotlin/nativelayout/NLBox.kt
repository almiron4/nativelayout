/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-18
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import nativelayout.core.Justify

/**
 * The box used for div, ul and li. On top of a LinearLayout it knows max-width and max-height,
 * justify-content: space-between, space-around and space-evenly, and flex-wrap in rows.
 */
internal class NLBox(context: Context) : LinearLayout(context) {
    var maxWidthPx: Int = -1
    var maxHeightPx: Int = -1
    var justify: Justify = Justify.START
    var wrap: Boolean = false
    var gapPx: Int = 0

    private class Line {
        val items = ArrayList<View>()
        var width = 0
        var height = 0
    }

    private val lines = ArrayList<Line>()

    private val wrapped: Boolean get() = wrap && orientation == HORIZONTAL

    private fun clamp(spec: Int, max: Int): Int {
        if (max < 0) return spec
        val size = MeasureSpec.getSize(spec)
        return when (MeasureSpec.getMode(spec)) {
            MeasureSpec.EXACTLY ->
                if (size > max) MeasureSpec.makeMeasureSpec(max, MeasureSpec.EXACTLY) else spec
            MeasureSpec.AT_MOST -> MeasureSpec.makeMeasureSpec(minOf(size, max), MeasureSpec.AT_MOST)
            else -> MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = clamp(widthMeasureSpec, maxWidthPx)
        val h = clamp(heightMeasureSpec, maxHeightPx)
        if (wrapped) measureWrap(w, h) else super.onMeasure(w, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (wrapped) {
            layoutWrap(r - l)
        } else {
            super.onLayout(changed, l, t, r, b)
            spread(r - l, b - t)
        }
    }

    // ---------- space-between, space-around, space-evenly (no wrap) ----------

    private fun spreads(): Boolean =
        justify == Justify.BETWEEN || justify == Justify.AROUND || justify == Justify.EVENLY

    private fun spread(width: Int, height: Int) {
        if (!spreads()) return
        val items = ArrayList<View>()
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility != GONE) items.add(c)
        }
        val n = items.size
        if (n == 0 || (n == 1 && justify == Justify.BETWEEN)) return
        val vertical = orientation == VERTICAL
        val first = items[0]
        val last = items[n - 1]
        val fp = first.layoutParams as MarginLayoutParams
        val lp = last.layoutParams as MarginLayoutParams
        val start = if (vertical) first.top - fp.topMargin else first.left - fp.leftMargin
        val end = if (vertical) last.bottom + lp.bottomMargin else last.right + lp.rightMargin
        val available =
            if (vertical) height - paddingTop - paddingBottom else width - paddingLeft - paddingRight
        val free = available - (end - start)
        if (free <= 0) return
        for (i in 0 until n) {
            val off = when (justify) {
                Justify.BETWEEN -> free * i / (n - 1)
                Justify.AROUND -> free * (2 * i + 1) / (2 * n)
                else -> free * (i + 1) / (n + 1)
            }
            if (vertical) items[i].offsetTopAndBottom(off) else items[i].offsetLeftAndRight(off)
        }
    }

    // ---------- flex-wrap (rows) ----------

    private fun measureWrap(widthSpec: Int, heightSpec: Int) {
        lines.clear()
        val mode = MeasureSpec.getMode(widthSpec)
        val available =
            if (mode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE
            else MeasureSpec.getSize(widthSpec) - paddingLeft - paddingRight
        var line = Line()
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == GONE) continue
            val lp = c.layoutParams as MarginLayoutParams
            if (lp is LayoutParams && lp.weight > 0f && lp.width == 0) {
                // flex does not grow inside a wrapped row: the item keeps its own width
                val cw = getChildMeasureSpec(
                    widthSpec, paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val ch = getChildMeasureSpec(
                    heightSpec, paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin, lp.height
                )
                c.measure(cw, ch)
            } else {
                measureChildWithMargins(c, widthSpec, 0, heightSpec, 0)
            }
            val cw = c.measuredWidth + lp.leftMargin + lp.rightMargin
            val ch = c.measuredHeight + lp.topMargin + lp.bottomMargin
            if (line.items.isNotEmpty() && line.width + gapPx + cw > available) {
                lines.add(line)
                line = Line()
            }
            line.width += (if (line.items.isEmpty()) 0 else gapPx) + cw
            line.height = maxOf(line.height, ch)
            line.items.add(c)
        }
        if (line.items.isNotEmpty()) lines.add(line)

        var contentW = 0
        var contentH = 0
        for ((k, l) in lines.withIndex()) {
            contentW = maxOf(contentW, l.width)
            contentH += l.height + (if (k > 0) gapPx else 0)
        }
        val totalW = maxOf(contentW + paddingLeft + paddingRight, suggestedMinimumWidth)
        val totalH = maxOf(contentH + paddingTop + paddingBottom, suggestedMinimumHeight)
        setMeasuredDimension(resolveSize(totalW, widthSpec), resolveSize(totalH, heightSpec))
    }

    private fun layoutWrap(width: Int) {
        val available = width - paddingLeft - paddingRight
        var y = paddingTop
        for ((k, line) in lines.withIndex()) {
            if (k > 0) y += gapPx
            val n = line.items.size
            val free = maxOf(available - line.width, 0)
            var lead = 0
            var extra = 0f
            when (justify) {
                Justify.CENTER -> lead = free / 2
                Justify.END -> lead = free
                Justify.BETWEEN -> extra = if (n > 1) free.toFloat() / (n - 1) else 0f
                Justify.AROUND -> { extra = free.toFloat() / n; lead = (extra / 2f).toInt() }
                Justify.EVENLY -> { extra = free.toFloat() / (n + 1); lead = extra.toInt() }
                Justify.START -> {}
            }
            var x = paddingLeft + lead
            for (c in line.items) {
                val lp = c.layoutParams as MarginLayoutParams
                val gravity = (lp as? LayoutParams)?.gravity ?: -1
                val vertical = if (gravity == -1) Gravity.TOP else gravity and Gravity.VERTICAL_GRAVITY_MASK
                val outer = c.measuredHeight + lp.topMargin + lp.bottomMargin
                val shift = when (vertical) {
                    Gravity.CENTER_VERTICAL -> (line.height - outer) / 2
                    Gravity.BOTTOM -> line.height - outer
                    else -> 0
                }
                val left = x + lp.leftMargin
                val top = y + lp.topMargin + shift
                c.layout(left, top, left + c.measuredWidth, top + c.measuredHeight)
                x += c.measuredWidth + lp.leftMargin + lp.rightMargin + gapPx + extra.toInt()
            }
            y += line.height
        }
    }
}
