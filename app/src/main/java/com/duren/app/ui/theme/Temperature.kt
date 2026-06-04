package com.duren.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The temperature of an Ember, derived purely from its echo count.
 *
 * Source of truth: June 4 brief §3.3. Temperature is NOT gamification — it is a
 * presence signal. It is a client-derived value (never stored), so it always
 * reflects the live echo count. Phase 2+ replaces the raw count with the
 * 7-layer algorithm's echo_velocity, but the buckets stay the same.
 */
enum class Temperature(
    val label: String,
    val emoji: String,
    val color: Color
) {
    Cold("Cold", "❄️", DurenColors.TempCold),
    Warm("Warm", "🌡", DurenColors.TempWarm),
    Hot("Hot", "🔥", DurenColors.TempHot),
    Blazing("Blazing", "🌋", DurenColors.TempBlazing),
    DrumCircle("Drum Circle", "☀️", DurenColors.TempDrumCircle);

    companion object {
        /** 0 → Cold, 1–4 → Warm, 5–9 → Hot, 10–19 → Blazing, 20+ → Drum Circle. */
        fun fromEchoCount(count: Int): Temperature = when {
            count >= 20 -> DrumCircle
            count >= 10 -> Blazing
            count >= 5 -> Hot
            count >= 1 -> Warm
            else -> Cold
        }
    }
}
