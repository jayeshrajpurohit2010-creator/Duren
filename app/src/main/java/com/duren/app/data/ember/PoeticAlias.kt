package com.duren.app.data.ember

import kotlin.random.Random

/**
 * The masks a Confession wears. When an ember is posted in Confess mode it's
 * stamped with one of these poetic aliases (Feature 15), so the feed shows
 * "The Wandering Star" instead of a bare "Anonymous" — a name without a person.
 *
 * Stored on the ember at post time so it stays stable; never derived from the
 * author (which would risk a correlation leak).
 */
object PoeticAlias {

    val all: List<String> = listOf(
        "The Wandering Star", "A Quiet Storm", "The Night Keeper",
        "Ember in the Dark", "The Moonlit Stranger", "A Lost Signal",
        "The Last Campfire", "Fading Echo", "The Dark Tide",
        "A Forgotten Flame", "The Silent Hour", "Midnight Wanderer",
        "The Unseen Hand", "A Cold Spark", "The Burning Fog",
        "Last Light Keeper", "A Ghost in Autumn", "The Hollow Star",
        "Tide of Shadows", "Ember Without Name", "The Drifting Coal",
        "A Whisper Unsent", "The Ashen Voice", "Keeper of Smoke",
        "A Dimming Lantern", "The Quiet Comet", "Shadow of the Pines",
        "A Restless Spark", "The Veiled Flame", "Ember of the Deep",
        "The Sleepless One", "A Candle's Ghost", "The Far Ember",
        "Smoke on the Water", "The Nameless Glow", "A Star Gone Out",
        "The Hush Between", "A Flicker at Dawn", "The Long Night's Friend",
        "Ash and Ember", "The Wandering Coal", "A Light Half-Seen",
        "The Dusk Caller", "A Voice in the Pines", "The Faint Aurora",
        "Keeper of the Quiet", "A Spark Adrift", "The Low Flame",
        "The Last Warmth", "A Soul Passing Through"
    )

    /** A random alias. A seed (e.g. the ember id) keeps it stable across reads. */
    fun random(seed: Long = System.nanoTime()): String =
        all[Random(seed).nextInt(all.size)]
}
