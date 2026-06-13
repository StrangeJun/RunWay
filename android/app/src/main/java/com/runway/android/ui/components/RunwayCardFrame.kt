package com.runway.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.runway.android.ui.theme.LocalIsDarkTheme

@Composable
fun Modifier.runwayCardFrame(shape: Shape): Modifier {
    val accent = MaterialTheme.colorScheme.primary
    val isDark = LocalIsDarkTheme.current
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = if (isDark) 0.72f else 0.48f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.16f else 0.10f),
            accent.copy(alpha = 0.12f),
        ),
    )

    return border(width = 1.dp, brush = borderBrush, shape = shape)
}
