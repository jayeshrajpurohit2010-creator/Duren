package com.duren.app.data.signal.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Signals are written client-side and read back by [SignalType.fromWire]. A renamed
 * wire string would make existing notifications fall through to "did something", so
 * these tests pin every wire value — including the newer `reply`.
 */
class SignalTypeTest {

    @Test
    fun `every type round-trips through its wire string`() {
        for (type in SignalType.entries) {
            assertEquals(type, SignalType.fromWire(type.wire))
        }
    }

    @Test
    fun `known wire values map correctly`() {
        assertEquals(SignalType.Reply, SignalType.fromWire("reply"))
        assertEquals(SignalType.Whisper, SignalType.fromWire("whisper"))
        assertEquals(SignalType.NestRequest, SignalType.fromWire("nest_request"))
        assertEquals(SignalType.MutualSpark, SignalType.fromWire("mutual_spark"))
        assertEquals(SignalType.SmokeSignal, SignalType.fromWire("smoke_signal"))
    }

    @Test
    fun `unknown or null wire falls back to Unknown`() {
        assertEquals(SignalType.Unknown, SignalType.fromWire(null))
        assertEquals(SignalType.Unknown, SignalType.fromWire(""))
        assertEquals(SignalType.Unknown, SignalType.fromWire("reply_v2"))
    }
}
