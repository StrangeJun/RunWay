package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.runway.android.data.course.model.NearbyCourseItem

@Composable
fun DiscoverCourseCard(
    course: NearbyCourseItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Column {
            CourseRoutePreview(
                variant = (course.courseId.hashCode() and 0x7FFFFFFF) % 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
            )

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (course.isLoop) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Loop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(end = 2.dp).height(12.dp),
                                )
                                Text(
                                    text = "루프",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(13.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${formatDistance(course.distanceFromMeMeters)} 거리 · ${formatDistance(course.distanceMeters)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "도전 ${course.attemptCount}회 · 완주 ${course.completionCount}회",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

private fun formatDistance(meters: Double): String = when {
    meters >= 1000 -> "%.1f km".format(meters / 1000)
    else -> "${meters.toInt()} m"
}

@Composable
private fun CourseRoutePreview(
    variant: Int,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.background(bgColor)) {
        val w = size.width
        val h = size.height

        val gridColor = primaryColor.copy(alpha = 0.12f)
        val gridStep = 20.dp.toPx()
        var xi = 0f
        while (xi <= w) { drawLine(gridColor, Offset(xi, 0f), Offset(xi, h), 1f); xi += gridStep }
        var yi = 0f
        while (yi <= h) { drawLine(gridColor, Offset(0f, yi), Offset(w, yi), 1f); yi += gridStep }

        val strokePx = 2.5.dp.toPx()
        val dotPx = 5.dp.toPx()
        val ringPx = 2.dp.toPx()
        val path = Path()
        val startPt: Offset
        val endPt: Offset

        when (variant % 3) {
            0 -> {
                startPt = Offset(w * 0.10f, h * 0.72f)
                endPt = Offset(w * 0.88f, h * 0.28f)
                path.moveTo(startPt.x, startPt.y)
                path.cubicTo(w * 0.30f, h * 0.15f, w * 0.62f, h * 0.92f, endPt.x, endPt.y)
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

        drawPath(path, primaryColor, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(primaryColor, dotPx, startPt)
        drawCircle(Color.White, dotPx, endPt)
        drawCircle(color = primaryColor, radius = dotPx - ringPx, center = endPt, style = Stroke(ringPx))
    }
}
