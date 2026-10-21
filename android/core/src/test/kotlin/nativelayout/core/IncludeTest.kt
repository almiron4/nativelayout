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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** `<include>` and the identity of the repetitions of a `for` (sections 5.6 and 5.7). */
class IncludeTest {
    private fun screen(body: String) = "<nativelayout>\n\n<body>\n$body\n</body>\n\n</nativelayout>\n"

    private val card = screen("    <div class=\"card\">\n        <p>{name}</p>\n        <button id=\"buy\">Buy</button>\n    </div>")

    private val files = mapOf("card.nl" to card)

    private fun expand(src: String, data: Map<String, Any?> = emptyMap()) =
        Expander.expand(src, data, { files[it] })

    private val products = listOf(
        mapOf("id" to 7, "name" to "Tea"),
        mapOf("id" to 9, "name" to "Rice")
    )

    @Test
    fun includeTakesThePlaceOfTheTag() {
        val r = expand(screen("    <include src=\"card.nl\" name=\"Hello\">"))
        assertTrue(r.html.contains("<p>Hello</p>"))
        assertTrue(!r.html.contains("<include"))
    }

    @Test
    fun idsInAForAreScopedByTheKeyOfTheItem() {
        val src = screen("for p in products:\n    <include src=\"card.nl\" name=\"{p.name}\">\nend")
        val r = expand(src, mapOf("products" to products))
        assertTrue(r.html, r.html.contains("id=\"buy~p:7\""))
        assertTrue(r.html, r.html.contains("id=\"buy~p:9\""))
        assertEquals(products[0], r.items["p:7"])
        assertEquals(products[1], r.items["p:9"])
    }

    @Test
    fun idsInAForWithoutKeyUseThePosition() {
        val src = screen("for t in names:\n    <button id=\"x\">{t}</button>\nend")
        val r = expand(src, mapOf("names" to listOf("a", "b")))
        assertTrue(r.html, r.html.contains("id=\"x~t:%p0\""))
        assertTrue(r.html, r.html.contains("id=\"x~t:%p1\""))
    }

    @Test
    fun idOutsideAForStaysAsWritten() {
        val r = expand(screen("    <button id=\"ok\">Ok</button>"))
        assertTrue(r.html.contains("id=\"ok\""))
        assertTrue(!r.html.contains("scope="))
    }

    @Test
    fun nestedForsGiveNestedPaths() {
        val src = screen("for a in as:\n    for b in bs:\n        <button id=\"x\">{b}</button>\n    end\nend")
        val r = expand(src, mapOf("as" to listOf(mapOf("id" to 1)), "bs" to listOf(mapOf("id" to 2))))
        assertTrue(r.html, r.html.contains("id=\"x~a:1/b:2\""))
        assertTrue(r.items.containsKey("a:1"))
        assertTrue(r.items.containsKey("a:1/b:2"))
    }

    @Test
    fun missingParameterNamesTheComponentAndTheIncludeLine() {
        try {
            expand(screen("    <include src=\"card.nl\">"))
            fail()
        } catch (e: ExpandError) {
            assertTrue(e.message, e.message!!.contains("card.nl"))
            assertEquals(4, e.line)
        }
    }

    @Test
    fun includeThatIncludesItselfIsAnError() {
        val loop = screen("    <include src=\"loop.nl\">")
        try {
            Expander.expand(loop, emptyMap(), { if (it == "loop.nl") loop else null })
            fail()
        } catch (e: ExpandError) {
            assertTrue(e.message, e.message!!.contains("includes itself"))
        }
    }

    @Test
    fun sessionFindsTheItemAndTheScopedField() {
        assertEquals("7", Scopes.keyOfItem(products[0]))
        assertTrue(Scopes.sameItem(mapOf("id" to 7, "name" to "Other"), products[0], "7"))
    }
}
