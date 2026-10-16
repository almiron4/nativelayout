/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 */

package co.adilson889.nativelayout.android

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt
import co.adilson889.nativelayout.core.*

class AndroidLayoutHost(
    private val context: Context,
    private val areaProvider: () -> FrameLayout,
    private val imageResolver: (String) -> Bitmap? = { null }
) : LayoutHost {
    private class ViewEntry(
        val view: View,
        val container: ViewGroup?,
        var style: NativeStyle,
        var node: Native
    )

    private val entries = HashMap<String, ViewEntry>()
    private var rootKey: String? = null
    private var area: FrameLayout? = null

    private val queue = ArrayDeque<LayoutEvent>()
    private var pending: Continuation<LayoutEvent?>? = null
    private var closed = false

    private val prefs by lazy { context.getSharedPreferences("nativelayout", Context.MODE_PRIVATE) }

    private val density: Float get() = context.resources.displayMetrics.density
    private val fontScale: Float get() = context.resources.configuration.fontScale

    private fun px(c: Length): Int = c.toPixels(density, fontScale).roundToInt()
    private fun pxF(c: Length): Float = c.toPixels(density, fontScale)

    override fun applyChanges(changes: List<Change>) {
        if (closed) return
        for (a in changes) {
            when (a) {
                is Change.SetRoot -> setRoot(a.node)
                is Change.Create -> create(a)
                is Change.Remove -> remove(a.key)
                is Change.Replace -> replace(a)
                is Change.UpdateText -> updateText(a)
                is Change.UpdateStyle -> updateStyle(a)
                is Change.UpdateAttributes -> updateAttributes(a)
            }
        }
    }

    override suspend fun nextEvent(): LayoutEvent? {
        if (queue.isNotEmpty()) return queue.removeFirst()
        if (closed) return null
        return suspendCoroutine<LayoutEvent?> { continuation -> pending = continuation }
    }

    override fun fieldText(id: String): String? =
        (entries["#$id"]?.view as? EditText)?.text?.toString()

    override fun fieldChecked(id: String): Boolean? =
        (entries["#$id"]?.view as? CheckBox)?.isChecked

    override fun setValue(id: String, text: String) {
        when (val v = entries["#$id"]?.view) {
            is EditText -> {
                v.setText(text)
                v.setSelection(v.text.length)
            }
            is CheckBox -> v.isChecked = text.trim().lowercase() in setOf("1", "true", "on")
            else -> {}
        }
    }

    override fun save(key: String, text: String) {
        prefs.edit().putString(key, text).apply()
    }

    override fun load(key: String): String? = prefs.getString(key, null)

    override fun exists(key: String): Boolean = prefs.contains(key)

    override fun delete(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun close() {
        if (closed) return
        closed = true
        val continuation = pending
        pending = null
        queue.clear()
        reset()
        area = null
        continuation?.resume(null)
    }

    private fun publish(key: String) {
        if (closed) return
        val continuation = pending
        if (continuation != null) {
            pending = null
            continuation.resume(LayoutEvent(key))
        } else {
            queue.addLast(LayoutEvent(key))
        }
    }

    private fun reset() {
        entries.clear()
        rootKey = null
        area?.removeAllViews()
    }

    private fun setRoot(node: Native) {
        reset()
        val target = areaProvider()
        area = target
        val entry = createView(node, null)
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(entry.view, rootParams(node.style))
        }
        target.removeAllViews()
        target.addView(scroll, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        rootKey = node.key
    }

    private fun create(a: Change.Create) {
        val parentEntry = entries[a.parentKey] ?: return
        val container = parentEntry.container ?: return
        val created = createView(a.node, parentEntry.style)
        container.addView(created.view, minOf(a.position, container.childCount))
        applyPercents(created.view, created.style, container)
    }

    private fun remove(key: String) {
        val entry = entries[key] ?: return
        if (key == rootKey) {
            reset()
            return
        }
        discard(entry.view)
        (entry.view.parent as? ViewGroup)?.removeView(entry.view)
    }

    private fun replace(a: Change.Replace) {
        val old = entries[a.key] ?: return
        if (a.key == rootKey) {
            setRoot(a.node)
            return
        }
        val parent = old.view.parent as? ViewGroup ?: return
        val index = parent.indexOfChild(old.view)
        val parentEntry = entryOfParent(parent)
        discard(old.view)
        parent.removeView(old.view)
        val created = createView(a.node, parentEntry?.style)
        parent.addView(created.view, minOf(index, parent.childCount))
        applyPercents(created.view, created.style, parent)
    }

    private fun updateText(a: Change.UpdateText) {
        val entry = entries[a.key] ?: return
        (entry.view as? TextView)?.text = buildText(a.spans)
    }

    private fun updateStyle(a: Change.UpdateStyle) {
        val entry = entries[a.key] ?: return
        entry.style = a.style
        applyStyle(entry)

        val parent = entry.view.parent as? ViewGroup
        val parentEntry = entryOfParent(parent)
        if (parentEntry != null && parent != null) {
            entry.view.layoutParams = childParams(a.style, parentEntry.style)
            applyPercents(entry.view, a.style, parent)
        } else if (a.key == rootKey) {
            entry.view.layoutParams = rootParams(a.style)
        }

        val container = entry.container
        if (container != null) {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                val childEntry = (child.tag as? String)?.let { entries[it] } ?: continue
                child.layoutParams = childParams(childEntry.style, a.style)
                applyPercents(child, childEntry.style, container)
            }
        }
    }

    private fun updateAttributes(a: Change.UpdateAttributes) {
        val entry = entries[a.key] ?: return
        entry.node = a.node
        when (val n = a.node) {
            is NativeButton -> (entry.view as? Button)?.let { configureButton(it, n) }
            is NativeField -> (entry.view as? EditText)?.let { configureField(it, n) }
            is NativeCheckbox -> (entry.view as? CheckBox)?.let {
                it.isEnabled = !n.disabled
                it.alpha = if (n.disabled) 0.5f else 1f
            }
            is NativeImage -> (entry.view as? ImageView)?.let { configureImage(it, n) }
            is NativeItem -> ((entry.view as? ViewGroup)?.getChildAt(0) as? TextView)?.text = n.marker + " "
            else -> {}
        }
        bindEvents(entry)
    }

    private fun entryOfParent(parent: ViewGroup?): ViewEntry? =
        (parent?.tag as? String)?.let { entries[it] }

    private fun discard(v: View) {
        (v.tag as? String)?.let { entries.remove(it) }
        if (v is ViewGroup) for (i in 0 until v.childCount) discard(v.getChildAt(i))
    }

    private fun createView(node: Native, parent: NativeStyle?): ViewEntry {
        val entry: ViewEntry = when (node) {
            is NativeBox -> {
                val box = LinearLayout(context)
                ViewEntry(box, box, node.style, node)
            }
            is NativeList -> {
                val list = LinearLayout(context)
                ViewEntry(list, list, node.style, node)
            }
            is NativeItem -> {
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                val markerView = TextView(context).apply { text = node.marker + " " }
                val content = LinearLayout(context)
                row.addView(markerView, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ))
                row.addView(content, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                ViewEntry(row, content, node.style, node)
            }
            is NativeText -> {
                val tv = TextView(context)
                tv.text = buildText(node.spans)
                ViewEntry(tv, null, node.style, node)
            }
            is NativeButton -> {
                val b = Button(context)
                b.isAllCaps = false
                b.minHeight = 0
                b.minWidth = 0
                b.minimumHeight = 0
                b.minimumWidth = 0
                if (Build.VERSION.SDK_INT >= 21) b.stateListAnimator = null
                configureButton(b, node)
                ViewEntry(b, null, node.style, node)
            }
            is NativeField -> {
                val et = EditText(context)
                et.setText(node.value)
                configureField(et, node)
                ViewEntry(et, null, node.style, node)
            }
            is NativeCheckbox -> {
                val cb = CheckBox(context)
                cb.isChecked = node.checked
                cb.isEnabled = !node.disabled
                cb.alpha = if (node.disabled) 0.5f else 1f
                ViewEntry(cb, null, node.style, node)
            }
            is NativeImage -> {
                val iv = ImageView(context)
                iv.adjustViewBounds = true
                iv.scaleType = ImageView.ScaleType.FIT_CENTER
                configureImage(iv, node)
                ViewEntry(iv, null, node.style, node)
            }
        }

        entry.view.tag = node.key
        if (entry.container != null && entry.container !== entry.view) {
            entry.container.tag = node.key
        }
        entries[node.key] = entry

        applyStyle(entry)
        if (parent != null) entry.view.layoutParams = childParams(node.style, parent)
        bindEvents(entry)

        val container = entry.container
        if (container != null) {
            for (child in node.nativeChildren()) {
                val childEntry = createView(child, node.style)
                container.addView(childEntry.view)
                applyPercents(childEntry.view, childEntry.style, container)
            }
        }
        return entry
    }

    private fun bindEvents(entry: ViewEntry) {
        val node = entry.node
        val v = entry.view
        if (v is EditText) return
        val interactive = node.id != null || node.classes.isNotEmpty()
        if (interactive) {
            val key = node.key
            v.setOnClickListener { publish(key) }
        } else {
            v.setOnClickListener(null)
            v.isClickable = v is Button || v is CheckBox
        }
    }

    private fun configureButton(b: Button, node: NativeButton) {
        b.text = node.label
        b.isEnabled = !node.disabled
        b.alpha = if (node.disabled) 0.5f else 1f
    }

    private fun configureField(et: EditText, node: NativeField) {
        et.hint = node.hint
        val wantedInput = when (node.type) {
            FieldType.TEXT -> InputType.TYPE_CLASS_TEXT
            FieldType.PASSWORD -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            FieldType.NUMBER ->
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            FieldType.AREA -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        if (et.inputType != wantedInput) et.inputType = wantedInput
        if (node.type == FieldType.AREA) {
            et.minLines = maxOf(1, node.rows)
            et.gravity = Gravity.TOP or Gravity.START
        }
        et.isEnabled = !node.disabled
        et.alpha = if (node.disabled) 0.5f else 1f
        et.isFocusable = !node.readOnly
        et.isFocusableInTouchMode = !node.readOnly
        et.isCursorVisible = !node.readOnly
    }

    private fun configureImage(iv: ImageView, node: NativeImage) {
        iv.contentDescription = node.description
        val bitmap = loadImage(node.source)
        if (bitmap != null) {
            iv.setImageBitmap(bitmap)
            iv.setBackgroundColor(Color.TRANSPARENT)
        } else {
            iv.setImageDrawable(null)
            iv.setBackgroundColor(0xFFDDDDDD.toInt())
        }
    }

    private fun loadImage(source: String): Bitmap? {
        if (source.startsWith("data:")) {
            return try {
                val bytes = Base64.decode(source.substringAfter(','), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        }
        return try {
            imageResolver(source)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildText(spans: List<Span>): CharSequence {
        val sb = SpannableStringBuilder()
        for (t in spans) {
            val start = sb.length
            sb.append(t.text)
            val end = sb.length
            if (end > start) {
                sb.setSpan(ForegroundColorSpan(t.color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(AbsoluteSizeSpan(pxF(t.size).roundToInt(), false), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (t.bold) sb.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return sb
    }

    private fun applyStyle(entry: ViewEntry) {
        val v = entry.view
        val st = entry.style
        val n = entry.node

        applyBackground(v, st, ripple = v is Button)
        v.setPadding(px(st.padding.left), px(st.padding.top), px(st.padding.right), px(st.padding.bottom))

        if (v is TextView) {
            v.setTextColor(st.textColor)
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX, pxF(st.fontSize))
            v.setTypeface(null, if (st.bold) Typeface.BOLD else Typeface.NORMAL)
            when (n) {
                is NativeText -> v.gravity = textGravity(st)
                is NativeButton -> v.gravity = buttonGravity(st)
                else -> {}
            }
            if (v is EditText) {
                v.setHintTextColor((st.textColor and 0x00FFFFFF) or 0x80000000.toInt())
            }
        }

        if (v is CheckBox) {
            v.buttonTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(0xFF2F81F7.toInt(), Colors.BORDER_GRAY)
            )
        }

        if (n is NativeItem) {
            ((v as? ViewGroup)?.getChildAt(0) as? TextView)?.apply {
                setTextColor(st.textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, pxF(st.fontSize))
            }
        }

        val container = entry.container
        if (container is LinearLayout) applyContainer(container, st)
    }

    private fun applyBackground(view: View, e: NativeStyle, ripple: Boolean) {
        val hasBackground = e.backgroundColor != 0
        val hasBorder = e.borderWidth.value > 0f
        if (!hasBackground && !hasBorder && !ripple) {
            view.background = null
            return
        }
        val shape = GradientDrawable().apply {
            setColor(if (hasBackground) e.backgroundColor else Color.TRANSPARENT)
            cornerRadius = pxF(e.borderRadius)
            if (hasBorder) setStroke(maxOf(1, px(e.borderWidth)), e.borderColor)
        }
        view.background = if (ripple && Build.VERSION.SDK_INT >= 21) {
            RippleDrawable(ColorStateList.valueOf(0x33000000), shape, null)
        } else {
            shape
        }
    }

    private fun applyContainer(lin: LinearLayout, e: NativeStyle) {
        val vertical = e.direction == Direction.COLUMN
        lin.orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        lin.gravity = when (e.justify) {
            Justify.START -> if (vertical) Gravity.TOP else Gravity.START
            Justify.CENTER -> if (vertical) Gravity.CENTER_VERTICAL else Gravity.CENTER_HORIZONTAL
            Justify.END -> if (vertical) Gravity.BOTTOM else Gravity.END
        }
        val gap = px(e.gap)
        if (gap > 0) {
            lin.dividerDrawable = GradientDrawable().apply {
                setSize(gap, gap)
                setColor(Color.TRANSPARENT)
            }
            lin.showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        } else {
            lin.showDividers = LinearLayout.SHOW_DIVIDER_NONE
            lin.dividerDrawable = null
        }
    }

    private fun buttonGravity(e: NativeStyle): Int = when (e.textAlign) {
        TextAlign.CENTER -> Gravity.CENTER
        TextAlign.END -> Gravity.END or Gravity.CENTER_VERTICAL
        TextAlign.START -> Gravity.START or Gravity.CENTER_VERTICAL
    }

    private fun textGravity(e: NativeStyle): Int {
        fun horiz(a: Align) = when (a) {
            Align.CENTER -> Gravity.CENTER_HORIZONTAL
            Align.END -> Gravity.END
            else -> Gravity.START
        }
        fun vert(a: Align) = when (a) {
            Align.CENTER -> Gravity.CENTER_VERTICAL
            Align.END -> Gravity.BOTTOM
            else -> Gravity.TOP
        }
        fun justHoriz(j: Justify) = when (j) {
            Justify.CENTER -> Gravity.CENTER_HORIZONTAL
            Justify.END -> Gravity.END
            Justify.START -> Gravity.START
        }
        fun justVert(j: Justify) = when (j) {
            Justify.CENTER -> Gravity.CENTER_VERTICAL
            Justify.END -> Gravity.BOTTOM
            Justify.START -> Gravity.TOP
        }

        var h = when (e.textAlign) {
            TextAlign.CENTER -> Gravity.CENTER_HORIZONTAL
            TextAlign.END -> Gravity.END
            TextAlign.START -> Gravity.START
        }
        val v: Int
        if (e.direction == Direction.COLUMN) {
            if (e.alignItems != Align.STRETCH && e.textAlign == TextAlign.START) h = horiz(e.alignItems)
            v = justVert(e.justify)
        } else {
            if (e.justify != Justify.START) h = justHoriz(e.justify)
            v = vert(e.alignItems)
        }
        return h or v
    }

    private fun rootParams(e: NativeStyle): FrameLayout.LayoutParams {
        val width = when (val m = e.width) {
            is Size.Dim -> px(m.length)
            else -> ViewGroup.LayoutParams.MATCH_PARENT
        }
        val height = when {
            e.flex > 0f -> ViewGroup.LayoutParams.MATCH_PARENT
            e.height is Size.Dim -> px(e.height.length)
            e.height is Size.Percent && e.height.value >= 100f -> ViewGroup.LayoutParams.MATCH_PARENT
            else -> ViewGroup.LayoutParams.WRAP_CONTENT
        }
        return FrameLayout.LayoutParams(width, height).apply {
            setMargins(px(e.margin.left), px(e.margin.top), px(e.margin.right), px(e.margin.bottom))
        }
    }

    private fun childParams(e: NativeStyle, parent: NativeStyle): LinearLayout.LayoutParams {
        val parentVertical = parent.direction == Direction.COLUMN

        fun mainSize(m: Size): Int = when (m) {
            is Size.Dim -> px(m.length)
            is Size.Percent ->
                if (m.value >= 100f) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
            Size.Auto -> ViewGroup.LayoutParams.WRAP_CONTENT
        }
        fun crossSize(m: Size): Int = when (m) {
            is Size.Dim -> px(m.length)
            is Size.Percent ->
                if (m.value >= 100f) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
            Size.Auto ->
                if (parent.alignItems == Align.STRETCH) ViewGroup.LayoutParams.MATCH_PARENT
                else ViewGroup.LayoutParams.WRAP_CONTENT
        }

        var width: Int
        var height: Int
        var weight = 0f
        if (parentVertical) {
            width = crossSize(e.width)
            height = mainSize(e.height)
            if (e.flex > 0f) { height = 0; weight = e.flex }
        } else {
            height = crossSize(e.height)
            width = mainSize(e.width)
            if (e.flex > 0f) { width = 0; weight = e.flex }
        }

        val lp = LinearLayout.LayoutParams(width, height, weight)
        lp.setMargins(px(e.margin.left), px(e.margin.top), px(e.margin.right), px(e.margin.bottom))
        lp.gravity = when (parent.alignItems) {
            Align.START -> if (parentVertical) Gravity.START else Gravity.TOP
            Align.CENTER -> if (parentVertical) Gravity.CENTER_HORIZONTAL else Gravity.CENTER_VERTICAL
            Align.END -> if (parentVertical) Gravity.END else Gravity.BOTTOM
            Align.STRETCH -> -1
        }
        return lp
    }

    private fun applyPercents(view: View, e: NativeStyle, parent: ViewGroup) {
        val l = (e.width as? Size.Percent)?.takeIf { it.value < 100f }
        val a = (e.height as? Size.Percent)?.takeIf { it.value < 100f }
        if (l == null && a == null) return

        fun apply(): Boolean {
            val lp = view.layoutParams ?: return true
            var pronto = true
            var mudou = false
            if (l != null) {
                val disponivel = parent.width - parent.paddingLeft - parent.paddingRight
                if (disponivel > 0) {
                    val alvo = (disponivel * l.value / 100f).roundToInt()
                    if (lp.width != alvo) { lp.width = alvo; mudou = true }
                } else {
                    pronto = false
                }
            }
            if (a != null) {
                val disponivel = parent.height - parent.paddingTop - parent.paddingBottom
                if (disponivel > 0) {
                    val alvo = (disponivel * a.value / 100f).roundToInt()
                    if (lp.height != alvo) { lp.height = alvo; mudou = true }
                } else {
                    pronto = false
                }
            }
            if (mudou) view.layoutParams = lp
            return pronto
        }

        parent.post {
            if (!apply()) {
                parent.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        v: View?, left: Int, top: Int, right: Int, bottom: Int,
                        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                    ) {
                        if (apply()) parent.removeOnLayoutChangeListener(this)
                    }
                })
            }
        }
    }
}
