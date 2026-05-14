package com.runway.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Phase B-1: Using system defaults.
// Replace with Space Grotesk when custom fonts are added in a later phase:
//   val DisplayFontFamily = FontFamily(Font(R.font.space_grotesk_regular), ...)
val DisplayFontFamily: FontFamily = FontFamily.Default

// Replace with JetBrains Mono for numeric / monospaced fields:
//   val MonoFontFamily = FontFamily(Font(R.font.jetbrains_mono_regular), ...)
val MonoFontFamily: FontFamily = FontFamily.Monospace

val RunwayTypography = Typography(
    // Hero numbers — timer (28:14), distance (5.42 km)
    displayLarge = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        letterSpacing = (-2).sp,
        lineHeight = 68.sp,
    ),
    // Large section number — weekly distance (24.6 km)
    displayMedium = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = (-1).sp,
    ),
    // Screen titles (Discover courses, Leaderboard)
    headlineLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
    ),
    // Card titles (Riverside Loop), sub-screen headers
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp,
    ),
    // Metric values in tracking grid (5'12", 11.5)
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    // List item titles, card names
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    ),
    // Body copy, descriptions
    bodyLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Secondary body, metadata rows
    bodyMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    // Mono labels — "RECORDING", "GPS · Strong", leaderboard times
    labelLarge = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
    ),
    // Small metric captions — "DISTANCE", "PACE"
    labelMedium = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 1.sp,
    ),
    // Tiny labels — units (km, /km, bpm)
    labelSmall = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        letterSpacing = 0.5.sp,
    ),
)
