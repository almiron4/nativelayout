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
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

internal val COLOR_ERROR = Color.parseColor("#C62828")
internal val COLOR_WARNING = Color.parseColor("#B26A00")
private val COLOR_PANEL = Color.parseColor("#EFE9D8")
private val COLOR_BALLOON = Color.parseColor("#FFFBF2")
private val COLOR_BORDER = Color.parseColor("#E8E0CE")
private val COLOR_TEXT = Color.parseColor("#3B382F")
private val COLOR_MUTED = Color.parseColor("#6F6858")

internal fun Context.dpPx(value: Int): Int = (value * resources.displayMetrics.density).toInt()

internal fun colorOf(s: Severity): Int = if (s == Severity.ERROR) COLOR_ERROR else COLOR_WARNING

internal fun labelOf(s: Severity): String = if (s == Severity.ERROR) "ERROR" else "WARNING"

private val iconPath = Path()

internal fun drawIcon(canvas: Canvas, s: Severity, cx: Float, cy: Float, r: Float, paint: Paint) {
    val styleBefore = paint.style
    val colorBefore = paint.color
    val widthBefore = paint.strokeWidth
    val capBefore = paint.strokeCap

    paint.style = Paint.Style.FILL
    paint.color = colorOf(s)
    val strokeTop: Float
    val strokeEnd: Float
    val dotCenter: Float
    if (s == Severity.ERROR) {
        canvas.drawCircle(cx, cy, r, paint)
        strokeTop = cy - r * 0.5f
        strokeEnd = cy + r * 0.1f
        dotCenter = cy + r * 0.5f
    } else {
        iconPath.reset()
        iconPath.moveTo(cx, cy - r)
        iconPath.lineTo(cx + r, cy + r * 0.85f)
        iconPath.lineTo(cx - r, cy + r * 0.85f)
        iconPath.close()
        canvas.drawPath(iconPath, paint)
        strokeTop = cy - r * 0.15f
        strokeEnd = cy + r * 0.3f
        dotCenter = cy + r * 0.62f
    }

    paint.color = Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = r * 0.28f
    paint.strokeCap = Paint.Cap.ROUND
    canvas.drawLine(cx, strokeTop, cx, strokeEnd, paint)
    paint.style = Paint.Style.FILL
    canvas.drawCircle(cx, dotCenter, r * 0.15f, paint)

    paint.style = styleBefore
    paint.color = colorBefore
    paint.strokeWidth = widthBefore
    paint.strokeCap = capBefore
}

internal class SeverityIconView(context: Context, initial: Severity) : View(context) {
    var severity: Severity = initial
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val r = minOf(width, height) / 2f - 1f
        drawIcon(canvas, severity, width / 2f, height / 2f, r, paint)
    }
}

private fun pill(c: Context, label: String, s: Severity): TextView {
    return TextView(c).apply {
        text = label
        textSize = 10f
        setTypeface(null, Typeface.BOLD)
        setTextColor(Color.WHITE)
        setPadding(c.dpPx(6), c.dpPx(2), c.dpPx(6), c.dpPx(2))
        background = GradientDrawable().apply {
            setColor(colorOf(s))
            cornerRadius = c.dpPx(4).toFloat()
        }
    }
}

class ProblemsBar(context: Context) : LinearLayout(context) {

    var onTap: (() -> Unit)? = null
    var current: Diagnostic? = null
        private set

    private val icon = SeverityIconView(context, Severity.ERROR)
    private val label = TextView(context)
    private val lineText = TextView(context)
    private val message = TextView(context)
    private val counter = TextView(context)
    private val content = LinearLayout(context)

    private fun dp(value: Int): Int = context.dpPx(value)

    init {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        setBackgroundColor(COLOR_PANEL)

        addView(View(context).apply { setBackgroundColor(COLOR_BORDER) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))

        content.orientation = LinearLayout.HORIZONTAL
        content.gravity = Gravity.CENTER_VERTICAL
        content.setPadding(dp(12), 0, dp(12), 0)
        content.isClickable = true
        content.setOnClickListener { onTap?.invoke() }

        content.addView(icon, LinearLayout.LayoutParams(dp(18), dp(18)).apply { rightMargin = dp(8) })

        label.textSize = 10f
        label.setTypeface(null, Typeface.BOLD)
        content.addView(label, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(8) })

        lineText.textSize = 12f
        lineText.typeface = Typeface.MONOSPACE
        lineText.setTextColor(COLOR_MUTED)
        content.addView(lineText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(8) })

        message.textSize = 13f
        message.setTextColor(COLOR_TEXT)
        message.maxLines = 1
        message.ellipsize = TextUtils.TruncateAt.END
        content.addView(message, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        counter.textSize = 11f
        counter.setTypeface(null, Typeface.BOLD)
        counter.setTextColor(COLOR_MUTED)
        counter.gravity = Gravity.CENTER
        counter.setPadding(dp(8), dp(2), dp(8), dp(2))
        counter.background = GradientDrawable().apply {
            setStroke(dp(1), COLOR_BORDER)
            cornerRadius = dp(10).toFloat()
        }
        content.addView(counter, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(8) })

        addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40)))
    }

    fun show(list: List<Diagnostic>, cursorLine: Int, outdated: Boolean) {
        if (list.isEmpty()) {
            current = null
            visibility = View.GONE
            return
        }
        visibility = View.VISIBLE
        val d = list.firstOrNull { it.line == cursorLine && it.severity == Severity.ERROR }
            ?: list.firstOrNull { it.line == cursorLine }
            ?: list.firstOrNull { it.severity == Severity.ERROR }
            ?: list.first()
        current = d

        icon.severity = d.severity
        label.text = labelOf(d.severity)
        label.setTextColor(Color.WHITE)
        label.setPadding(dp(6), dp(2), dp(6), dp(2))
        label.background = GradientDrawable().apply {
            setColor(colorOf(d.severity))
            cornerRadius = dp(4).toFloat()
        }
        lineText.text = "L" + d.line
        message.text = d.message
        counter.text = list.size.toString()
        counter.visibility = if (list.size > 1) View.VISIBLE else View.GONE
        content.alpha = if (outdated) 0.5f else 1f
    }
}

internal object DiagnosticBalloon {

    private var popup: PopupWindow? = null

    fun dismiss() {
        popup?.dismiss()
        popup = null
    }

    fun show(anchor: View, list: List<Diagnostic>, screenX: Int, yBelow: Int, yAbove: Int) {
        dismiss()
        if (list.isEmpty()) return
        val c = anchor.context
        val worst = if (list.any { it.severity == Severity.ERROR }) Severity.ERROR else Severity.WARNING

        val content = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(COLOR_BALLOON)
                cornerRadius = c.dpPx(10).toFloat()
                setStroke(c.dpPx(1), colorOf(worst))
            }
            setPadding(c.dpPx(12), c.dpPx(10), c.dpPx(12), c.dpPx(10))
        }
        list.forEachIndexed { i, d ->
            if (i > 0) {
                content.addView(View(c).apply { setBackgroundColor(COLOR_BORDER) },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                        topMargin = c.dpPx(8)
                        bottomMargin = c.dpPx(8)
                    })
            }
            content.addView(blockOf(c, d))
        }

        val screen = c.resources.displayMetrics
        val width = minOf(c.dpPx(320), screen.widthPixels - c.dpPx(24))
        content.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val height = content.measuredHeight
        val fitsBelow = yBelow + height + c.dpPx(8) < screen.heightPixels - c.dpPx(120)
        val y = if (fitsBelow) yBelow + c.dpPx(6) else (yAbove - height - c.dpPx(6)).coerceAtLeast(c.dpPx(24))
        val x = screenX.coerceIn(c.dpPx(12), (screen.widthPixels - width - c.dpPx(12)).coerceAtLeast(c.dpPx(12)))

        val window = PopupWindow(content, width, ViewGroup.LayoutParams.WRAP_CONTENT, false)
        window.isOutsideTouchable = true
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
        popup = window
        content.postDelayed({ if (popup === window) dismiss() }, 7000)
    }

    private fun blockOf(c: Context, d: Diagnostic): View {
        val block = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }

        val header = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(SeverityIconView(c, d.severity),
            LinearLayout.LayoutParams(c.dpPx(16), c.dpPx(16)).apply { rightMargin = c.dpPx(8) })
        header.addView(pill(c, labelOf(d.severity), d.severity),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = c.dpPx(8) })
        header.addView(TextView(c).apply {
            text = "line " + d.line
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextColor(COLOR_MUTED)
        })
        block.addView(header)

        block.addView(TextView(c).apply {
            text = d.message
            textSize = 14f
            setTextColor(COLOR_TEXT)
            setPadding(0, c.dpPx(6), 0, 0)
        })

        val suggestion = d.suggestion
        if (!suggestion.isNullOrEmpty()) {
            block.addView(TextView(c).apply {
                text = "Suggestion: " + suggestion
                textSize = 13f
                setTextColor(COLOR_MUTED)
                setPadding(0, c.dpPx(4), 0, 0)
            })
        }
        return block
    }
}
