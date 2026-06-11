package com.duren.app.data.testimonial.model

import com.google.firebase.Timestamp

/**
 * A Testimonial (testimonials/{userId}/items/{id}) — a 150-char public note a Nest
 * member leaves on your presence. Lives 30 days, then fades like everything here
 * (Feature 27).
 */
data class Testimonial(
    val id: String = "",
    val text: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)
