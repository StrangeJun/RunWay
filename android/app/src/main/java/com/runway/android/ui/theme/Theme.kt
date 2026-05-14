package com.runway.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// RunWay is a permanently dark app — no light scheme.
private val RunwayColorScheme = darkColorScheme(
    // ─── Backgrounds ───
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = MutedDark,

    // ─── Primary (neon lime green) ───
    primary = RunwayGreen,
    onPrimary = OnRunwayGreen,
    primaryContainer = Color(0xFF1C2E14),     // subtle tinted bg for primary-tinted surfaces
    onPrimaryContainer = RunwayGreen,

    // ─── Secondary (dark container tones) ───
    secondary = SecondaryContainerDark,
    onSecondary = OnSurfaceWhite,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSurfaceWhite,

    // ─── Error / Destructive ───
    error = DestructiveRed,
    onError = OnSurfaceWhite,
    errorContainer = Color(0xFF3B1410),
    onErrorContainer = DestructiveRed,

    // ─── Text ───
    onBackground = OnSurfaceWhite,
    onSurface = OnSurfaceWhite,
    onSurfaceVariant = OnSurfaceMuted,

    // ─── Borders ───
    outline = BorderColorDark,
    outlineVariant = BorderColorDark,

    // ─── Inverse (not heavily used in dark-only app) ───
    inverseSurface = OnSurfaceWhite,
    inverseOnSurface = BackgroundDark,
    inversePrimary = Color(0xFF2A5C10),
)

@Composable
fun RunwayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RunwayColorScheme,
        typography = RunwayTypography,
        shapes = RunwayShapes,
        content = content,
    )
}
