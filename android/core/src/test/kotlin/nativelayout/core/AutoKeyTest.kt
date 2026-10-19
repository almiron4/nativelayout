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
import org.junit.Assert.assertTrue
import org.junit.Test

/** The automatic key of an item in a `for` (section 5.7). */
class AutoKeyTest {
    private fun screen(body: String) = """
<nativelayout>

<body>
$body
</body>

</nativelayout>
"""

    private val list = screen(
        """
    for p in products:
        <div class="card"><p>{p.name}</p><p>x</p></div>
    end
"""
    )

    private fun products(vararg items: Map<String, Any?>) = mapOf<String, Any?>("products" to items.toList())

    private fun keysOf(data: Map<String, Any?>, src: String = list): List<String> {
        val html = Expander.expand(src, data).html
        val root = LayoutEngine.build(html).root as NativeBox
        return root.children.map { it.key }
    }

    @Test
    fun fieldIdBecomesTheKey() {
        val keys = keysOf(products(mapOf("id" to "a", "name" to "A"), mapOf("id" to "b", "name" to "B")))
        assertEquals(listOf("/~a", "/~b"), keys)
    }

    @Test
    fun fieldKeyWinsOverId() {
        val keys = keysOf(products(mapOf("key" to "k1", "id" to "i1", "name" to "A")))
        assertEquals(listOf("/~k1"), keys)
    }

    @Test
    fun wholeNumbersCount() {
        val keys = keysOf(products(mapOf("id" to 7, "name" to "A"), mapOf("id" to 8L, "name" to "B")))
        assertEquals(listOf("/~7", "/~8"), keys)
    }

    @Test
    fun theKeyIsOnlyOnTheFirstLevelOfTheItem() {
        val html = Expander.expand(list, products(mapOf("id" to "a", "name" to "A"))).html
        assertEquals(1, Regex("""\skey\s*=""").findAll(html).count())
        assertTrue(html.contains("""key="a""""))
    }

    @Test
    fun aKeyWrittenByTheAuthorIsKept() {
        val src = screen(
            """
    for p in products:
        <div key="{p.name}"><p>{p.name}</p><p>x</p></div>
    end
"""
        )
        val keys = keysOf(products(mapOf("id" to "a", "name" to "Bread")), src)
        assertEquals(listOf("/~Bread"), keys)
    }

    @Test
    fun anItemWithoutIdUsesThePosition() {
        val keys = keysOf(products(mapOf("name" to "A"), mapOf("name" to "B")))
        assertEquals(listOf("/0", "/1"), keys)
    }

    @Test
    fun valuesThatAreNotTextOrWholeNumbersAreIgnored() {
        val keys = keysOf(products(mapOf("id" to 1.5, "name" to "A"), mapOf("id" to mapOf("x" to 1), "name" to "B")))
        assertEquals(listOf("/0", "/1"), keys)
    }

    @Test
    fun theKeyIsEscaped() {
        val html = Expander.expand(list, products(mapOf("id" to "a\"b", "name" to "A"))).html
        assertTrue(html.contains("""key="a&quot;b""""))
        LayoutEngine.build(html)
    }

    @Test
    fun anItemThatMovesKeepsItsKey() {
        val a = mapOf("id" to "a", "name" to "A")
        val b = mapOf("id" to "b", "name" to "B")
        val before = LayoutEngine.build(Expander.expand(list, products(a, b)).html).root
        val after = LayoutEngine.build(Expander.expand(list, products(b, a)).html).root
        val keysBefore = (before as NativeBox).children.map { it.key }.toSet()
        val keysAfter = (after as NativeBox).children.map { it.key }.toSet()
        assertEquals(keysBefore, keysAfter)
    }

    @Test
    fun nestedForsKeyEachLevelByItsOwnItem() {
        val src = screen(
            """
    for p in products:
        <div class="card">
            for t in p.tags:
                <div>{t.name}</div>
            end
        </div>
    end
"""
        )
        val data = mapOf<String, Any?>(
            "products" to listOf(
                mapOf("id" to "p1", "tags" to listOf(mapOf("id" to "t1", "name" to "x"), mapOf("id" to "t2", "name" to "y")))
            )
        )
        val root = LayoutEngine.build(Expander.expand(src, data).html).root as NativeBox
        val card = root.children.single() as NativeBox
        assertEquals("/~p1", card.key)
        assertEquals(listOf("/~p1/~t1", "/~p1/~t2"), card.children.map { it.key })
    }

    @Test
    fun anItemWithFieldsAndNoIdGivesAWarning() {
        val src = screen(
            """
    for p in products:
        <div><input id="x" type="text" value="{p.name}"></div>
    end
"""
        )
        val e = Expander.expand(src, products(mapOf("name" to "A")))
        assertEquals(1, e.warnings.size)
        assertTrue(e.warnings[0].message.contains("no id"))
    }

    @Test
    fun noWarningWhenTheItemsHaveAnId() {
        val src = screen(
            """
    for p in products:
        <div><input type="text" value="{p.name}"></div>
    end
"""
        )
        val e = Expander.expand(src, products(mapOf("id" to "a", "name" to "A")))
        assertTrue(e.warnings.isEmpty())
    }

    @Test
    fun noWarningWhenThereIsNothingToFollowTheItem() {
        val e = Expander.expand(list, products(mapOf("name" to "A")))
        assertTrue(e.warnings.isEmpty())
    }

    @Test
    fun virtualListNoLongerNeedsAWrittenKey() {
        val src = screen(
            """
    <div virtual>
        for p in products:
            <div class="item"><p>{p.name}</p></div>
        end
    </div>
"""
        )
        val r = LayoutEngine.build(Expander.expand(src, products(mapOf("id" to "a", "name" to "A"), mapOf("name" to "B"))).html)
        val list = (r.root as NativeBox).children.single() as NativeBox
        assertTrue(list.virtual)
        assertEquals(2, list.children.size)
        assertFalse(list.children.any { it.key.isEmpty() })
    }
}
