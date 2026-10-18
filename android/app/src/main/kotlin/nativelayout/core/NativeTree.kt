/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-15
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout.core

data class Length(val value: Float, val scalable: Boolean = false) {
    fun toPixels(density: Float, fontScale: Float): Float =
        if (scalable) value * 16f * density * fontScale else value * density

    companion object {
        val ZERO = Length(0f)
        fun rem(value: Float) = Length(value, true)
        fun px(value: Float) = Length(value, false)
    }
}

sealed class Size {
    data class Dim(val length: Length) : Size()
    data class Percent(val value: Float) : Size()
    object Auto : Size()
}

enum class Direction { COLUMN, ROW }
enum class Justify { START, CENTER, END }
enum class Align { START, CENTER, END, STRETCH }
enum class TextAlign { START, CENTER, END }
enum class FieldType { TEXT, PASSWORD, NUMBER, AREA }

data class Sides(
    val top: Length = Length.ZERO,
    val right: Length = Length.ZERO,
    val bottom: Length = Length.ZERO,
    val left: Length = Length.ZERO
)

object Colors {
    val BLACK: Int = 0xFF000000.toInt()
    val WHITE: Int = 0xFFFFFFFF.toInt()
    val BUTTON_GRAY: Int = 0xFFE0E0E0.toInt()
    val BORDER_GRAY: Int = 0xFF767676.toInt()
}

data class NativeStyle(
    val textColor: Int = Colors.BLACK,
    val backgroundColor: Int = 0,
    val fontSize: Length = Length.rem(1f),
    val bold: Boolean = false,
    val textAlign: TextAlign = TextAlign.START,
    val width: Size = Size.Auto,
    val height: Size = Size.Auto,
    val padding: Sides = Sides(),
    val margin: Sides = Sides(),
    val borderRadius: Length = Length.ZERO,
    val borderWidth: Length = Length.ZERO,
    val borderColor: Int = Colors.BLACK,
    val direction: Direction = Direction.COLUMN,
    val justify: Justify = Justify.START,
    val alignItems: Align = Align.STRETCH,
    val gap: Length = Length.ZERO,
    val flex: Float = 0f
)

data class Span(val text: String, val bold: Boolean, val color: Int, val size: Length)

sealed class Native(
    val id: String?,
    val classes: List<String>,
    val style: NativeStyle,
    val line: Int
) {
    var key: String = ""
        internal set

    /** Value of the optional key="..." attribute: a stable identity among siblings. */
    var keyHint: String? = null
        internal set
}

class NativeBox(
    id: String?, classes: List<String>, style: NativeStyle, line: Int,
    val children: List<Native>, val virtual: Boolean = false
) : Native(id, classes, style, line)

class NativeText(
    id: String?, classes: List<String>, style: NativeStyle, line: Int,
    val spans: List<Span>
) : Native(id, classes, style, line) {
    val plainText: String get() = spans.joinToString("") { it.text }
}

class NativeButton(
    id: String?, classes: List<String>, style: NativeStyle, line: Int,
    val label: String, val disabled: Boolean
) : Native(id, classes, style, line)

class NativeField(
    id: String?, classes: List<String>, style: NativeStyle, line: Int,
    val type: FieldType, val hint: String, val value: String,
    val disabled: Boolean, val readOnly: Boolean, val rows: Int
) : Native(id, classes, style, line)

class NativeCheckbox(
    id: String?, classes: List<String>, style: NativeStyle, line: Int,
    val checked: Boolean, val disabled: Boolean
) : Native(id, classes, style, line)

class NativeImage(
    id: String?, classes: List<String>, style: NativeStyle, line: Int,
    val source: String, val description: String
) : Native(id, classes, style, line)

class NativeList(
    id: String?, classes: List<String>, style: NativeStyle, line: Int,
    val items: List<NativeItem>
) : Native(id, classes, style, line)

class NativeItem(
    id: String?, classes: List<String>, style: NativeStyle, line: Int,
    val marker: String, val children: List<Native>
) : Native(id, classes, style, line)

fun Native.nativeChildren(): List<Native> = when (this) {
    is NativeBox -> children
    is NativeList -> items
    is NativeItem -> children
    else -> emptyList()
}

fun Native.describe(level: Int = 0): String {
    val idText = id?.let { " #$it" } ?: ""
    val clsText = if (classes.isEmpty()) "" else classes.joinToString(" ", prefix = " .")
    val detail = when (this) {
        is NativeText -> " \"$plainText\""
        is NativeButton -> " \"$label\""
        is NativeField -> " $type"
        is NativeImage -> " $source"
        else -> ""
    }
    val name = this::class.simpleName ?: "Native"
    return "  ".repeat(level) + name + idText + clsText + detail + "\n" +
        nativeChildren().joinToString("") { it.describe(level + 1) }
}

fun Native.sameAs(o: Native): Boolean {
    if (this === o) return true
    if (this::class != o::class || key != o.key || id != o.id || classes != o.classes || style != o.style) return false
    val same = when {
        this is NativeText && o is NativeText -> spans == o.spans
        this is NativeButton && o is NativeButton -> label == o.label && disabled == o.disabled
        this is NativeField && o is NativeField ->
            type == o.type && hint == o.hint && value == o.value &&
                disabled == o.disabled && readOnly == o.readOnly && rows == o.rows
        this is NativeCheckbox && o is NativeCheckbox -> checked == o.checked && disabled == o.disabled
        this is NativeImage && o is NativeImage -> source == o.source && description == o.description
        this is NativeItem && o is NativeItem -> marker == o.marker
        this is NativeBox && o is NativeBox -> virtual == o.virtual
        else -> true
    }
    if (!same) return false
    val a = nativeChildren()
    val b = o.nativeChildren()
    if (a.size != b.size) return false
    for (i in a.indices) if (!a[i].sameAs(b[i])) return false
    return true
}
