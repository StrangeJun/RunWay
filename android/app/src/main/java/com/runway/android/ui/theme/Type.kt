package com.runway.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.runway.android.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val notoSansKr = GoogleFont("Noto Sans KR")
private val jetbrainsMono = GoogleFont("JetBrains Mono")

val DisplayFontFamily = FontFamily(
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Bold),
)

val MonoFontFamily = FontFamily(
    Font(googleFont = jetbrainsMono, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = jetbrainsMono, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = jetbrainsMono, fontProvider = fontProvider, weight = FontWeight.Bold),
)

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
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
        lineHeight = 36.sp,
    ),
    // Card titles (Riverside Loop), sub-screen headers
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.3).sp,
        lineHeight = 30.sp,
    ),
    // Section headers, smaller screen titles
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    // Metric values in tracking grid (5'12", 11.5)
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    // List item titles, card names
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    // Smaller item titles, sub-labels
    titleSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    // Body copy, descriptions
    bodyLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    // Secondary body, metadata rows
    bodyMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    // Tertiary body, helper text
    bodySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    // Caption labels — "RECORDING", "GPS · Strong"
    labelLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 18.sp,
    ),
    // Small metric captions — "거리", "페이스"
    labelMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp,
        lineHeight = 16.sp,
    ),
    // Tiny labels — units (km, /km)
    labelSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.2.sp,
        lineHeight = 15.sp,
    ),
)
