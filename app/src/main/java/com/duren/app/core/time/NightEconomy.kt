package com.duren.app.core.time

import java.util.Calendar
import java.util.TimeZone

/**
 * The current chapter of Duren's day, used by the "Night Economy" features.
 *
 * - [DeadHours]  — 2:00–2:59 AM. The campfire rests: composing and DMs go quiet
 *   so the network has a genuine off-switch instead of doom-scrolling at 3 AM.
 * - [MorningFade] — 6:00–6:59 AM. A gold-tinted farewell window as the night's
 *   embers cool toward expiry.
 * - [Day] — everything else; the app behaves normally.
 */
enum class NightPhase { DeadHours, MorningFade, Day }

/**
 * Decides the [NightPhase] from a wall clock — **entirely on-device, for free.**
 *
 * This is the whole answer to "how do you do Dead Hours country-by-country?": there
 * is no server, no Cloud Function, no per-country job and no Firebase read. We just
 * ask the device (or a tribe's home timezone) what hour it is. A phone in Mumbai and
 * a phone in London each compute their own correct phase locally at zero cost.
 *
 * Tribe-scoped Dead Hours pass the tribe's `homeTimezone`; the global Clearing passes
 * `null` to use the viewer's own timezone.
 */
object NightEconomy {

    /**
     * @param timezone an IANA id like "Asia/Kolkata" (a tribe's home timezone), or
     *        null/blank to use the device's current timezone.
     */
    fun phaseFor(timezone: String? = null): NightPhase {
        val tz = if (timezone.isNullOrBlank()) {
            TimeZone.getDefault()
        } else {
            // getTimeZone never throws — an unknown id falls back to GMT, which we
            // then treat as "no special phase" rather than crashing.
            TimeZone.getTimeZone(timezone)
        }
        val hour = Calendar.getInstance(tz).get(Calendar.HOUR_OF_DAY)
        return phaseForHour(hour)
    }

    /** Pure mapping from a 24h hour to a phase — extracted so it's trivially testable. */
    fun phaseForHour(hour: Int): NightPhase = when (hour) {
        2 -> NightPhase.DeadHours
        6 -> NightPhase.MorningFade
        else -> NightPhase.Day
    }
}
