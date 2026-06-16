package com.duren.app.data.ember.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Ember model carries the two pieces of read-time logic the feed depends on:
 * [Ember.photos] (the multi-image accessor, with back-compat onto the legacy single
 * [Ember.mediaUrl]) and [Ember.pinnedNow] (a Floating Lantern only counts while its
 * window is open). Both are pure, so we pin them here.
 */
class EmberTest {

    @Test
    fun `photos prefers the multi-image list when present`() {
        val e = Ember(mediaUrl = "data:legacy", mediaUrls = listOf("data:a", "data:b", "data:c"))
        assertEquals(listOf("data:a", "data:b", "data:c"), e.photos)
    }

    @Test
    fun `photos falls back to the single legacy url for older embers`() {
        val e = Ember(mediaUrl = "data:legacy", mediaUrls = emptyList())
        assertEquals(listOf("data:legacy"), e.photos)
    }

    @Test
    fun `a text-only ember has no photos`() {
        assertTrue(Ember(mediaUrl = null, mediaUrls = emptyList()).photos.isEmpty())
    }

    @Test
    fun `a pin counts while its window is open`() {
        val now = Timestamp(1_000_000, 0)
        val future = Timestamp(1_003_600, 0)
        assertTrue(Ember(isPinned = true, pinExpiresAt = future).pinnedNow(now))
    }

    @Test
    fun `a lapsed pin no longer counts`() {
        val now = Timestamp(1_000_000, 0)
        val past = Timestamp(999_000, 0)
        assertFalse(Ember(isPinned = true, pinExpiresAt = past).pinnedNow(now))
    }

    @Test
    fun `an unpinned ember is never pinned even with a future window`() {
        val now = Timestamp(1_000_000, 0)
        val future = Timestamp(1_003_600, 0)
        assertFalse(Ember(isPinned = false, pinExpiresAt = future).pinnedNow(now))
    }
}
