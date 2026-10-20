/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-20
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import nativelayout.core.IconSet
import nativelayout.core.IconShape

/**
 * A built-in icon (`<img src="icon:name">`). It is drawn in white and scaled to its bounds; the host
 * paints it with the text color through a color filter.
 */
internal class IconDrawable(private val shapes: List<IconShape>, private val sizePx: Int) : Drawable() {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = IconSet.STROKE
        color = Color.WHITE
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val path = Path()
    private val rect = RectF()

    override fun getIntrinsicWidth(): Int = sizePx

    override fun getIntrinsicHeight(): Int = sizePx

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val scale = minOf(b.width(), b.height()) / IconSet.GRID
        val dx = b.left + (b.width() - IconSet.GRID * scale) / 2f
        val dy = b.top + (b.height() - IconSet.GRID * scale) / 2f
        val saved = canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        for (shape in shapes) {
            when (shape) {
                is IconShape.Line -> {
                    val p = shape.points
                    if (p.size >= 4) {
                        path.rewind()
                        path.moveTo(p[0], p[1])
                        var i = 2
                        while (i + 1 < p.size) {
                            path.lineTo(p[i], p[i + 1])
                            i += 2
                        }
                        if (shape.closed) path.close()
                        canvas.drawPath(path, stroke)
                    }
                }
                is IconShape.Circle -> canvas.drawCircle(shape.cx, shape.cy, shape.r, stroke)
                is IconShape.Dot -> canvas.drawCircle(shape.cx, shape.cy, IconSet.DOT_RADIUS, fill)
                is IconShape.Rect -> {
                    rect.set(shape.left, shape.top, shape.right, shape.bottom)
                    canvas.drawRoundRect(rect, shape.radius, shape.radius, stroke)
                }
            }
        }
        canvas.restoreToCount(saved)
    }

    override fun setAlpha(alpha: Int) {
        stroke.alpha = alpha
        fill.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        stroke.colorFilter = colorFilter
        fill.colorFilter = colorFilter
        invalidateSelf()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
