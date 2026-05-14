package com.runway.android.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Background layers (oklch 250° hue, blue-charcoal family) ───
val BackgroundDark = Color(0xFF111119)        // oklch(0.14 0.01 250) — app background
val SurfaceDark = Color(0xFF1B1B2B)          // oklch(0.18 0.012 250) — card surface
val MutedDark = Color(0xFF222232)            // oklch(0.22 0.012 250) — input / muted bg
val SecondaryContainerDark = Color(0xFF262638) // oklch(0.24 0.015 250) — secondary

// ─── Text ───
val OnSurfaceWhite = Color(0xFFF8F8FC)       // oklch(0.98 0 0) — primary text
val OnSurfaceMuted = Color(0xFF888AA8)       // oklch(0.65 0.015 250) — secondary text

// ─── Border / Outline ───
val BorderColorDark = Color(0xFF2B2B3D)      // oklch(0.26 0.014 250)

// ─── Primary — Neon lime green (oklch 0.85 0.22 hue 130) ───
// Future: refine to exact oklch match if design tooling allows
val RunwayGreen = Color(0xFFA4E168)
val OnRunwayGreen = Color(0xFF111119)        // dark text on green button

// ─── Semantic states ───
val DestructiveRed = Color(0xFFDC5040)       // oklch(0.65 0.22 25)
val SuccessGreen = Color(0xFF7FD98A)         // oklch(0.75 0.18 145)
val WarningYellow = Color(0xFFCCBE50)        // oklch(0.80 0.18 75)
