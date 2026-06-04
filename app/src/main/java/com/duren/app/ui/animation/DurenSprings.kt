package com.duren.app.ui.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Animation Bible v2.1 Part 1 — default spring specs.
 * Spring physics on every interaction. No CSS-style linear easing for interactions.
 */
object DurenSprings {
    val Soft = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    val Medium = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val Snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val Bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessLow
    )
}
