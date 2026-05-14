package com.runway.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Corner radii mirroring Lovable's --radius scale (base = 1rem ≈ 16dp)
val RunwayShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),    // Filter chips, small badges
    small = RoundedCornerShape(12.dp),         // Icon containers, avatar fallbacks
    medium = RoundedCornerShape(16.dp),        // Default cards, list items
    large = RoundedCornerShape(20.dp),         // Input fields, primary buttons
    extraLarge = RoundedCornerShape(24.dp),    // Stats card, map container, metric grid
)
