package com.runway.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** True when the active theme is dark. Use instead of isSystemInDarkTheme() inside the app. */
val LocalIsDarkTheme = compositionLocalOf { true }

enum class ThemeMode { DARK, LIGHT, SYSTEM }

private val RunwayDarkColorScheme = darkColorScheme(
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = MutedDark,

    primary = RunwayGreen,
    onPrimary = OnRunwayGreen,
    primaryContainer = Color(0xFF1C2E14),
    onPrimaryContainer = RunwayGreen,

    secondary = SecondaryContainerDark,
    onSecondary = OnSurfaceWhite,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSurfaceWhite,

    error = DestructiveRed,
    onError = OnSurfaceWhite,
    errorContainer = Color(0xFF3B1410),
    onErrorContainer = DestructiveRed,

    onBackground = OnSurfaceWhite,
    onSurface = OnSurfaceWhite,
    onSurfaceVariant = OnSurfaceMuted,

    outline = BorderColorDark,
    outlineVariant = BorderColorDark,

    inverseSurface = OnSurfaceWhite,
    inverseOnSurface = BackgroundDark,
    inversePrimary = Color(0xFF2A5C10),
)

private val RunwayLightColorScheme = lightColorScheme(
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = MutedLight,

    primary = RunwayGreen,
    onPrimary = OnRunwayGreen,
    primaryContainer = Color(0xFFD6F5B0),
    onPrimaryContainer = Color(0xFF1A4000),

    secondary = SecondaryContainerLight,
    onSecondary = OnSurfaceDark,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSurfaceDark,

    error = DestructiveRed,
    onError = OnSurfaceWhite,
    errorContainer = Color(0xFFFFEDEB),
    onErrorContainer = DestructiveRed,

    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceMutedLight,

    outline = BorderColorLight,
    outlineVariant = BorderColorLight,

    inverseSurface = OnSurfaceDark,
    inverseOnSurface = BackgroundLight,
    inversePrimary = Color(0xFF2A5C10),
)

@Composable
fun RunwayTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = if (isDark) RunwayDarkColorScheme else RunwayLightColorScheme,
            typography = RunwayTypography,
            shapes = RunwayShapes,
            content = content,
        )
    }
}
