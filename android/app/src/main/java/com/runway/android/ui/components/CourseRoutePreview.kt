package com.runway.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.runway.android.data.course.model.CoursePointResponse

/**
 * Renders actual GPS course points onto a Canvas.
 * Falls back to a decorative placeholder when [points] is empty.
 */
@Composable
fun CourseRoutePreview(
    points: List<CoursePointResponse>,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.background(bgColor)) {
        val w = size.width
        val h = size.height
        val paddingPx = w * 0.10f

        val gridColor = primaryColor.copy(alpha = 0.10f)
        val gridStep = 24.dp.toPx()
        var xi = 0f
        while (xi <= w) { drawLine(gridColor, Offset(xi, 0f), Offset(xi, h), 1f); xi += gridStep }
        var yi = 0f
        while (yi <= h) { drawLine(gridColor, Offset(0f, yi), Offset(w, yi), 1f); yi += gridStep }

        val strokePx = 2.5.dp.toPx()
        val dotPx = 5.dp.toPx()

        if (points.size >= 2) {
            val minLat = points.minOf { it.latitude }
            val maxLat = points.maxOf { it.latitude }
            val minLon = points.minOf { it.longitude }
            val maxLon = points.maxOf { it.longitude }
            val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
            val lonRange = (maxLon - minLon).coerceAtLeast(0.0001)

            val drawW = w - 2 * paddingPx
            val drawH = h - 2 * paddingPx

            fun toOffset(p: CoursePointResponse) = Offset(
                x = paddingPx + ((p.longitude - minLon) / lonRange * drawW).toFloat(),
                y = paddingPx + ((1.0 - (p.latitude - minLat) / latRange) * drawH).toFloat(),
            )

            val offsets = points.map { toOffset(it) }
            val path = Path().apply {
                moveTo(offsets[0].x, offsets[0].y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }

            drawPath(path, primaryColor, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(primaryColor, dotPx, offsets.first())
            drawCircle(Color.White, dotPx, offsets.last())
            drawCircle(primaryColor, dotPx - 2.dp.toPx(), offsets.last(), style = Stroke(2.dp.toPx()))
        } else {
            // Placeholder path when no points available
            val startPt = Offset(w * 0.10f, h * 0.72f)
            val endPt = Offset(w * 0.88f, h * 0.28f)
            val path = Path().apply {
                moveTo(startPt.x, startPt.y)
                cubicTo(w * 0.30f, h * 0.15f, w * 0.62f, h * 0.92f, endPt.x, endPt.y)
            }
            drawPath(path, primaryColor.copy(alpha = 0.4f), style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(primaryColor.copy(alpha = 0.4f), dotPx, startPt)
            drawCircle(primaryColor.copy(alpha = 0.4f), dotPx, endPt, style = Stroke(2.dp.toPx()))
        }
    }
}
