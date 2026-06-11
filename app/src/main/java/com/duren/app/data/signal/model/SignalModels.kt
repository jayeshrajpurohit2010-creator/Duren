package com.duren.app.data.signal.model

import com.duren.app.data.profile.model.Profile
import com.google.firebase.Timestamp

/** What a Signal (notification) is about. Drives its icon and phrasing. */
enum class SignalType(val wire: String) {
    NestRequest("nest_request"),
    NestAccepted("nest_accepted"),
    Echo("echo"),
    Whisper("whisper"),
    Dm("dm"),
    Nudge("nudge"),
    MutualSpark("mutual_spark"),
    Hearth("hearth"),
    Testimonial("testimonial"),
    SmokeSignal("smoke_signal"),
    Unknown("unknown");

    companion object {
        fun fromWire(value: String?): SignalType =
            entries.firstOrNull { it.wire == value } ?: Unknown
    }
}

/** The result of trying to nudge someone (SignalRepository.nudge). */
enum class NudgeOutcome { Sent, LimitReached, Failed }

/**
 * A Signal — Duren's word for a notification (notifications/{me}/items/{id}).
 *
 * Written client-side by whoever performed the action (no Cloud Functions on the
 * free plan), so each doc carries only [fromUserId]; the actor's name/avatar are
 * hydrated fresh at read time (like [com.duren.app.data.nest.model.NestRequest]).
 * That also means anonymous actions deliberately never create a Signal — there'd be
 * no way to show them without leaking who acted.
 */
data class Signal(
    val id: String = "",
    val type: SignalType = SignalType.Unknown,
    val fromUserId: String = "",
    val emberId: String? = null,
    val preview: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null,
    /** Hydrated for display; null until the actor's profile resolves. */
    val fromProfile: Profile? = null
)
