/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-21
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** `position: absolute`, `background-image` and `object-fit` (section 4). */
class AbsoluteTest {
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
    fun absoluteChildMakesTheParentAFrame() {
        val r = LayoutEngine.build(
            """<div><div id="box"><p>Flow</p><p id="badge" style="position: absolute; top: 4px; right: 8px">1</p></div></div>"""
        )
        val box = r.byId["box"] as NativeBox
        assertTrue(box.hasAbsolute)
        val badge = r.byId["badge"]!!
        assertEquals(Position.ABSOLUTE, badge.style.position)
        assertEquals(4f, badge.style.top!!.value, 0.001f)
        assertEquals(8f, badge.style.right!!.value, 0.001f)
        assertNull(badge.style.left)
    }

    @Test
    fun aBoxWithoutAbsoluteChildrenIsNotAFrame() {
        val box = LayoutEngine.build("""<div><div id="box"><p>Flow</p><p>Two</p></div></div>""").byId["box"] as NativeBox
        assertFalse(box.hasAbsolute)
    }

    @Test
    fun aSingleAbsoluteTextIsNotCollapsedIntoItsParent() {
        val box = LayoutEngine.build(
            """<div><div id="box"><p style="position: absolute; top: 0">X</p></div></div>"""
        ).byId["box"]
        assertTrue(box is NativeBox && box.hasAbsolute)
    }

    @Test
    fun relativeIsAcceptedAndDoesNothing() {
        val s = LayoutEngine.build("""<div><p id="p" style="position: relative">x</p></div>""").byId["p"]!!.style
        assertEquals(Position.STATIC, s.position)
    }

    @Test
    fun absoluteIsIgnoredWithAWarningWhereItMakesNoSense() {
        val r = LayoutEngine.build("""<div><header id="h" style="position: absolute"></header></div>""")
        assertEquals(Position.STATIC, r.byId["h"]!!.style.position)
        assertTrue(r.warnings.any { it.message.contains("position: absolute") })
    }

    @Test
    fun absoluteInsideAVirtualContainerIsAnError() {
        assertTrue(errorOf("""<div><div virtual><p style="position: absolute">x</p></div></div>""").contains("virtual"))
    }

    @Test
    fun zIndexIsRead() {
        val s = LayoutEngine.build("""<div><div><p id="p" style="position: absolute; z-index: 3">x</p></div></div>""")
            .byId["p"]!!.style
        assertEquals(3, s.zIndex)
    }

    @Test
    fun backgroundImageNameAndOptions() {
        val s = LayoutEngine.build(
            """<div><div id="d" style="background-image: url(cover.jpg); background-size: contain; background-position: top"></div></div>"""
        ).byId["d"]!!.style
        assertEquals("cover.jpg", s.backgroundImage)
        assertEquals(ImageFit.CONTAIN, s.backgroundSize)
        assertEquals(ImagePos.TOP, s.backgroundPosition)
    }

    @Test
    fun backgroundShorthandWithAColorClearsTheImage() {
        val s = LayoutEngine.build(
            """<div><div id="d" style="background-image: url(a.jpg); background: #fff"></div></div>"""
        ).byId["d"]!!.style
        assertNull(s.backgroundImage)
    }

    @Test
    fun objectFit() {
        val s = LayoutEngine.build("""<div><img id="i" src="x" style="object-fit: cover"></div>""").byId["i"]!!.style
        assertEquals(ImageFit.COVER, s.objectFit)
    }

    @Test
    fun anAbsoluteChildIsCreatedByItsPositionAmongTheAbsoluteOnes() {
        val a = LayoutEngine.build("""<div><div id="b"><p>one</p><p>two</p></div></div>""").root
        val b = LayoutEngine.build(
            """<div><div id="b"><p>one</p><p>two</p><p id="x" style="position: absolute; top: 0">x</p></div></div>"""
        ).root
        // the box becomes a frame, so it is replaced as a whole
        assertTrue(LayoutDiffer.diff(a, b).any { it is Change.Replace })
    }
}