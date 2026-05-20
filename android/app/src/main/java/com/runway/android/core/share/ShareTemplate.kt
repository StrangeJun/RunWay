package com.runway.android.core.share

enum class ShareTemplate(
    val displayName: String,
    val bgColor: Long,
    val accentColor: Long,
    val textPrimary: Long,
    val textSecondary: Long,
) {
    DARK_SPORT(
        displayName = "Dark Sport",
        bgColor = 0xFF0D0D0D,
        accentColor = 0xFF39FF14,
        textPrimary = 0xFFFFFFFF,
        textSecondary = 0xFFAAAAAA,
    ),
    MINIMAL_LIGHT(
        displayName = "Minimal",
        bgColor = 0xFFF5F5F5,
        accentColor = 0xFF222222,
        textPrimary = 0xFF111111,
        textSecondary = 0xFF666666,
    ),
    ROUTE_FOCUS(
        displayName = "Route Focus",
        bgColor = 0xFF111827,
        accentColor = 0xFF6366F1,
        textPrimary = 0xFFFFFFFF,
        textSecondary = 0xFF9CA3AF,
    ),
}
