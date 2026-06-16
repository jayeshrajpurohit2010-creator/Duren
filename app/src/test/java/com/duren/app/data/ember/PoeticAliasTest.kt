package com.duren.app.data.ember

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Confess mode (Feature 15) stamps an ember with one of fifty poetic masks. The spec
 * asks for fifty; the alias is stored at post time and must stay stable across reads,
 * so [PoeticAlias.random] is seedable. These tests hold both promises.
 */
class PoeticAliasTest {

    @Test
    fun `there are fifty masks, as the spec asks`() {
        assertEquals(50, PoeticAlias.all.size)
    }

    @Test
    fun `no mask is blank and none repeat`() {
        assertTrue(PoeticAlias.all.none { it.isBlank() })
        assertEquals(PoeticAlias.all.size, PoeticAlias.all.toSet().size)
    }

    @Test
    fun `the same seed always yields the same mask`() {
        // The ember id seeds it, so a confession keeps its name on every reload.
        assertEquals(PoeticAlias.random(42L), PoeticAlias.random(42L))
        assertEquals(PoeticAlias.random(12345L), PoeticAlias.random(12345L))
    }

    @Test
    fun `a random mask is always one of the fifty`() {
        repeat(200) { i ->
            assertTrue(PoeticAlias.random(i.toLong()) in PoeticAlias.all)
        }
    }

    @Test
    fun `different seeds can reach different masks`() {
        val seen = (0 until 200).map { PoeticAlias.random(it.toLong()) }.toSet()
        assertTrue("seeding should spread across masks", seen.size > 1)
    }
}
