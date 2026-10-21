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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The <footer>: a region fixed outside the scroll, placed by `dock` (bottom by default). */
class FooterTest {
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
    fun footerComesAfterTheBodyAndBeforeTheDialogs() {
        val root = LayoutEngine.build(
            """<div><dialog id="d"></dialog><footer id="f"></footer><p>Body</p><header id="h"></header></div>"""
        ).root as NativeBox
        assertTrue((root.children[0] as NativeBox).header)
        assertTrue(root.children[1] is NativeText)
        assertTrue((root.children[2] as NativeBox).footer)
        assertTrue((root.children[3] as NativeBox).overlay)
    }

    @Test
    fun footerWithOnlyTextStaysABox() {
        val f = LayoutEngine.build("""<div><footer id="f">Menu</footer><p>Body</p></div>""").byId["f"]!!
        assertTrue(f is NativeBox && f.footer)
    }

    @Test
    fun defaultsAreARowCentered() {
        val f = LayoutEngine.build("""<div><footer id="f"></footer></div>""").byId["f"]!!
        assertEquals(Direction.ROW, f.style.direction)
        assertEquals(Justify.CENTER, f.style.justify)
        assertEquals(Align.CENTER, f.style.alignItems)
        assertNull(f.style.dock)
    }

    @Test
    fun dockValues() {
        val cases = mapOf(
            "bottom" to Dock.BOTTOM, "top" to Dock.TOP, "left" to Dock.LEFT,
            "right" to Dock.RIGHT, "center" to Dock.CENTER
        )
        for ((word, dock) in cases) {
            val f = LayoutEngine.build("""<div><footer id="f" style="dock: $word"></footer></div>""").byId["f"]!!
            assertEquals(word, dock, f.style.dock)
        }
    }

    @Test
    fun footerMustBeAtTheFirstLevel() {
        assertTrue(errorOf("<div><div><footer></footer></div></div>").contains("direct child of the root"))
    }

    @Test
    fun onlyOneFooterIsAllowed() {
        assertTrue(errorOf("<div><footer></footer><footer></footer></div>").contains("only one"))
    }

    @Test
    fun changingDockIsAStyleChange() {
        val a = LayoutEngine.build("""<div><footer id="f"></footer></div>""").root
        val b = LayoutEngine.build("""<div><footer id="f" style="dock: top"></footer></div>""").root
        val changes = LayoutDiffer.diff(a, b)
        assertTrue(changes.any { it is Change.UpdateStyle })
    }
}
