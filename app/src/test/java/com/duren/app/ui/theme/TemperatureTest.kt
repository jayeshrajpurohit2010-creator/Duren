package com.duren.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Temperature is a presence signal derived purely from echo count (June 4 brief §3.3),
 * never stored. These tests pin every bucket boundary so a stray `>=` edit can't quietly
 * shift when an ember reads Hot, Blazing, or Drum Circle — the gold pulse the whole feed
 * leans on.
 */
class TemperatureTest {

    @Test
    fun `zero echoes is Cold`() {
        assertEquals(Temperature.Cold, Temperature.fromEchoCount(0))
    }

    @Test
    fun `the warm band is one to four`() {
        assertEquals(Temperature.Warm, Temperature.fromEchoCount(1))
        assertEquals(Temperature.Warm, Temperature.fromEchoCount(4))
    }

    @Test
    fun `the hot band is five to nine`() {
        assertEquals(Temperature.Hot, Temperature.fromEchoCount(5))
        assertEquals(Temperature.Hot, Temperature.fromEchoCount(9))
    }

    @Test
    fun `the blazing band is ten to nineteen`() {
        assertEquals(Temperature.Blazing, Temperature.fromEchoCount(10))
        assertEquals(Temperature.Blazing, Temperature.fromEchoCount(19))
    }

    @Test
    fun `drum circle starts at twenty`() {
        assertEquals(Temperature.DrumCircle, Temperature.fromEchoCount(20))
        assertEquals(Temperature.DrumCircle, Temperature.fromEchoCount(21))
        assertEquals(Temperature.DrumCircle, Temperature.fromEchoCount(9999))
    }

    @Test
    fun `a negative count degrades to Cold instead of crashing`() {
        assertEquals(Temperature.Cold, Temperature.fromEchoCount(-1))
    }

    @Test
    fun `every band has its own label and emoji`() {
        val labels = Temperature.entries.map { it.label }.toSet()
        val emojis = Temperature.entries.map { it.emoji }.toSet()
        assertEquals(Temperature.entries.size, labels.size)
        assertEquals(Temperature.entries.size, emojis.size)
    }
}
