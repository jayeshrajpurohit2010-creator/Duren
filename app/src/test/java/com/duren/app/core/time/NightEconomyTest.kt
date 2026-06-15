package com.duren.app.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Night Economy is the one architecture-defining bit of pure logic in the app:
 * Dead Hours (2 AM) and Morning Fade (6 AM) are decided on-device from the wall clock,
 * with no server. These tests pin that mapping so a stray edit can't quietly shift the
 * quiet hours — and confirm a bad timezone id degrades instead of crashing.
 */
class NightEconomyTest {

    @Test
    fun `2 AM is Dead Hours`() {
        assertEquals(NightPhase.DeadHours, NightEconomy.phaseForHour(2))
    }

    @Test
    fun `6 AM is Morning Fade`() {
        assertEquals(NightPhase.MorningFade, NightEconomy.phaseForHour(6))
    }

    @Test
    fun `every hour maps to the documented phase`() {
        for (hour in 0..23) {
            val expected = when (hour) {
                2 -> NightPhase.DeadHours
                6 -> NightPhase.MorningFade
                else -> NightPhase.Day
            }
            assertEquals("hour $hour", expected, NightEconomy.phaseForHour(hour))
        }
    }

    @Test
    fun `dead hours and morning fade are exactly one hour each`() {
        val dead = (0..23).count { NightEconomy.phaseForHour(it) == NightPhase.DeadHours }
        val fade = (0..23).count { NightEconomy.phaseForHour(it) == NightPhase.MorningFade }
        assertEquals(1, dead)
        assertEquals(1, fade)
    }

    @Test
    fun `blank or null timezone resolves to a real phase`() {
        // Uses the device timezone; the guarantee is "returns a valid phase, no throw".
        assertValidPhase(NightEconomy.phaseFor(null))
        assertValidPhase(NightEconomy.phaseFor(""))
        assertValidPhase(NightEconomy.phaseFor("   "))
    }

    @Test
    fun `unknown timezone id falls back instead of crashing`() {
        assertValidPhase(NightEconomy.phaseFor("Not/A_Real_Zone"))
    }

    @Test
    fun `a real IANA timezone resolves`() {
        assertValidPhase(NightEconomy.phaseFor("Asia/Kolkata"))
        assertValidPhase(NightEconomy.phaseFor("America/New_York"))
    }

    private fun assertValidPhase(phase: NightPhase) {
        assertTrue(phase == NightPhase.DeadHours || phase == NightPhase.MorningFade || phase == NightPhase.Day)
    }
}
