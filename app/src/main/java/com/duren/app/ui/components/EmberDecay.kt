package com.duren.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.duren.app.ui.theme.DurenColors
import com.google.firebase.Timestamp

/**
 * The visible burning of an Ember — computed client-side from its own timestamps,
 * so there is no Cloud Function and no stored `opacityFactor` field. (The spec's
 * `onOpacityDecay` function is a Blaze-only convenience; we don't need it — the
 * client already knows `createdAt` and `expiresAt`.)
 *
 * Maps the Phase 0 atmosphere intent directly:
 *   - 48h / fresh  → full opacity, no blur, teal burning bar
 *   - ~6h left     → amber bar, still crisp
 *   - 2h left      → ~40% opacity
 *   - 1h left      → ~20% opacity, slightly blurred, red bar
 *   - dying        → floor of 15% so it never fully vanishes before expiry
 */
data class EmberDecay(
    val opacity: Float,
    val blur: Dp,
    /** Fraction of total lifespan remaining, 0f..1f — drives the burning bar width. */
    val remaining: Float,
    /** Bar color: teal (fresh) → amber (~6h) → red (<1h). */
    val barColor: Color
) {
    companion object {
        private val Teal = DurenColors.AccentTeal
        private val Amber = Color(0xFFFFA040)
        private val Red = Color(0xFFFF4D4F)

        fun of(
            createdAt: Timestamp?,
            expiresAt: Timestamp?,
            nowMillis: Long = System.currentTimeMillis()
        ): EmberDecay {
            val expiry = expiresAt?.toDate()?.time
            val created = createdAt?.toDate()?.time
            if (expiry == null) {
                // Unknown lifespan — treat as fully alive rather than guessing.
                return EmberDecay(1f, 0.dp, 1f, Teal)
            }
            val msLeft = (expiry - nowMillis).coerceAtLeast(0L)
            val hoursLeft = msLeft / 3_600_000f

            val total = if (created != null) (expiry - created).coerceAtLeast(1L) else 48L * 3_600_000L
            val remaining = (msLeft.toFloat() / total).coerceIn(0f, 1f)

            // Opacity by absolute hours remaining (piecewise, per Phase 0 anchors).
            val opacity = when {
                hoursLeft >= 6f -> 1f
                hoursLeft >= 2f -> lerp(0.40f, 1f, (hoursLeft - 2f) / 4f)   // 2h→6h
                hoursLeft >= 1f -> lerp(0.20f, 0.40f, hoursLeft - 1f)        // 1h→2h
                else -> lerp(0.15f, 0.20f, hoursLeft)                        // 0h→1h
            }.coerceIn(0.15f, 1f)

            // Blur only kicks in in the final stretch, peaking as it dies.
            val blur = when {
                hoursLeft >= 1.5f -> 0.dp
                else -> lerp(2.5f, 0f, (hoursLeft / 1.5f)).dp
            }

            val barColor = when {
                hoursLeft >= 12f -> Teal
                hoursLeft >= 6f -> lerpColor(Amber, Teal, (hoursLeft - 6f) / 6f)  // 6h→12h
                hoursLeft >= 1f -> lerpColor(Red, Amber, (hoursLeft - 1f) / 5f)   // 1h→6h
                else -> Red
            }

            return EmberDecay(opacity, blur, remaining, barColor)
        }

        private fun lerp(a: Float, b: Float, t: Float): Float =
            a + (b - a) * t.coerceIn(0f, 1f)

        private fun lerpColor(a: Color, b: Color, t: Float): Color {
            val k = t.coerceIn(0f, 1f)
            return Color(
                red = a.red + (b.red - a.red) * k,
                green = a.green + (b.green - a.green) * k,
                blue = a.blue + (b.blue - a.blue) * k,
                alpha = 1f
            )
        }
    }
}
