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

enum class AccentColor(
    val displayName: String,
    val color: Color,
    val darkContainer: Color,
    val lightContainer: Color,
) {
    RED("빨강", RunwayRed, Color(0xFF3D1B20), Color(0xFFFFDADA)),
    ORANGE("주황", RunwayOrange, Color(0xFF3B2414), Color(0xFFFFDCC2)),
    YELLOW("노랑", RunwayYellow, Color(0xFF342D12), Color(0xFFFFEFA8)),
    GREEN("초록", RunwayGreen, Color(0xFF1C2E14), Color(0xFFD6F5B0)),
    BLUE("파랑", RunwayBlue, Color(0xFF142B3D), Color(0xFFCDE8FF)),
    PURPLE("보라", RunwayPurple, Color(0xFF2D1D3D), Color(0xFFEBD8FF)),
}

private fun runwayDarkColorScheme(accent: AccentColor) = darkColorScheme(
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = MutedDark,

    primary = accent.color,
    onPrimary = OnRunwayGreen,
    primaryContainer = accent.darkContainer,
    onPrimaryContainer = accent.color,

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
    inversePrimary = accent.color,
)

private fun runwayLightColorScheme(accent: AccentColor) = lightColorScheme(
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = MutedLight,

    primary = accent.color,
    onPrimary = OnRunwayGreen,
    primaryContainer = accent.lightContainer,
    onPrimaryContainer = OnSurfaceDark,

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
    inversePrimary = accent.color,
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
            colorScheme = if (isDark) {
                runwayDarkColorScheme(accentColor)
            } else {
                runwayLightColorScheme(accentColor)
            },
            typography = RunwayTypography,
            shapes = RunwayShapes,
            content = content,
        )
    }
}
