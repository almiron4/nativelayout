/*
 * Author: Adilson C. Rafael (adilson889)
 * Date: 2026-10-18
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
import org.junit.Test

/** The CSS properties, the state selectors (:pressed, :disabled, :focus) and @media. */
class CssFeaturesTest {
    private fun styleOf(css: String, parent: String = ""): NativeStyle {
        val html =
            if (parent.isEmpty()) """<div id="a" style="$css"></div>"""
            else """<div style="$parent"><div id="a" style="$css"></div></div>"""
        return LayoutEngine.build(html).byId["a"]!!.style
    }

    private fun warnsOf(css: String): List<String> =
        LayoutEngine.build("""<div id="a" style="$css"></div>""").warnings.map { it.message }

    private fun expandHtml(css: String, dark: Boolean = false): String =
        Expander.expand(SCREEN, emptyMap(), { name -> if (name == "t.css") css else null }, dark).html

    private fun color(argb: Long): Int = argb.toInt()

    // ---------- text ----------

    @Test
    fun textAlign() {
        assertEquals(TextAlign.CENTER, styleOf("text-align: center").textAlign)
        assertEquals(TextAlign.END, styleOf("text-align: right").textAlign)
        assertEquals(TextAlign.START, styleOf("text-align: left").textAlign)
        assertEquals(TextAlign.CENTER, styleOf("", "text-align: center").textAlign)
        assertTrue(warnsOf("text-align: justify").any { "justify" in it })
    }

    @Test
    fun textStyle() {
        val s = styleOf(
            "font-style: italic; text-decoration: underline line-through; letter-spacing: 2px; " +
                "text-transform: uppercase; font-family: 'Courier New', monospace"
        )
        assertTrue(s.italic)
        assertTrue(s.underline && s.strike)
        assertEquals(Length.px(2f), s.letterSpacing)
        assertEquals(TextTransform.UPPERCASE, s.textTransform)
        assertEquals(FontFamily.MONOSPACE, s.fontFamily)
        assertTrue(!styleOf("text-decoration: underline; text-decoration: none").underline)
        assertEquals(LineHeight.Multiple(1.5f), styleOf("line-height: 1.5").lineHeight)
        assertEquals(LineHeight.Fixed(Length.px(24f)), styleOf("line-height: 24px").lineHeight)
        assertEquals(LineHeight.Multiple(1.5f), styleOf("line-height: 150%").lineHeight)
        val inherited = styleOf("", "font-style: italic; text-transform: uppercase")
        assertTrue(inherited.italic && inherited.textTransform == TextTransform.UPPERCASE)
    }

    @Test
    fun textTransformIsAppliedToTheText() {
        val upper = LayoutEngine.build(
            """<div style="text-transform: uppercase; text-decoration: line-through"><p id="p">ola mundo</p></div>"""
        ).byId["p"] as NativeText
        assertEquals("OLA MUNDO", upper.plainText)
        assertTrue(upper.spans.all { it.strike })

        val capital = LayoutEngine.build(
            """<p id="p" style="text-transform: capitalize">ola   mundo bonito</p>"""
        ).byId["p"] as NativeText
        assertEquals("Ola Mundo Bonito", capital.plainText)
    }

    // ---------- box ----------

    @Test
    fun longhands() {
        val s = styleOf("padding: 1px; padding-top: 4px; margin-left: 2rem; border-width: 2px; border-color: #ff0000")
        assertEquals(Length.px(4f), s.padding.top)
        assertEquals(Length.px(1f), s.padding.left)
        assertEquals(Length.rem(2f), s.margin.left)
        assertEquals(Length.px(2f), s.borderWidth)
        assertEquals(color(0xFFFF0000), s.borderColor)
        assertEquals(Length.ZERO, styleOf("border: 1px solid #000; border-style: none").borderWidth)
    }

    @Test
    fun opacityAndMinMax() {
        assertEquals(0.5f, styleOf("opacity: 0.5").opacity)
        assertEquals(0.4f, styleOf("opacity: 40%").opacity)
        assertEquals(1f, styleOf("opacity: 3").opacity)
        val s = styleOf("min-width: 100px; max-width: 20rem; min-height: 1rem; max-height: 50px")
        assertEquals(Length.px(100f), s.minWidth)
        assertEquals(Length.rem(20f), s.maxWidth)
        assertEquals(Length.rem(1f), s.minHeight)
        assertEquals(Length.px(50f), s.maxHeight)
        assertNull(styleOf("max-width: 10px; max-width: none").maxWidth)
    }

    @Test
    fun overflowAlignSelfWrapAndJustify() {
        assertTrue(styleOf("overflow: hidden").overflowHidden)
        assertTrue(warnsOf("overflow: scroll").isNotEmpty())
        assertEquals(Align.CENTER, styleOf("align-self: center").alignSelf)
        assertTrue(styleOf("flex-wrap: wrap").wrap)
        assertEquals(Justify.BETWEEN, styleOf("justify-content: space-between").justify)
        assertEquals(Justify.AROUND, styleOf("justify-content: space-around").justify)
        assertEquals(Justify.EVENLY, styleOf("justify-content: space-evenly").justify)
    }

    @Test
    fun shadowAndGradient() {
        assertEquals(Length.px(6f), styleOf("box-shadow: 0 4px 12px rgba(0,0,0,0.3)").elevation)
        assertEquals(Length.px(2f), styleOf("box-shadow: 0px 4px #000").elevation)
        assertEquals(Length.ZERO, styleOf("box-shadow: 0 4px 12px #000; box-shadow: none").elevation)

        val g1 = styleOf("background: linear-gradient(90deg, #ff0000, rgba(0, 0, 255, 0.5) 80%)").gradient
        assertNotNull(g1)
        assertEquals(90, g1?.angle)
        assertEquals(2, g1?.colors?.size)
        assertEquals(color(0xFFFF0000), g1?.colors?.get(0))

        val g2 = styleOf("background-image: linear-gradient(to bottom right, #000, #fff, #00f)").gradient
        assertEquals(135, g2?.angle)
        assertEquals(3, g2?.colors?.size)

        assertEquals(180, styleOf("background: linear-gradient(#000, #fff)").gradient?.angle)
        assertNull(styleOf("background: linear-gradient(#000, #fff); background: #123456").gradient)
        assertTrue(warnsOf("background: linear-gradient(#000)").isNotEmpty())
    }

    @Test
    fun invalidValuesAreIgnored() {
        val s = styleOf("opacity: abc; max-width: 3; text-align: x")
        assertEquals(1f, s.opacity)
        assertNull(s.maxWidth)
    }

    @Test
    fun existingBehaviorIsKept() {
        val s = styleOf("padding: 1rem 2rem; border: 2px solid #ff0000; justify-content: center; gap: 1rem")
        assertEquals(Length.rem(2f), s.padding.left)
        assertEquals(Length.px(2f), s.borderWidth)
        assertEquals(Justify.CENTER, s.justify)
        assertEquals(Length.rem(1f), s.gap)
        assertTrue(warnsOf("position: absolute").any { "not supported" in it })
    }

    // ---------- :pressed, :disabled, :focus ----------

    @Test
    fun statesFromAttributes() {
        val tree = LayoutEngine.build(
            """<button id="a" style-pressed="background: #00ff00; color: #000" """ +
                """style-disabled="opacity: 0.3; background: #cccccc" style-focus="border-color: #0000ff">x</button>"""
        )
        val states = tree.byId["a"]!!.style.states
        assertEquals(color(0xFF00FF00), states?.pressed?.backgroundColor)
        assertEquals(color(0xFF000000), states?.pressed?.textColor)
        assertEquals(0.3f, states?.disabled?.opacity)
        assertEquals(color(0xFF0000FF), states?.focused?.borderColor)
        assertTrue(
            LayoutEngine.build("""<button id="a" style-pressed="opacity: 0.3">x</button>""").warnings.isNotEmpty()
        )
    }

    @Test
    fun expanderWritesTheStates() {
        val css = """
            :root { --accent: #112233; }
            .btn { background: var(--accent); color: white; }
            .btn:pressed { background: #445566; }
            button:disabled { opacity: 0.4; }
            #go:focus { border-color: red; }
        """.trimIndent()
        val html = expandHtml(css)
        assertTrue(html, "style-pressed=\"background: #445566\"" in html)
        assertTrue(html, "style-disabled=\"opacity: 0.4\"" in html)
        assertTrue(html, "style-focus=\"border-color: red\"" in html)
        assertTrue(html, "style=\"background: #112233; color: white\"" in html)

        val style = LayoutEngine.build(html).byId["go"]!!.style
        assertEquals(color(0xFF445566), style.states?.pressed?.backgroundColor)
        assertEquals(0.4f, style.states?.disabled?.opacity)
    }

    @Test
    fun activeIsAnAliasAndHoverIsRejected() {
        assertTrue("style-pressed" in expandHtml(".btn:active { background: #000 }"))
        val error = runCatching { expandHtml(".btn:hover { background: #000 }") }.exceptionOrNull()
        assertTrue(error?.message ?: "no error", "not supported" in (error?.message ?: ""))
        assertTrue(runCatching { expandHtml(":root { --a: red; }") }.isSuccess)
    }

    // ---------- @media ----------

    @Test
    fun mediaPicksTheTheme() {
        val css = """
            :root { --bg: #ffffff; }
            @media (prefers-color-scheme: dark) {
                :root { --bg: #000000; }
                .btn { color: yellow; }
            }
            @media (prefers-color-scheme: light) { .btn { color: blue; } }
            .btn { background: var(--bg); }
        """.trimIndent()
        val light = expandHtml(css, dark = false)
        val dark = expandHtml(css, dark = true)
        assertTrue(light, "background: #ffffff" in light && "color: blue" in light && "yellow" !in light)
        assertTrue(dark, "background: #000000" in dark && "color: yellow" in dark && "blue" !in dark)
        // the cache must not mix the two themes
        assertEquals(dark, expandHtml(css, dark = true))
        assertEquals(light, expandHtml(css, dark = false))
    }

    @Test
    fun mediaErrors() {
        val unsupported = runCatching { expandHtml("@media (min-width: 600px) { .btn { color: red } }") }.exceptionOrNull()
        assertTrue(unsupported?.message ?: "no error", "@media" in (unsupported?.message ?: ""))
        val unclosed = runCatching { expandHtml("@media (prefers-color-scheme: dark) { .btn { color: red }") }.exceptionOrNull()
        assertNotNull(unclosed)
    }

    private companion object {
        val SCREEN = """
<nativelayout>
<link rel="stylesheet" href="t.css">
<body>
    <button id="go" class="btn">ok</button>
</body>
</nativelayout>""".trim()
    }
}
