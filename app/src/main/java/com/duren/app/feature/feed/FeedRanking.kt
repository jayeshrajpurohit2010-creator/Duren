package com.duren.app.feature.feed

import com.duren.app.data.ember.model.Ember

/**
 * The Clearing's heat ranking — a faithful Phase-1 slice of the Algorithm Spec's
 * scoring layer, runnable with no Cloud Functions.
 *
 * Pulled out of [FeedViewModel] as a pure, clock-injectable function so the ordering
 * that decides what the whole network sees first can actually be tested, instead of
 * silently drifting between releases. The ViewModel delegates here.
 *
 *   heat = (echoes − coldMarks, floored at 0, +1) / (ageHours + 2) ^ GRAVITY
 *          × (affinity ? BOOST : 1)
 *
 * Engagement lifted by recency, damped by age (classic gravity decay): a fresh post
 * still surfaces while one that's catching fire outranks an older quiet one. Cold marks
 * subtract (light anti-gaming). Embers from a tribe you've joined or someone in your
 * Nest get the [AFFINITY_BOOST] — personalization, client-side — but a truly blazing
 * stranger ember can still break through.
 */
object FeedRanking {

    // Higher = recency wins harder over raw echo count. 1.5 ≈ Hacker-News gravity.
    const val HEAT_GRAVITY = 1.5

    // Heat multiplier for embers from your tribes / Nest. ~4× lifts your people well
    // above strangers while still letting a smoking-hot stranger ember break through.
    const val AFFINITY_BOOST = 4.0

    /** The heat score of a single ember at [nowMillis]; higher sorts first. */
    fun heat(ember: Ember, nowMillis: Long, boosted: Boolean): Double {
        val createdMs = ember.createdAt?.toDate()?.time ?: nowMillis
        val ageHours = (nowMillis - createdMs).coerceAtLeast(0L) / 3_600_000.0
        val engagement = (ember.echoCount - ember.coldMarkCount).coerceAtLeast(0) + 1
        val base = engagement.toDouble() / Math.pow(ageHours + 2.0, HEAT_GRAVITY)
        return if (boosted) base * AFFINITY_BOOST else base
    }

    /** Order embers hottest-first. [boosted] tells whether an ember is in the viewer's affinity graph. */
    fun rank(embers: List<Ember>, nowMillis: Long, boosted: (Ember) -> Boolean): List<Ember> =
        embers.sortedByDescending { heat(it, nowMillis, boosted(it)) }
}
