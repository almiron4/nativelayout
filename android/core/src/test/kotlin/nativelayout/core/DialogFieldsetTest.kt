/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-19
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package nativelayout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** border-radius with four values, the <dialog> layer and <fieldset> with <legend>. */
class DialogFieldsetTest {
    private fun styleOf(css: String): NativeStyle =
        LayoutEngine.build("""<div id="a" style="$css"></div>""").byId["a"]!!.style

    private fun errorOf(html: String): String {
        try {
            LayoutEngine.build(html)
        } catch (e: NativeLayoutError) {
            return e.message
        }
        fail("expected an error")
        return ""
    }

    // ---------- border-radius ----------

    @Test
    fun oneValueKeepsASingleRadius() {
        val s = styleOf("border-radius: 8px")
        assertNull(s.corners)
        assertEquals(8f, s.borderRadius.value, 0.001f)
    }

    @Test
    fun fourValuesGoToTheCorners() {
        val c = styleOf("border-radius: 16px 12px 8px 4px").corners!!
        assertEquals(16f, c.topLeft.value, 0.001f)
        assertEquals(12f, c.topRight.value, 0.001f)
        assertEquals(8f, c.bottomRight.value, 0.001f)
        assertEquals(4f, c.bottomLeft.value, 0.001f)
    }

    @Test
    fun sheetShapeRoundsOnlyTheTop() {
        val c = styleOf("border-radius: 16px 16px 0 0").corners!!
        assertEquals(16f, c.topLeft.value, 0.001f)
        assertEquals(16f, c.topRight.value, 0.001f)
        assertEquals(0f, c.bottomRight.value, 0.001f)
        assertEquals(0f, c.bottomLeft.value, 0.001f)
    }

    @Test
    fun twoAndThreeValuesFollowTheCssOrder() {
        val two = styleOf("border-radius: 10px 2px").corners!!
        assertEquals(10f, two.topLeft.value, 0.001f)
        assertEquals(2f, two.topRight.value, 0.001f)
        assertEquals(10f, two.bottomRight.value, 0.001f)
        assertEquals(2f, two.bottomLeft.value, 0.001f)

        val three = styleOf("border-radius: 10px 2px 6px").corners!!
        assertEquals(10f, three.topLeft.value, 0.001f)
        assertEquals(2f, three.topRight.value, 0.001f)
        assertEquals(6f, three.bottomRight.value, 0.001f)
        assertEquals(2f, three.bottomLeft.value, 0.001f)
    }

    @Test
    fun equalValuesBecomeASingleRadius() {
        val s = styleOf("border-radius: 5px 5px 5px 5px")
        assertNull(s.corners)
        assertEquals(5f, s.borderRadius.value, 0.001f)
    }

    @Test
    fun longhandChangesOneCorner() {
        val s = styleOf("border-radius: 8px; border-top-left-radius: 0")
        val c = s.corners!!
        assertEquals(0f, c.topLeft.value, 0.001f)
        assertEquals(8f, c.topRight.value, 0.001f)
        assertEquals(8f, c.bottomRight.value, 0.001f)
        assertEquals(8f, c.bottomLeft.value, 0.001f)
    }

    @Test
    fun slashFormAndTooManyValuesAreIgnoredWithAWarning() {
        val slash = LayoutEngine.build("""<div id="a" style="border-radius: 10px / 20px"></div>""")
        assertNull(slash.byId["a"]!!.style.corners)
        assertTrue(slash.warnings.isNotEmpty())
        val many = LayoutEngine.build("""<div id="a" style="border-radius: 1px 2px 3px 4px 5px"></div>""")
        assertNull(many.byId["a"]!!.style.corners)
        assertTrue(many.warnings.isNotEmpty())
    }

    // ---------- dialog ----------

    @Test
    fun dialogIsALayerAndComesLast() {
        val r = LayoutEngine.build(
            """<div><p>Hi</p><dialog id="d" class="layer"><div id="sheet"></div></dialog><p>Bye</p></div>"""
        )
        val root = r.root as NativeBox
        assertEquals(3, root.children.size)
        val last = root.children.last() as NativeBox
        assertTrue(last.overlay)
        assertEquals("d", last.id)
        assertFalse((root.children[0] as? NativeBox)?.overlay ?: false)
        assertNotNull(r.byId["sheet"])
    }

    @Test
    fun dialogDefaultsAreADimmedLayerWithCenteredContent() {
        val d = LayoutEngine.build("""<div><dialog id="d"></dialog></div>""").byId["d"]!!
        assertEquals(0x80000000.toInt(), d.style.backgroundColor)
        assertEquals(Justify.CENTER, d.style.justify)
        assertEquals(Align.CENTER, d.style.alignItems)
    }

    @Test
    fun dialogCanBeStyledAsABottomSheet() {
        val d = LayoutEngine.build(
            """<div><dialog id="d" style="justify-content: flex-end; align-items: stretch"></dialog></div>"""
        ).byId["d"]!!
        assertEquals(Justify.END, d.style.justify)
        assertEquals(Align.STRETCH, d.style.alignItems)
    }

    @Test
    fun dialogWithOnlyTextStaysABox() {
        val d = LayoutEngine.build("""<div><dialog id="d">Hello</dialog></div>""").byId["d"]!!
        assertTrue(d is NativeBox && d.overlay)
    }

    @Test
    fun dialogMustBeAtTheFirstLevel() {
        assertTrue(errorOf("<div><div><dialog></dialog></div></div>").contains("direct child of the root"))
        assertTrue(errorOf("<dialog></dialog>").contains("direct child of the root"))
    }

    @Test
    fun dialogIsNotAllowedInAVirtualContainer() {
        val msg = errorOf("""<div virtual><dialog key="a"></dialog></div>""")
        assertTrue(msg.contains("virtual"))
    }

    @Test
    fun changingADialogIntoABoxIsAReplace() {
        val before = LayoutEngine.build("""<div><dialog id="d"></dialog></div>""").root
        val after = LayoutEngine.build("""<div><div id="d"></div></div>""").root
        val changes = LayoutDiffer.diff(before, after)
        assertTrue(changes.any { it is Change.Replace })
    }

    // ---------- fieldset and legend ----------

    @Test
    fun fieldsetIsABorderedGroup() {
        val f = LayoutEngine.build("""<div><fieldset id="f"><legend id="l">Data</legend></fieldset></div>""")
        val box = f.byId["f"] as NativeBox
        assertEquals(1f, box.style.borderWidth.value, 0.001f)
        assertEquals(1, box.children.size)
        val legend = f.byId["l"] as NativeText
        assertTrue(legend.style.bold)
        assertEquals("Data", legend.plainText)
    }

    @Test
    fun fieldsetWithOnlyTextStaysABox() {
        val f = LayoutEngine.build("""<div><fieldset id="f">Text</fieldset></div>""").byId["f"]!!
        assertTrue(f is NativeBox)
    }

    @Test
    fun disabledFieldsetBlocksEverythingInside() {
        val r = LayoutEngine.build(
            """<div><fieldset disabled><legend>Data</legend>
               <input id="i" type="text"><input id="c" type="checkbox"><button id="b">Ok</button>
               <textarea id="t"></textarea></fieldset><button id="out">Out</button></div>"""
        )
        assertTrue((r.byId["i"] as NativeField).disabled)
        assertTrue((r.byId["c"] as NativeCheckbox).disabled)
        assertTrue((r.byId["b"] as NativeButton).disabled)
        assertTrue((r.byId["t"] as NativeField).disabled)
        assertFalse((r.byId["out"] as NativeButton).disabled)
    }

    @Test
    fun enabledFieldsetBlocksNothing() {
        val r = LayoutEngine.build("""<div><fieldset><button id="b">Ok</button></fieldset></div>""")
        assertFalse((r.byId["b"] as NativeButton).disabled)
    }

    @Test
    fun legendOutsideAFieldsetIsAnError() {
        assertTrue(errorOf("<div><legend>Data</legend></div>").contains("<legend> can only be inside <fieldset>"))
        assertTrue(errorOf("<div><fieldset><div><legend>x</legend></div></fieldset></div>").contains("<legend>"))
    }
}
