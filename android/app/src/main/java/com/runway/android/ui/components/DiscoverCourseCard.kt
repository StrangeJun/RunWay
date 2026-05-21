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
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.ui.discover.DiscoverViewModel

private val BgTop = Color(0xFF0E1117)
private val BgBottom = Color(0xFF161B26)
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Column {
            Box {
                SportyCourseCanvas(
                    courseId = course.courseId,
                    distanceMeters = course.distanceMeters,
                    isLoop = course.isLoop,
                    accentColor = MaterialTheme.colorScheme.primary,
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
                    color = Color.White.copy(alpha = 0.10f),
                ) {
                    Text(
                        text = formatDistance(course.distanceMeters),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                        ),
                        color = Color.White.copy(alpha = 0.90f),
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
    courseId: String,
    distanceMeters: Double,
    isLoop: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val variant = (courseId.hashCode() and 0x7FFFFFFF) % 4

    Box(modifier = modifier.background(
        Brush.verticalGradient(listOf(BgTop, BgBottom))
    )) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 도트 그리드
            val dotStep = 22.dp.toPx()
            val dotR = 1.dp.toPx()
            var xi = dotStep
            while (xi < w) {
                var yi = dotStep
                while (yi < h) {
                    drawCircle(Color.White.copy(alpha = 0.055f), dotR, Offset(xi, yi))
                    yi += dotStep
                }
                xi += dotStep
            }

            val path = Path()
            val startPt: Offset
            val endPt: Offset

            if (isLoop) {
                // 루프 코스: 타원형 경로
                when (variant % 2) {
                    0 -> {
                        startPt = Offset(w * 0.50f, h * 0.78f)
                        endPt = startPt
                        path.moveTo(startPt.x, startPt.y)
                        path.cubicTo(w * 0.10f, h * 0.78f, w * 0.08f, h * 0.10f, w * 0.50f, h * 0.14f)
                        path.cubicTo(w * 0.92f, h * 0.10f, w * 0.90f, h * 0.78f, startPt.x, startPt.y)
                    }
                    else -> {
                        startPt = Offset(w * 0.22f, h * 0.72f)
                        endPt = startPt
                        path.moveTo(startPt.x, startPt.y)
                        path.cubicTo(w * 0.08f, h * 0.30f, w * 0.38f, h * 0.08f, w * 0.60f, h * 0.18f)
                        path.cubicTo(w * 0.88f, h * 0.30f, w * 0.85f, h * 0.70f, w * 0.65f, h * 0.80f)
                        path.cubicTo(w * 0.48f, h * 0.88f, w * 0.32f, h * 0.82f, startPt.x, startPt.y)
                    }
                }
            } else {
                when (variant) {
                    0 -> {
                        startPt = Offset(w * 0.10f, h * 0.78f)
                        endPt = Offset(w * 0.90f, h * 0.22f)
                        path.moveTo(startPt.x, startPt.y)
                        path.cubicTo(w * 0.22f, h * 0.12f, w * 0.52f, h * 0.92f, w * 0.70f, h * 0.38f)
                        path.cubicTo(w * 0.80f, h * 0.18f, w * 0.88f, h * 0.28f, endPt.x, endPt.y)
                    }
                    1 -> {
                        startPt = Offset(w * 0.08f, h * 0.68f)
                        endPt = Offset(w * 0.92f, h * 0.32f)
                        path.moveTo(startPt.x, startPt.y)
                        path.cubicTo(w * 0.28f, h * 0.08f, w * 0.45f, h * 0.88f, w * 0.62f, h * 0.44f)
                        path.lineTo(w * 0.75f, h * 0.28f)
                        path.cubicTo(w * 0.82f, h * 0.18f, w * 0.88f, h * 0.28f, endPt.x, endPt.y)
                    }
                    2 -> {
                        startPt = Offset(w * 0.08f, h * 0.82f)
                        endPt = Offset(w * 0.92f, h * 0.22f)
                        path.moveTo(startPt.x, startPt.y)
                        path.cubicTo(w * 0.30f, h * 0.80f, w * 0.38f, h * 0.12f, w * 0.58f, h * 0.20f)
                        path.cubicTo(w * 0.72f, h * 0.26f, w * 0.82f, h * 0.52f, endPt.x, endPt.y)
                    }
                    else -> {
                        startPt = Offset(w * 0.12f, h * 0.75f)
                        endPt = Offset(w * 0.88f, h * 0.28f)
                        path.moveTo(startPt.x, startPt.y)
                        path.cubicTo(w * 0.18f, h * 0.18f, w * 0.48f, h * 0.90f, w * 0.68f, h * 0.55f)
                        path.cubicTo(w * 0.78f, h * 0.38f, w * 0.85f, h * 0.22f, endPt.x, endPt.y)
                    }
                }
            }

            // 외곽 글로우 (넓고 흐릿)
            drawPath(path, accentColor.copy(alpha = 0.06f), style = Stroke(32.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, accentColor.copy(alpha = 0.12f), style = Stroke(18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            // 중간 글로우
            drawPath(path, accentColor.copy(alpha = 0.30f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            // 메인 라인
            drawPath(path, accentColor.copy(alpha = 0.95f), style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            // 출발점 (초록)
            drawCircle(StartGreen.copy(alpha = 0.25f), 9.dp.toPx(), startPt)
            drawCircle(StartGreen.copy(alpha = 0.55f), 5.5.dp.toPx(), startPt)
            drawCircle(StartGreen, 3.dp.toPx(), startPt)

            // 도착/반환점 (루프 아니면 주황)
            if (!isLoop) {
                drawCircle(EndOrange.copy(alpha = 0.25f), 9.dp.toPx(), endPt)
                drawCircle(EndOrange.copy(alpha = 0.55f), 5.5.dp.toPx(), endPt)
                drawCircle(EndOrange, 3.dp.toPx(), endPt)
            }
        }
    }
}

private fun formatDistance(meters: Double): String = when {
    meters >= 1000 -> "%.1f km".format(meters / 1000)
    else -> "${meters.toInt()} m"
}
