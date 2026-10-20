/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-20
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The built-in icons: `<img src="icon:name">`. */
class IconTest {
    private fun errorOf(html: String): String {
        try {
            LayoutEngine.build(html)
        } catch (e: NativeLayoutError) {
            return e.message
        }
        fail("expected an error")
        return ""
    }

    @Test
    fun nameOfReadsOnlyTheIconScheme() {
        assertEquals("close", IconSet.nameOf("icon:close"))
        assertEquals("close", IconSet.nameOf("icon: close "))
        assertNull(IconSet.nameOf("photo.png"))
        assertNull(IconSet.nameOf("data:image/png;base64,AAAA"))
    }

    @Test
    fun anIconIsAnImage() {
        val r = LayoutEngine.build("""<div><img id="i" src="icon:arrow-left" alt="Back"></div>""")
        val img = r.byId["i"] as NativeImage
        assertEquals("icon:arrow-left", img.source)
        assertEquals("Back", img.description)
    }

    @Test
    fun anUnknownIconIsAClearError() {
        val msg = errorOf("""<div><img src="icon:nope"></div>""")
        assertTrue(msg.contains("does not exist"))
    }

    @Test
    fun everyIconHasShapesInsideTheGrid() {
        assertTrue(IconSet.names.size >= 25)
        for (name in IconSet.names) {
            val shapes = IconSet.shapesOf(name)
            assertNotNull(name, shapes)
            assertTrue("$name has no shapes", shapes!!.isNotEmpty())
            for (s in shapes) {
                val values: List<Float> = when (s) {
                    is IconShape.Line -> s.points.toList()
                    is IconShape.Circle -> listOf(s.cx - s.r, s.cy - s.r, s.cx + s.r, s.cy + s.r)
                    is IconShape.Dot -> listOf(s.cx, s.cy)
                    is IconShape.Rect -> listOf(s.left, s.top, s.right, s.bottom)
                }
                for (v in values) assertTrue("$name: $v is outside the grid", v >= 0f && v <= IconSet.GRID)
                if (s is IconShape.Line) assertTrue("$name: odd number of coordinates", s.points.size % 2 == 0)
            }
        }
    }

    @Test
    fun iconColorFollowsTheTextColor() {
        val r = LayoutEngine.build("""<div><img id="i" src="icon:check" style="color: #ff0000"></div>""")
        assertEquals(0xFFFF0000.toInt(), r.byId["i"]!!.style.textColor)
    }
}
