package com.duren.app.ui.theme

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the contrast invariants the glassmorphism pass leans on — the very ones whose
 * violation once made light-mode text vanish. These are pure data assertions on the two
 * palettes (no Compose runtime), so they fail fast in CI if a token edit breaks the rule.
 */
class GlassPaletteTest {

    private val dark = DarkDurenPalette
    private val light = LightDurenPalette

    @Test
    fun `primary text inverts between the palettes`() {
        // The reason two palettes exist: body text flips so it stays readable on a
        // background that also flipped.
        assertNotEquals(dark.TextPrimary, light.TextPrimary)
        assertTrue("dark-mode text should be light", dark.TextPrimary.luminance() > 0.5f)
        assertTrue("light-mode text should be dark", light.TextPrimary.luminance() < 0.5f)
    }

    @Test
    fun `OnDark tokens never invert`() {
        // These ride the vibe gradient, which is near-black in BOTH themes, so they must
        // be identical across palettes. Reusing them on a glass surface (which DOES
        // invert) is exactly the old invisible-text bug — kept here as a tripwire.
        assertEquals(dark.OnDarkPrimary, light.OnDarkPrimary)
        assertEquals(dark.OnDarkSecondary, light.OnDarkSecondary)
        assertEquals(dark.OnDarkMuted, light.OnDarkMuted)
    }

    @Test
    fun `glass fills stay translucent and follow the theme`() {
        // Glass must be see-through (that's the effect) and dark-on-dark / light-on-light
        // so whatever text the screen puts on it keeps its contrast.
        assertTrue(dark.Glass.alpha < 1f)
        assertTrue(light.Glass.alpha < 1f)
        assertTrue(dark.GlassStrong.alpha < 1f)
        assertTrue(light.GlassStrong.alpha < 1f)
        assertTrue(
            "dark glass should be a darker fill than light glass",
            dark.Glass.luminance() < light.Glass.luminance()
        )
    }

    @Test
    fun `glass rim separates the panel in each theme`() {
        // A bright rim in the dark, a faint dark rim in light — either way the panel edge
        // reads instead of dissolving into a same-coloured background.
        assertTrue(dark.GlassBorder.alpha > 0f)
        assertTrue(light.GlassBorder.alpha > 0f)
        assertTrue(
            "dark rim should be lighter than the light-mode rim",
            dark.GlassBorder.luminance() > light.GlassBorder.luminance()
        )
    }
}
