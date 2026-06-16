package com.duren.app.feature.feed

import com.duren.app.data.ember.model.Ember
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FeedRanking decides what the whole network sees first, so its behaviour is pinned
 * here: recency must beat raw echo count (gravity decay), engagement and cold marks
 * must move the score the right way, and an ember in your affinity graph must outrank
 * an identical stranger's — without a hot stranger being shut out entirely.
 */
class FeedRankingTest {

    private val now = 1_700_000_000_000L
    private fun hoursAgo(h: Double): Timestamp =
        Timestamp(((now - (h * 3_600_000).toLong()) / 1000), 0)

    private fun ember(
        id: String,
        echoes: Int = 0,
        coldMarks: Int = 0,
        ageHours: Double = 1.0,
        tribeId: String? = null,
        authorId: String = "someone",
    ) = Ember(
        id = id,
        authorId = authorId,
        tribeId = tribeId,
        echoCount = echoes,
        coldMarkCount = coldMarks,
        createdAt = hoursAgo(ageHours),
    )

    @Test
    fun `more echoes at the same age means more heat`() {
        val hot = ember("hot", echoes = 10, ageHours = 2.0)
        val cool = ember("cool", echoes = 2, ageHours = 2.0)
        assertTrue(FeedRanking.heat(hot, now, false) > FeedRanking.heat(cool, now, false))
    }

    @Test
    fun `a fresh quiet ember outranks an old louder one`() {
        // Gravity decay: a 1h-old post with 2 echoes beats a 40h-old post with 5.
        val fresh = ember("fresh", echoes = 2, ageHours = 1.0)
        val old = ember("old", echoes = 5, ageHours = 40.0)
        val ranked = FeedRanking.rank(listOf(old, fresh), now) { false }
        assertEquals("fresh", ranked.first().id)
    }

    @Test
    fun `cold marks cool an ember down`() {
        val clean = ember("clean", echoes = 10, coldMarks = 0, ageHours = 3.0)
        val marked = ember("marked", echoes = 10, coldMarks = 8, ageHours = 3.0)
        assertTrue(FeedRanking.heat(clean, now, false) > FeedRanking.heat(marked, now, false))
    }

    @Test
    fun `an ember from your people outranks an identical stranger`() {
        val mine = ember("mine", echoes = 3, ageHours = 5.0, tribeId = "t1")
        val stranger = ember("stranger", echoes = 3, ageHours = 5.0)
        val ranked = FeedRanking.rank(listOf(stranger, mine), now) { it.id == "mine" }
        assertEquals("mine", ranked.first().id)
    }

    @Test
    fun `a blazing stranger can still break through your affinity boost`() {
        // Boost is ~4x, not infinite: a wildly hotter stranger ember still wins.
        val mine = ember("mine", echoes = 1, ageHours = 5.0)
        val blazing = ember("blazing", echoes = 200, ageHours = 1.0)
        val ranked = FeedRanking.rank(listOf(mine, blazing), now) { it.id == "mine" }
        assertEquals("blazing", ranked.first().id)
    }

    @Test
    fun `engagement floors at zero so a pile of cold marks never goes negative`() {
        val buried = ember("buried", echoes = 0, coldMarks = 5, ageHours = 1.0)
        assertTrue("heat must stay positive", FeedRanking.heat(buried, now, false) > 0.0)
    }

    @Test
    fun `the affinity boost is exactly the documented multiple`() {
        val e = ember("e", echoes = 4, ageHours = 6.0)
        val plain = FeedRanking.heat(e, now, false)
        val boosted = FeedRanking.heat(e, now, true)
        assertEquals(plain * FeedRanking.AFFINITY_BOOST, boosted, 1e-9)
    }
}
