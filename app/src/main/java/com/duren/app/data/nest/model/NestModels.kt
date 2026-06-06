package com.duren.app.data.nest.model

import com.duren.app.data.profile.model.Profile
import com.google.firebase.Timestamp

/** Where the current user stands relative to another user, for the Nest button. */
enum class NestRelation {
    /** It's your own profile. */
    Self,

    /** No request either way, not in the Nest. */
    None,

    /** You sent a request that's still pending. */
    OutgoingPending,

    /** They sent you a request that's still pending. */
    IncomingPending,

    /** You're in each other's Nest. */
    Member
}

/** A pending request to join someone's Nest (nestRequests/{fromUid_toUid}). */
data class NestRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val createdAt: Timestamp? = null,
    /** Hydrated for display in the incoming-requests list; null until resolved. */
    val fromProfile: Profile? = null
)
