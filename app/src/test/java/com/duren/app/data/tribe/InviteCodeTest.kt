package com.duren.app.data.tribe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tribe invite codes (F37) are a pure function of the tribe's doc id — a hand-rolled
 * 31-multiplier hash, deliberately NOT String.hashCode so the contract survives any
 * platform. That determinism is what lets two devices backfill the same code and a
 * scanned code resolve. These tests hold the format (six digits) and that determinism.
 */
class InviteCodeTest {

    @Test
    fun `the same id always derives the same code`() {
        assertEquals(
            TribeRepository.inviteCodeFor("anime-late-night"),
            TribeRepository.inviteCodeFor("anime-late-night")
        )
    }

    @Test
    fun `every code is exactly six digits`() {
        listOf("a", "tribe-1", "night-owls", "x9Z", "the-clearing", "").forEach { id ->
            val code = TribeRepository.inviteCodeFor(id)
            assertEquals("len for '$id'", 6, code.length)
            assertTrue("digits for '$id'", code.all { it.isDigit() })
        }
    }

    @Test
    fun `every code lands in the 100000 to 999999 range`() {
        listOf("a", "b", "study-grind", "vent-space", "dev-lounge").forEach { id ->
            val n = TribeRepository.inviteCodeFor(id).toInt()
            assertTrue("range for '$id' was $n", n in 100_000..999_999)
        }
    }

    @Test
    fun `distinct ids generally produce distinct codes`() {
        val ids = (0 until 100).map { "tribe-$it" }
        val codes = ids.map { TribeRepository.inviteCodeFor(it) }.toSet()
        // Not a perfect hash, but collisions should be rare across a small catalog.
        assertTrue("too many collisions: ${codes.size}/100", codes.size >= 95)
    }
}
