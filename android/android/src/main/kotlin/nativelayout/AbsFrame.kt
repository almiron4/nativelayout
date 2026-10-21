/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-21
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * The view of a box that has `position: absolute` children. The first child is the box itself
 * ([flow], where the children in the flow go); the others are the absolute children, placed by
 * top, right, bottom and left. They take no space in the flow.
 *
 * They are drawn above the flow, in the order of `z-index` and then in the order written. A
 * negative `z-index` puts a child under the flow.
 */
internal class AbsFrame(context: Context) : FrameLayout(context) {
    class Lp(width: Int, height: Int) : FrameLayout.LayoutParams(width, height) {
        var z: Int = 0
    }

    val flow = NLBox(context)

    init {
        clipChildren = false
        clipToPadding = false
        isChildrenDrawingOrderEnabled = true
        addView(flow, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    /** Adds an absolute child. [index] counts only the absolute children. */
    fun addAbs(view: View, index: Int = Int.MAX_VALUE) {
        val at = (1 + index).coerceIn(1, childCount)
        addView(view, at)
    }

    private fun zOf(i: Int): Int = ((getChildAt(i).layoutParams as? Lp)?.z) ?: 0

    override fun getChildDrawingOrder(childCount: Int, drawingPosition: Int): Int {
        val order = (0 until childCount).sortedWith(compareBy({ zOf(it) }, { it }))
        return order[drawingPosition]
    }
}
