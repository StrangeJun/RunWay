package com.runway.android.ui.components

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * GPS route visualization placeholder using Compose Canvas.
 * Draws a bezier route path with an animated current-position dot when [isAnimated].
 */
@Composable
fun RouteMapPlaceholder(
    modifier: Modifier = Modifier,
    isAnimated: Boolean = false,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    val infiniteTransition = rememberInfiniteTransition(label = "gps_pulse")

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.8f,
        animationSpec = InfiniteRepeatableSpec(tween(900), repeatMode = RepeatMode.Restart),
        label = "pulse_radius",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = InfiniteRepeatableSpec(tween(900), repeatMode = RepeatMode.Restart),
        label = "pulse_alpha",
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        val w = size.width
        val h = size.height

        // ─── Grid ───
        val gridColor = primaryColor.copy(alpha = 0.1f)
        val step = 24.dp.toPx()
        var xi = 0f
        while (xi <= w) {
            drawLine(gridColor, Offset(xi, 0f), Offset(xi, h), strokeWidth = 1f)
            xi += step
        }
        var yi = 0f
        while (yi <= h) {
            drawLine(gridColor, Offset(0f, yi), Offset(w, yi), strokeWidth = 1f)
            yi += step
        }

        // ─── Route path control points ───
        val startPt = Offset(w * 0.08f, h * 0.78f)
        val midPt = Offset(w * 0.72f, h * 0.24f)
        val endPt = Offset(w * 0.88f, h * 0.58f)

        // Full dim route (planned)
        val fullPath = Path().apply {
            moveTo(startPt.x, startPt.y)
            cubicTo(w * 0.20f, h * 0.08f, w * 0.50f, h * 0.92f, midPt.x, midPt.y)
            cubicTo(w * 0.84f, h * 0.06f, w * 0.92f, h * 0.44f, endPt.x, endPt.y)
        }
        drawPath(
            fullPath,
            color = primaryColor.copy(alpha = 0.30f),
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Completed portion (first segment, bright)
        val completedPath = Path().apply {
            moveTo(startPt.x, startPt.y)
            cubicTo(w * 0.20f, h * 0.08f, w * 0.50f, h * 0.92f, midPt.x, midPt.y)
        }
        drawPath(
            completedPath,
            color = primaryColor,
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Start dot
        drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = startPt)

        // End dot (future, dim ring)
        drawCircle(
            color = primaryColor.copy(alpha = 0.35f),
            radius = 5.dp.toPx(),
            center = endPt,
            style = Stroke(2.dp.toPx()),
        )

        // Current position (midPt = ~50% progress simulation)
        if (isAnimated) {
            drawCircle(
                color = primaryColor.copy(alpha = pulseAlpha),
                radius = 6.dp.toPx() * pulseRadius,
                center = midPt,
            )
        }
        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = midPt)
        drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = midPt)
    }
}
