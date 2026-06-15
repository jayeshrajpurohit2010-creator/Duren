package com.duren.app.data.ember.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [PostMode.wire] is the exact string persisted on every ember doc. If a wire value
 * ever drifts, already-stored embers silently mis-map (a Confess could read as Named
 * and get unmasked). These tests lock the mapping and the masking rule.
 */
class PostModeTest {

    @Test
    fun `wire values map to the right mode`() {
        assertEquals(PostMode.Named, PostMode.fromWire("named"))
        assertEquals(PostMode.Anonymous, PostMode.fromWire("anonymous"))
        assertEquals(PostMode.Confess, PostMode.fromWire("confess"))
    }

    @Test
    fun `every mode round-trips through its wire string`() {
        for (mode in PostMode.entries) {
            assertEquals(mode, PostMode.fromWire(mode.wire))
        }
    }

    @Test
    fun `unknown or null wire defaults to Named`() {
        assertEquals(PostMode.Named, PostMode.fromWire(null))
        assertEquals(PostMode.Named, PostMode.fromWire(""))
        assertEquals(PostMode.Named, PostMode.fromWire("NAMED")) // matching is case-sensitive
        assertEquals(PostMode.Named, PostMode.fromWire("whatever"))
    }

    @Test
    fun `only Named is unmasked`() {
        assertFalse(PostMode.Named.isMasked)
        assertTrue(PostMode.Anonymous.isMasked)
        assertTrue(PostMode.Confess.isMasked)
    }
}
