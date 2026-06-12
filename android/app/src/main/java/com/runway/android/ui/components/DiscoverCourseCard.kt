package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runway.android.data.course.model.GeoPoint
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.ui.discover.DiscoverViewModel
import com.runway.android.ui.theme.GlassBorderDark
import com.runway.android.ui.theme.GlassSurfaceDark
import com.runway.android.ui.theme.LocalIsDarkTheme

private val DarkBgTop = Color(0xFF0A0A0A)
private val DarkBgBottom = Color(0xFF141414)
private val LightBgTop = Color(0xFFE4EAF2)
private val LightBgBottom = Color(0xFFEDF2F8)
private val StartGreen = Color(0xFF4ADE80)
private val EndOrange = Color(0xFFFB923C)

@Composable
fun DiscoverCourseCard(
    course: NearbyCourseItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val isPopular = DiscoverViewModel.isPopular(course)
    val isNew = DiscoverViewModel.isNew(course)
    val completionRate = DiscoverViewModel.completionRate(course)
    val isDark = LocalIsDarkTheme.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isDark) GlassSurfaceDark else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isDark) GlassBorderDark else MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Column {
            Box {
                SportyCourseCanvas(
                    routePoints = course.routePoints,
                    courseId = course.courseId,
                    accentColor = MaterialTheme.colorScheme.primary,
                    isDark = isDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                )
                // 거리 배지 — 좌상단
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.10f)
                            else Color.Black.copy(alpha = 0.07f),
                ) {
                    Text(
                        text = formatDistance(course.distanceMeters),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (isDark) Color.White.copy(alpha = 0.90f)
                                else Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
                // 품질 배지 — 우상단
                if (isPopular || isNew) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPopular) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.secondary,
                    ) {
                        Text(
                            text = if (isPopular) "🔥 인기" else "🆕 신규",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPopular) MaterialTheme.colorScheme.onTertiary
                            else MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

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
                                    modifier = Modifier
                                        .padding(end = 2.dp)
                                        .height(12.dp),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val rateText = if (course.attemptCount > 0)
                        " · 완주율 ${(completionRate * 100).toInt()}%"
                    else ""
                    Text(
                        text = "도전 ${course.attemptCount}회 · 완주 ${course.completionCount}회$rateText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                    )
                    if (course.avgRating != null && course.ratingCount != null && course.ratingCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "%.1f".format(course.avgRating),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "(${course.ratingCount})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SportyCourseCanvas(
    routePoints: List<GeoPoint>,
    courseId: String,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val bgColors = if (isDark) listOf(DarkBgTop, DarkBgBottom)
                   else listOf(LightBgTop, LightBgBottom)
    Box(modifier = modifier.background(Brush.verticalGradient(bgColors))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pad = 16.dp.toPx()

            // 도트 그리드
            val dotStep = 22.dp.toPx()
            val dotR = 1.dp.toPx()
            val dotColor = if (isDark) Color.White.copy(alpha = 0.055f)
                           else Color.Black.copy(alpha = 0.06f)
            var xi = dotStep
            while (xi < w) {
                var yi = dotStep
                while (yi < h) {
                    drawCircle(dotColor, dotR, Offset(xi, yi))
                    yi += dotStep
                }
                xi += dotStep
            }

            if (routePoints.size < 2) return@Canvas

            // GPS 좌표 → 캔버스 좌표 투영 (종횡비 유지, 중앙 정렬)
            val minLat = routePoints.minOf { it.latitude }
            val maxLat = routePoints.maxOf { it.latitude }
            val minLon = routePoints.minOf { it.longitude }
            val maxLon = routePoints.maxOf { it.longitude }
            val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
            val lonSpan = (maxLon - minLon).coerceAtLeast(0.0001)

            val drawW = w - pad * 2
            val drawH = h - pad * 2
            val scaleByLat = drawH / latSpan
            val scaleByLon = drawW / lonSpan
            val scale = minOf(scaleByLat, scaleByLon)

            val projW = lonSpan * scale
            val projH = latSpan * scale
            val offsetX = pad + (drawW - projW) / 2f
            val offsetY = pad + (drawH - projH) / 2f

            fun project(pt: com.runway.android.data.course.model.GeoPoint) = Offset(
                x = (offsetX + (pt.longitude - minLon) * scale).toFloat(),
                y = (offsetY + (maxLat - pt.latitude) * scale).toFloat(),
            )

            val offsets = routePoints.map { project(it) }

            val path = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }

            // 글로우 레이어
            drawPath(path, accentColor.copy(alpha = 0.07f), style = Stroke(28.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, accentColor.copy(alpha = 0.18f), style = Stroke(12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, accentColor.copy(alpha = 0.40f), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            // 메인 라인
            drawPath(path, accentColor.copy(alpha = 0.95f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            // 출발점 (초록)
            val start = offsets.first()
            drawCircle(StartGreen.copy(alpha = 0.25f), 9.dp.toPx(), start)
            drawCircle(StartGreen.copy(alpha = 0.60f), 5.dp.toPx(), start)
            drawCircle(StartGreen, 3.dp.toPx(), start)

            // 도착점 (주황)
            val end = offsets.last()
            if (end != start) {
                drawCircle(EndOrange.copy(alpha = 0.25f), 9.dp.toPx(), end)
                drawCircle(EndOrange.copy(alpha = 0.60f), 5.dp.toPx(), end)
                drawCircle(EndOrange, 3.dp.toPx(), end)
            }
        }
    }
}

private fun formatDistance(meters: Double): String = when {
    meters >= 1000 -> "%.1f km".format(meters / 1000)
    else -> "${meters.toInt()} m"
}
