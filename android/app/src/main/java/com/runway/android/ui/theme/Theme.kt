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
 * User-selectable accent color — drives primaryContainer (the glow/neon accent).
 * Primary (interactive buttons) stays WHITE regardless of accent.
 */
enum class AccentColor(
    val displayName: String,
    val accentColor: Color,
    val onAccentColor: Color,
) {
    GREEN("초록", RunwayGreen, OnRunwayGreen),
    ORANGE("오렌지", AccentOrange, OnAccentOrange),
    ELECTRIC_BLUE("블루", AccentBlue, OnAccentBlue),
    RED("빨강", AccentRed, OnAccentRed),
    YELLOW("노랑", AccentYellow, OnAccentYellow),
    PURPLE("보라", AccentPurple, OnAccentPurple),
}

private fun runwayDarkColorScheme(accent: AccentColor) = darkColorScheme(
    // Primary = WHITE — CTA buttons (login, signup, etc.)
    primary = PrimaryWhite,
    onPrimary = OnPrimaryDark,
    primaryContainer = accent.accentColor,      // Lime green glow/accent
    onPrimaryContainer = accent.onAccentColor,

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
    primary = accent.accentColor,               // Light mode: accent as primary
    onPrimary = accent.onAccentColor,
    primaryContainer = accent.accentColor.copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFF111119),

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
