/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-20
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout.core

/**
 * One stroke of a built-in icon. Every icon is drawn on a 24 x 24 grid with a 2-unit stroke and round
 * caps and joins, in the text color of the element. The shapes are plain data, so every host (Android,
 * Swing, Qt...) draws the same icons.
 */
sealed class IconShape {
    /** A polyline; [points] is x1, y1, x2, y2... When [closed] the last point joins the first. */
    class Line(val points: FloatArray, val closed: Boolean = false) : IconShape()

    class Circle(val cx: Float, val cy: Float, val r: Float) : IconShape()

    /** A small filled dot, for "more" icons. */
    class Dot(val cx: Float, val cy: Float) : IconShape()

    class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float, val radius: Float) : IconShape()
}

/** The built-in icons, used as `<img src="icon:name">`. */
object IconSet {
    const val PREFIX = "icon:"
    const val GRID = 24f
    const val STROKE = 2f
    const val DOT_RADIUS = 1.6f

    /** The icon name of an `<img src>`, or null when the source is not an `icon:` one. */
    fun nameOf(source: String): String? =
        if (source.startsWith(PREFIX)) source.substring(PREFIX.length).trim() else null

    fun shapesOf(name: String): List<IconShape>? = shapes[name]

    val names: List<String> get() = shapes.keys.toList()

    private fun line(vararg p: Float) = IconShape.Line(p)

    private fun polygon(vararg p: Float) = IconShape.Line(p, closed = true)

    private val shapes: Map<String, List<IconShape>> = linkedMapOf(
        "arrow-left" to listOf(
            line(19f, 12f, 5f, 12f),
            line(12f, 19f, 5f, 12f, 12f, 5f)
        ),
        "arrow-right" to listOf(
            line(5f, 12f, 19f, 12f),
            line(12f, 5f, 19f, 12f, 12f, 19f)
        ),
        "arrow-up" to listOf(
            line(12f, 19f, 12f, 5f),
            line(5f, 12f, 12f, 5f, 19f, 12f)
        ),
        "arrow-down" to listOf(
            line(12f, 5f, 12f, 19f),
            line(5f, 12f, 12f, 19f, 19f, 12f)
        ),
        "chevron-left" to listOf(
            line(15f, 5f, 8f, 12f, 15f, 19f)
        ),
        "chevron-right" to listOf(
            line(9f, 5f, 16f, 12f, 9f, 19f)
        ),
        "chevron-up" to listOf(
            line(5f, 15f, 12f, 8f, 19f, 15f)
        ),
        "chevron-down" to listOf(
            line(5f, 9f, 12f, 16f, 19f, 9f)
        ),
        "close" to listOf(
            line(6f, 6f, 18f, 18f),
            line(18f, 6f, 6f, 18f)
        ),
        "check" to listOf(
            line(4f, 12.5f, 9.5f, 18f, 20f, 6.5f)
        ),
        "plus" to listOf(
            line(12f, 5f, 12f, 19f),
            line(5f, 12f, 19f, 12f)
        ),
        "minus" to listOf(
            line(5f, 12f, 19f, 12f)
        ),
        "menu" to listOf(
            line(4f, 6f, 20f, 6f),
            line(4f, 12f, 20f, 12f),
            line(4f, 18f, 20f, 18f)
        ),
        "search" to listOf(
            IconShape.Circle(10.5f, 10.5f, 6.5f),
            line(15.3f, 15.3f, 20f, 20f)
        ),
        "more-vertical" to listOf(
            IconShape.Dot(12f, 5f),
            IconShape.Dot(12f, 12f),
            IconShape.Dot(12f, 19f)
        ),
        "more-horizontal" to listOf(
            IconShape.Dot(5f, 12f),
            IconShape.Dot(12f, 12f),
            IconShape.Dot(19f, 12f)
        ),
        "home" to listOf(
            line(3f, 11f, 12f, 3f, 21f, 11f),
            line(5f, 9.5f, 5f, 21f, 19f, 21f, 19f, 9.5f),
            line(10f, 21f, 10f, 15f, 14f, 15f, 14f, 21f)
        ),
        "user" to listOf(
            IconShape.Circle(12f, 8f, 4f),
            line(5f, 21f, 5f, 19.5f, 5.8f, 17f, 7.6f, 15.3f, 10f, 14.3f, 12f, 14f, 14f, 14.3f, 16.4f, 15.3f, 18.2f, 17f, 19f, 19.5f, 19f, 21f)
        ),
        "trash" to listOf(
            line(4f, 7f, 20f, 7f),
            line(9f, 7f, 9f, 4f, 15f, 4f, 15f, 7f),
            line(6f, 7f, 7f, 21f, 17f, 21f, 18f, 7f),
            line(10f, 11f, 10f, 17f),
            line(14f, 11f, 14f, 17f)
        ),
        "edit" to listOf(
            polygon(4f, 20f, 5f, 15f, 16f, 4f, 20f, 8f, 9f, 19f),
            line(14f, 6f, 18f, 10f)
        ),
        "star" to listOf(
            polygon(12f, 3f, 14.4f, 9.3f, 21.1f, 9.6f, 15.9f, 13.9f, 17.6f, 20.4f, 12f, 16.7f, 6.4f, 20.4f, 8.1f, 13.9f, 2.9f, 9.6f, 9.6f, 9.3f)
        ),
        "lock" to listOf(
            IconShape.Rect(5f, 11f, 19f, 21f, 2f),
            line(8f, 11f, 8f, 8.2f, 8.6f, 6.4f, 10f, 5.1f, 12f, 4.6f, 14f, 5.1f, 15.4f, 6.4f, 16f, 8.2f, 16f, 11f)
        ),
        "eye" to listOf(
            polygon(2f, 12f, 4f, 9.6f, 7f, 7.6f, 9.5f, 6.6f, 12f, 6.3f, 14.5f, 6.6f, 17f, 7.6f, 20f, 9.6f, 22f, 12f, 20f, 14.4f, 17f, 16.4f, 14.5f, 17.4f, 12f, 17.7f, 9.5f, 17.4f, 7f, 16.4f, 4f, 14.4f),
            IconShape.Circle(12f, 12f, 3f)
        ),
        "mail" to listOf(
            IconShape.Rect(3f, 5f, 21f, 19f, 2f),
            line(3f, 7f, 12f, 13f, 21f, 7f)
        ),
        "info" to listOf(
            IconShape.Circle(12f, 12f, 9f),
            line(12f, 11f, 12f, 17f),
            IconShape.Dot(12f, 7.5f)
        ),
        "alert" to listOf(
            polygon(12f, 3f, 22f, 20f, 2f, 20f),
            line(12f, 9f, 12f, 14f),
            IconShape.Dot(12f, 17f)
        ),
        "calendar" to listOf(
            IconShape.Rect(3f, 5f, 21f, 21f, 2f),
            line(3f, 10f, 21f, 10f),
            line(8f, 3f, 8f, 7f),
            line(16f, 3f, 16f, 7f)
        ),
        "logout" to listOf(
            line(9f, 4f, 5f, 4f, 5f, 20f, 9f, 20f),
            line(10f, 12f, 20f, 12f),
            line(16f, 8f, 20f, 12f, 16f, 16f)
        ),
        "bookmark" to listOf(
            polygon(6f, 3f, 18f, 3f, 18f, 21f, 12f, 16f, 6f, 21f)
        ),
        "clock" to listOf(
            IconShape.Circle(12f, 12f, 9f),
            line(12f, 7f, 12f, 12f, 16f, 14f)
        )
    )
}
