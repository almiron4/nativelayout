/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-21
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import nativelayout.core.ImageFit
import nativelayout.core.ImagePos

/**
 * `background-image`: draws [bitmap] inside the box, by `background-size` ([fit]) and
 * `background-position` ([pos]), cut to the rounded corners given by [radii] (eight values) or [radius].
 */
internal class ImageBackground(
    private val bitmap: Bitmap,
    private val fit: ImageFit,
    private val pos: ImagePos,
    private val radius: Float,
    private val radii: FloatArray?
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val clip = Path()
    private val src = Rect()
    private val dst = RectF()

    override fun onBoundsChange(bounds: Rect) {
        clip.reset()
        val r = RectF(bounds)
        if (radii != null) clip.addRoundRect(r, radii, Path.Direction.CW)
        else clip.addRoundRect(r, radius, radius, Path.Direction.CW)
        compute(bounds)
    }

    private fun compute(b: Rect) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        if (bw <= 0f || bh <= 0f || w <= 0f || h <= 0f) {
            src.setEmpty()
            dst.setEmpty()
            return
        }
        when (fit) {
            ImageFit.FILL -> {
                src.set(0, 0, bitmap.width, bitmap.height)
                dst.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
            }
            ImageFit.COVER -> {
                // the image fills the box and the part that does not fit is cut
                val scale = maxOf(w / bw, h / bh)
                val sw = w / scale
                val sh = h / scale
                val sx = when (pos) {
                    ImagePos.LEFT -> 0f
                    ImagePos.RIGHT -> bw - sw
                    else -> (bw - sw) / 2f
                }
                val sy = when (pos) {
                    ImagePos.TOP -> 0f
                    ImagePos.BOTTOM -> bh - sh
                    else -> (bh - sh) / 2f
                }
                src.set(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt())
                dst.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
            }
            ImageFit.CONTAIN -> {
                // the whole image is seen, and the box has empty space on one side
                val scale = minOf(w / bw, h / bh)
                val dw = bw * scale
                val dh = bh * scale
                val dx = when (pos) {
                    ImagePos.LEFT -> 0f
                    ImagePos.RIGHT -> w - dw
                    else -> (w - dw) / 2f
                }
                val dy = when (pos) {
                    ImagePos.TOP -> 0f
                    ImagePos.BOTTOM -> h - dh
                    else -> (h - dh) / 2f
                }
                src.set(0, 0, bitmap.width, bitmap.height)
                dst.set(b.left + dx, b.top + dy, b.left + dx + dw, b.top + dy + dh)
            }
        }
    }

    override fun draw(canvas: Canvas) {
        if (src.isEmpty || dst.isEmpty) return
        val save = canvas.save()
        canvas.clipPath(clip)
        canvas.drawBitmap(bitmap, src, dst, paint)
        canvas.restoreToCount(save)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Required by Drawable")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
