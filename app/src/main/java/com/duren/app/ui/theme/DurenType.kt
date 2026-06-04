package com.duren.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Duren type scale (Design System v2.0 Part 3).
 *
 * Phase 0: uses FontFamily.Default. General Sans / Inter / JetBrains Mono fonts
 * land in Phase 2 with the rest of the brand application.
 */
object DurenType {
    private val displayFamily = FontFamily.Default
    private val bodyFamily = FontFamily.Default
    private val monoFamily = FontFamily.Monospace

    val DisplayLarge = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    )
    val DisplayMedium = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp
    )
    val DisplaySmall = TextStyle(
        fontFamily = displayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    )
    val TitleLarge = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )
    val TitleMedium = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
    val BodyLarge = TextStyle(
        fontFamily = bodyFamily,
        fontSize = 16.sp,
        lineHeight = 26.sp
    )
    val BodyMedium = TextStyle(
        fontFamily = bodyFamily,
        fontSize = 14.sp,
        lineHeight = 22.sp
    )
    val LabelLarge = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
    val LabelSmall = TextStyle(
        fontFamily = bodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    val Caption = TextStyle(
        fontFamily = bodyFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
    val Timestamp = TextStyle(
        fontFamily = monoFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
}
