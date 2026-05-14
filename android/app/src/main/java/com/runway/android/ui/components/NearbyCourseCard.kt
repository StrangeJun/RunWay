package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset

data class NearbyCourse(
    val name: String,
    val distanceAway: String,
    val lengthKm: String,
    val routeVariant: Int,
)

@Composable
fun NearbyCourseCard(
    course: NearbyCourse,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(190.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column {
            RoutePreview(
                variant = course.routeVariant,
                modifier = Modifier
                    .height(88.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${course.distanceAway} away · ${course.lengthKm}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutePreview(
    variant: Int,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .background(bgColor),
    ) {
        val w = size.width
        val h = size.height

        // Subtle grid
        val gridColor = primaryColor.copy(alpha = 0.12f)
        val gridStep = 20.dp.toPx()
        var xi = 0f
        while (xi <= w) {
            drawLine(gridColor, Offset(xi, 0f), Offset(xi, h), 1f)
            xi += gridStep
        }
        var yi = 0f
        while (yi <= h) {
            drawLine(gridColor, Offset(0f, yi), Offset(w, yi), 1f)
            yi += gridStep
        }

        val strokePx = 2.5.dp.toPx()
        val dotPx = 5.dp.toPx()
        val ringPx = 2.dp.toPx()
        val path = Path()

        val startPt: Offset
        val endPt: Offset

        when (variant % 3) {
            0 -> {
                startPt = Offset(w * 0.1f, h * 0.72f)
                endPt = Offset(w * 0.88f, h * 0.28f)
                path.moveTo(startPt.x, startPt.y)
                path.cubicTo(w * 0.3f, h * 0.15f, w * 0.62f, h * 0.92f, endPt.x, endPt.y)
            }
            1 -> {
                startPt = Offset(w * 0.12f, h * 0.55f)
                endPt = Offset(w * 0.85f, h * 0.48f)
                path.moveTo(startPt.x, startPt.y)
                path.cubicTo(w * 0.35f, h * 0.08f, w * 0.62f, h * 0.92f, endPt.x, endPt.y)
            }
            else -> {
                startPt = Offset(w * 0.06f, h * 0.65f)
                endPt = Offset(w * 0.94f, h * 0.35f)
                path.moveTo(startPt.x, startPt.y)
                path.cubicTo(w * 0.28f, h * 0.08f, w * 0.5f, h * 0.92f, w * 0.72f, h * 0.2f)
                path.cubicTo(w * 0.82f, h * 0.08f, w * 0.9f, h * 0.55f, endPt.x, endPt.y)
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        // Start dot
        drawCircle(primaryColor, dotPx, startPt)
        // End dot (ring)
        drawCircle(Color.White, dotPx, endPt)
        drawCircle(
            color = primaryColor,
            radius = dotPx - ringPx,
            center = endPt,
            style = Stroke(ringPx),
        )
    }
}
