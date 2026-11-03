package com.fabin.evoca.ui.theme

import com.fabin.evoca.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// Evoca — Font Families
// ─────────────────────────────────────────────

val EvocaSerif = FontFamily(
    Font(
        resId = R.font.dm_serif_display_regular,
        weight = FontWeight.Normal
    )
)

val EvocaSans = FontFamily(
    Font(
        resId = R.font.manrope_wght,
        weight = FontWeight.Normal
    )
)

// ─────────────────────────────────────────────
// Evoca — Typography System
// ─────────────────────────────────────────────

val EvocaTypography = Typography(

    // Brand / major editorial headings
    displayLarge = TextStyle(
        fontFamily = EvocaSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        color = EvocaInk
    ),

    // Screen-level headings
    headlineMedium = TextStyle(
        fontFamily = EvocaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        color = EvocaInk
    ),

    // Empty-state / feature headings
    titleLarge = TextStyle(
        fontFamily = EvocaSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = EvocaInk
    ),

    // Reminder titles
    titleMedium = TextStyle(
        fontFamily = EvocaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = EvocaInk
    ),

    // Normal application text
    bodyLarge = TextStyle(
        fontFamily = EvocaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = EvocaInk
    ),

    bodyMedium = TextStyle(
        fontFamily = EvocaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = EvocaMuted
    ),

    // Dates, sources and other metadata
    bodySmall = TextStyle(
        fontFamily = EvocaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = EvocaMuted
    ),

    // Buttons
    labelLarge = TextStyle(
        fontFamily = EvocaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    labelMedium = TextStyle(
        fontFamily = EvocaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)