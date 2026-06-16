package com.duren.app.ui.components

import com.duren.app.ui.theme.DurenColors
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EmberDecay is how ephemerality is *made visible* — the one bit of signature
 * atmosphere computed entirely on-device (no Blaze opacity Cloud Function). If these
 * anchors drift, embers stop visibly burning, which is the whole product. So we pin
 * the Phase 0 curve: full life → 1.0, 2h → 0.40, 1h → 0.20, dead → a 0.15 floor that
 * never fully vanishes before expiry, plus the teal→red burning bar at the ends.
 */
class EmberDecayTest {

    private val created = Timestamp(0, 0)
    private val expiryMillis = 48L * 3600 * 1000          // 48h lifespan
    private val expiry = Timestamp(48L * 3600, 0)

    /** now = the instant with [hoursLeft] hours until expiry. */
    private fun nowWith(hoursLeft: Float): Long =
        expiryMillis - (hoursLeft * 3_600_000f).toLong()

    @Test
    fun `a fresh ember is fully opaque, unblurred, full bar, teal`() {
        val d = EmberDecay.of(created, expiry, nowMillis = nowWith(48f))
        assertEquals(1f, d.opacity, 0.001f)
        assertEquals(0f, d.blur.value, 0.001f)
        assertEquals(1f, d.remaining, 0.001f)
        assertEquals(DurenColors.AccentTeal, d.barColor)
    }

    @Test
    fun `two hours left is about forty percent opacity`() {
        val d = EmberDecay.of(created, expiry, nowMillis = nowWith(2f))
        assertEquals(0.40f, d.opacity, 0.001f)
    }

    @Test
    fun `one hour left is about twenty percent opacity`() {
        val d = EmberDecay.of(created, expiry, nowMillis = nowWith(1f))
        assertEquals(0.20f, d.opacity, 0.001f)
    }

    @Test
    fun `a dead ember floors at fifteen percent and never goes lower`() {
        val d = EmberDecay.of(created, expiry, nowMillis = expiryMillis + 5_000)
        assertEquals(0.15f, d.opacity, 0.001f)
        assertEquals(0f, d.remaining, 0.001f)
    }

    @Test
    fun `opacity always stays within the 0_15 to 1 band across the whole life`() {
        var h = 48f
        while (h >= -1f) {
            val o = EmberDecay.of(created, expiry, nowMillis = nowWith(h)).opacity
            assertTrue("opacity $o at ${h}h out of band", o in 0.15f..1f)
            h -= 0.25f
        }
    }

    @Test
    fun `blur only appears in the final stretch`() {
        assertEquals(0f, EmberDecay.of(created, expiry, nowMillis = nowWith(2f)).blur.value, 0.001f)
        assertTrue(EmberDecay.of(created, expiry, nowMillis = nowWith(0.5f)).blur.value > 0f)
    }

    @Test
    fun `the bar burns red in the last hour`() {
        assertEquals(Color0xFFFF4D4F, EmberDecay.of(created, expiry, nowMillis = nowWith(1f)).barColor)
    }

    @Test
    fun `unknown expiry treats the ember as fully alive rather than guessing`() {
        val d = EmberDecay.of(created, expiresAt = null, nowMillis = 0)
        assertEquals(1f, d.opacity, 0.001f)
        assertEquals(1f, d.remaining, 0.001f)
        assertEquals(DurenColors.AccentTeal, d.barColor)
    }

    private companion object {
        val Color0xFFFF4D4F = androidx.compose.ui.graphics.Color(0xFFFF4D4F)
    }
}
