package com.duren.app.data.mood.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mood Canvas paints the avatar aura from a 1-5 value, and the spec fixes both the colour
 * and the word for each step (1 blue/Heavy ... 5 teal/Alight). These tests pin that exact
 * mapping plus the out-of-range behaviour: [Mood.isSet] is true only inside 1..5, an
 * unset/garbage value paints the neutral teal default and an empty label rather than
 * crashing, and the five real moods all map to distinct colours and labels (a copy-paste
 * slip that doubled one up would be invisible in the UI but caught here).
 */
class MoodTest {

    // --- isSet ---

    @Test
    fun `isSet is true exactly on one through five`() {
        for (m in 1..5) assertTrue("mood $m should be set", Mood(mood = m).isSet)
    }

    @Test
    fun `isSet is false for zero, negatives, and above five`() {
        assertFalse(Mood(mood = 0).isSet)
        assertFalse(Mood(mood = -3).isSet)
        assertFalse(Mood(mood = 6).isSet)
    }

    // --- hexFor: the spec colours ---

    @Test
    fun `each mood paints its spec colour`() {
        assertEquals("#3B82F6", Mood.hexFor(1)) // blue — low
        assertEquals("#A855F7", Mood.hexFor(2)) // purple
        assertEquals("#6B7280", Mood.hexFor(3)) // grey — neutral
        assertEquals("#EAB308", Mood.hexFor(4)) // yellow
        assertEquals("#2DD4BF", Mood.hexFor(5)) // teal — bright
    }

    @Test
    fun `an out-of-range mood falls back to the teal default`() {
        // No-mood-set and any garbage value both paint the canonical teal aura.
        assertEquals("#2dd4bf", Mood.hexFor(0))
        assertEquals("#2dd4bf", Mood.hexFor(99))
        assertEquals("#2dd4bf", Mood.hexFor(-1))
    }

    @Test
    fun `the five mood colours are all distinct`() {
        val colours = (1..5).map { Mood.hexFor(it) }
        assertEquals(colours.size, colours.toSet().size)
    }

    // --- labelFor ---

    @Test
    fun `each mood has its spec label`() {
        assertEquals("Heavy", Mood.labelFor(1))
        assertEquals("Tender", Mood.labelFor(2))
        assertEquals("Even", Mood.labelFor(3))
        assertEquals("Bright", Mood.labelFor(4))
        assertEquals("Alight", Mood.labelFor(5))
    }

    @Test
    fun `an out-of-range mood has an empty label`() {
        assertEquals("", Mood.labelFor(0))
        assertEquals("", Mood.labelFor(7))
    }

    @Test
    fun `the five mood labels are all distinct`() {
        val labels = (1..5).map { Mood.labelFor(it) }
        assertEquals(labels.size, labels.toSet().size)
    }
}
