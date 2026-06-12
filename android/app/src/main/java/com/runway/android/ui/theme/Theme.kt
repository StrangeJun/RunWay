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

enum class AccentColor(
    val displayName: String,
    val color: Color,
    val onColor: Color,
    val darkContainer: Color,
    val lightContainer: Color,
) {
    GREEN("초록", RunwayGreen, OnRunwayGreen, Color(0xFF1C2E14), Color(0xFFD6F5B0)),
    WHITE("화이트", RunwayWhite, OnRunwayWhite, Color(0xFF1E1E1E), Color(0xFFF0F0F0)),
    ENERGY_ORANGE("오렌지", RunwayEnergyOrange, OnRunwayOrange, Color(0xFF3D1A0A), Color(0xFFFFD5C2)),
    ELECTRIC_BLUE("블루", RunwayElectricBlue, OnRunwayBlue, Color(0xFF0A1E3D), Color(0xFFC2DCFF)),
    RED("빨강", RunwayRed, OnSurfaceDark, Color(0xFF3D1B20), Color(0xFFFFDADA)),
    ORANGE("주황", RunwayOrange, OnSurfaceDark, Color(0xFF3B2414), Color(0xFFFFDCC2)),
    YELLOW("노랑", RunwayYellow, OnSurfaceDark, Color(0xFF342D12), Color(0xFFFFEFA8)),
    BLUE("파랑", RunwayBlue, OnSurfaceDark, Color(0xFF142B3D), Color(0xFFCDE8FF)),
    PURPLE("보라", RunwayPurple, OnSurfaceDark, Color(0xFF2D1D3D), Color(0xFFEBD8FF)),
}

private fun runwayDarkColorScheme(accent: AccentColor) = darkColorScheme(
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = MutedDark,

    primary = accent.color,
    onPrimary = accent.onColor,
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
    onPrimary = accent.onColor,
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
            colorScheme = if (isDark) runwayDarkColorScheme(accentColor)
                          else runwayLightColorScheme(accentColor),
            typography = RunwayTypography,
            shapes = RunwayShapes,
            content = content,
        )
    }
}
