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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The <header>: a bar fixed at the top of the screen, outside the scroll. */
class HeaderTest {
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
    fun headerComesFirstEvenWhenWrittenLater() {
        val r = LayoutEngine.build(
            """<div><p>Hi</p><header id="h"><p>Title</p></header><p>Bye</p></div>"""
        )
        val root = r.root as NativeBox
        assertEquals(3, root.children.size)
        val first = root.children.first() as NativeBox
        assertTrue(first.header)
        assertEquals("h", first.id)
        assertFalse((root.children[1] as? NativeBox)?.header ?: false)
    }

    @Test
    fun headerWithOnlyTextStaysABox() {
        val h = LayoutEngine.build("""<div><header id="h">Title</header><p>Body</p></div>""").byId["h"]!!
        assertTrue(h is NativeBox && h.header)
    }

    @Test
    fun headerAndDialogTogether() {
        val root = LayoutEngine.build(
            """<div><dialog id="d"></dialog><p>Body</p><header id="h"></header></div>"""
        ).root as NativeBox
        assertTrue((root.children.first() as NativeBox).header)
        assertTrue((root.children.last() as NativeBox).overlay)
    }

    @Test
    fun headerMustBeAtTheFirstLevel() {
        assertTrue(errorOf("<div><div><header></header></div></div>").contains("direct child of the root"))
        assertTrue(errorOf("<header></header>").contains("direct child of the root"))
    }

    @Test
    fun onlyOneHeaderIsAllowed() {
        assertTrue(errorOf("<div><header></header><header></header></div>").contains("only one"))
    }

    @Test
    fun headerKeyDoesNotDependOnTheNumberOfSiblings() {
        val a = LayoutEngine.build("<div><header><p>T</p></header><p>A</p></div>").root as NativeBox
        val b = LayoutEngine.build("<div><header><p>T</p></header><p>A</p><p>B</p></div>").root as NativeBox
        assertEquals(a.children[0].key, b.children[0].key)
    }

    @Test
    fun changingAHeaderIntoABoxIsAReplace() {
        val before = LayoutEngine.build("""<div><header id="h"></header><p>A</p></div>""").root
        val after = LayoutEngine.build("""<div><div id="h"></div><p>A</p></div>""").root
        assertTrue(LayoutDiffer.diff(before, after).any { it is Change.Replace })
    }

    @Test
    fun anUnchangedHeaderProducesNoChanges() {
        val a = LayoutEngine.build("""<div><header><p>T</p></header><p>A</p></div>""").root
        val b = LayoutEngine.build("""<div><header><p>T</p></header><p>A</p></div>""").root
        assertTrue(LayoutDiffer.diff(a, b).isEmpty())
    }
}
