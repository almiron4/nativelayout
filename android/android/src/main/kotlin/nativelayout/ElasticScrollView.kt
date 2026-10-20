/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-20
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout

import android.animation.ValueAnimator
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView
import kotlin.math.abs
import kotlin.math.min

/**
 * A ScrollView that stretches like rubber at the ends, for Android before 12, which has no
 * stretch effect of its own. The content follows the finger with resistance and springs back.
 */
internal class ElasticScrollView(context: Context) : ScrollView(context) {
    private var lastY = 0f
    private var anim: ValueAnimator? = null

    init {
        overScrollMode = OVER_SCROLL_NEVER
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val child = getChildAt(0) ?: return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                anim?.cancel()
                lastY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.y - lastY
                lastY = ev.y
                val t = child.translationY
                val pullDown = t > 0f || (dy > 0f && !canScrollVertically(-1))
                val pullUp = t < 0f || (dy < 0f && !canScrollVertically(1))
                if (pullDown || pullUp) {
                    val returning = t != 0f && dy * t < 0f
                    val resist = if (returning) 1f else 0.5f * (1f - min(abs(t) / height, 0.8f))
                    var next = t + dy * resist
                    if ((t > 0f && next < 0f) || (t < 0f && next > 0f)) next = 0f
                    child.translationY = next
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> springBack(child)
        }
        return super.onTouchEvent(ev)
    }

    private fun springBack(child: View) {
        val from = child.translationY
        if (from == 0f) return
        anim = ValueAnimator.ofFloat(from, 0f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { child.translationY = it.animatedValue as Float }
            start()
        }
    }
}
