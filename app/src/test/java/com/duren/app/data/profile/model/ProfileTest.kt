package com.duren.app.data.profile.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [Profile.isBanked] drives the AIM-style away note: it should show only while the note
 * is both set AND still within its window, and auto-expire on its own so a stale "back at
 * 11PM" doesn't linger for days. These tests pin the two halves of that AND — a blank
 * note is never banked no matter the timestamp, and a lapsed `bankedUntil` switches it off
 * even while the text is still present. The boundary against the literal current
 * millisecond can't be pinned deterministically (the property reads the system clock with
 * no injection point), so these use far-past / far-future offsets to stay stable.
 */
class ProfileTest {

    private fun future(): Timestamp = Timestamp(java.util.Date(System.currentTimeMillis() + 86_400_000L))
    private fun past(): Timestamp = Timestamp(java.util.Date(System.currentTimeMillis() - 86_400_000L))

    @Test
    fun `a set note still in its window reads as banked`() {
        val p = Profile(bankedStatus = "back at 11PM", bankedUntil = future())
        assertTrue(p.isBanked)
    }

    @Test
    fun `a note whose window has lapsed auto-expires even with text still set`() {
        val p = Profile(bankedStatus = "back at 11PM", bankedUntil = past())
        assertFalse(p.isBanked)
    }

    @Test
    fun `a blank note is never banked, however far in the future the timestamp`() {
        assertFalse(Profile(bankedStatus = "", bankedUntil = future()).isBanked)
        assertFalse(Profile(bankedStatus = "   ", bankedUntil = future()).isBanked)
    }

    @Test
    fun `a note with no expiry timestamp is not banked`() {
        // bankedUntil null -> the window check fails closed.
        assertFalse(Profile(bankedStatus = "away", bankedUntil = null).isBanked)
    }

    @Test
    fun `a default profile is not banked`() {
        assertFalse(Profile().isBanked)
    }
}
