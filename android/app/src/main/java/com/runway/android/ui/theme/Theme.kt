package com.runway.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = compositionLocalOf { true }

enum class ThemeMode { DARK, LIGHT, SYSTEM }

/**
 * User-selectable accent color — drives primary (buttons, active states, highlights, text accents).
 * primaryContainer is a darker tint of the accent for chip/badge backgrounds.
 */
enum class AccentColor(
    val displayName: String,
    val accentColor: Color,
    val onAccentColor: Color,
    val darkContainer: Color,
) {
    GREEN("초록", RunwayGreen, OnRunwayGreen, Color(0xFF1C2E14)),
    ORANGE("오렌지", AccentOrange, OnAccentOrange, Color(0xFF3D1A0A)),
    ELECTRIC_BLUE("블루", AccentBlue, OnAccentBlue, Color(0xFF0A1E3D)),
    RED("빨강", AccentRed, OnAccentRed, Color(0xFF3D1B20)),
    YELLOW("노랑", AccentYellow, OnAccentYellow, Color(0xFF342D12)),
    PURPLE("보라", AccentPurple, OnAccentPurple, Color(0xFF2D1D3D)),
}

private fun runwayDarkColorScheme(accent: AccentColor) = darkColorScheme(
    // Primary = accent color — drives highlights, active nav, focused borders, metric accents
    primary = accent.accentColor,
    onPrimary = accent.onAccentColor,
    primaryContainer = accent.darkContainer,
    onPrimaryContainer = accent.accentColor,

    secondary = SurfaceContainerDark,           // Card / elevated surface
    onSecondary = OnSurfaceDark,
    secondaryContainer = SurfaceContainerHighDark,
    onSecondaryContainer = OnSurfaceDark,

    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = MutedDark,

    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceMuted,

    outline = BorderColorDark,
    outlineVariant = OutlineVariantDark,

    error = DestructiveRed,
    onError = OnSurfaceDark,
    errorContainer = Color(0xFF3B1410),
    onErrorContainer = DestructiveRed,

    inverseSurface = OnSurfaceDark,
    inverseOnSurface = BackgroundDark,
    inversePrimary = accent.accentColor,
)

private fun runwayLightColorScheme(accent: AccentColor) = lightColorScheme(
    primary = accent.accentColor,
    onPrimary = accent.onAccentColor,
    primaryContainer = accent.darkContainer,
    onPrimaryContainer = accent.accentColor,

    secondary = Color(0xFFE8E8F0),
    onSecondary = Color(0xFF111119),
    secondaryContainer = Color(0xFFEEEEF4),
    onSecondaryContainer = Color(0xFF111119),

    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = MutedLight,

    onBackground = OnSurfaceDarkLight,
    onSurface = OnSurfaceDarkLight,
    onSurfaceVariant = OnSurfaceMutedLight,

    outline = BorderColorLight,
    outlineVariant = BorderColorLight,

    error = DestructiveRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFEDEB),
    onErrorContainer = DestructiveRed,

    inverseSurface = OnSurfaceDarkLight,
    inverseOnSurface = BackgroundLight,
    inversePrimary = accent.accentColor,
)

@Composable
fun RunwayTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColor = AccentColor.GREEN,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = if (isDark) runwayDarkColorScheme(accentColor)
                          else runwayLightColorScheme(accentColor),
            typography = RunwayTypography,
            shapes = RunwayShapes,
            content = content,
        )
    }
}
