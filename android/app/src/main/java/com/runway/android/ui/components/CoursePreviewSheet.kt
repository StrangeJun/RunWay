package com.runway.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runway.android.core.map.MapPoint
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.data.course.model.GeoPoint
import com.runway.android.ui.theme.SurfaceContainerDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursePreviewSheet(
    course: CourseResponse,
    routePoints: List<GeoPoint>,
    bestTimeSeconds: Int?,
    avgRating: Double?,
    ratingCount: Long?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onViewMap: (String) -> Unit,
    onStartAttempt: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mapPoints = routePoints.map { MapPoint(it.latitude, it.longitude) }
    val primary = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceContainerDark,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // ── 지도 영역 (상단 전체폭, 라운딩 없음) ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            ) {
                if (mapPoints.size >= 2) {
                    RouteMapView(
                        points = mapPoints,
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        gesturesEnabled = false,
                        onClick = { onViewMap(course.courseId) },
                    )
                } else {
                    // 포인트 로딩 중 플레이스홀더
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = primary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }

                // 지도 아래에서 올라오는 그라데이션 페이드
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, SurfaceContainerDark),
                            ),
                        ),
                )

                // 자세히 보기 배지 — 지도 우하단
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = MaterialTheme.shapes.large,
                    color = SurfaceContainerDark.copy(alpha = 0.92f),
                    onClick = { onViewMap(course.courseId) },
                ) {
                    Text(
                        text = "자세히 보기",
                        style = MaterialTheme.typography.labelMedium,
                        color = primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }

                // 루프 배지 — 지도 우상단
                if (course.isLoop) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = primary.copy(alpha = 0.88f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Filled.Loop, null, Modifier.size(12.dp), tint = Color.White)
                            Text("루프", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }

            // ── 정보 영역 ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 24.dp),
            ) {
                // 코스 이름
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (!course.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 스탯 카드 — 단일 컨테이너로 통합
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .runwayCardFrame(MaterialTheme.shapes.extraLarge),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatCell(
                            icon = Icons.Filled.Route,
                            label = "거리",
                            value = formatDistance(course.distanceMeters),
                            modifier = Modifier.weight(1f),
                        )
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        )
                        StatCell(
                            icon = Icons.Filled.EmojiEvents,
                            label = "내 최고기록",
                            value = if (bestTimeSeconds != null) formatDurationShort(bestTimeSeconds)
                                    else if (isLoading) "…" else "--",
                            modifier = Modifier.weight(1f),
                        )
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        )
                        StatCell(
                            icon = Icons.Filled.Star,
                            label = "평점",
                            value = if (avgRating != null && (ratingCount ?: 0L) > 0)
                                        "%.1f".format(avgRating)
                                    else if (isLoading) "…" else "--",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 바로 도전하기
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .runwayCardFrame(MaterialTheme.shapes.extraLarge),
                    onClick = { onStartAttempt(course.courseId) },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = primary,
                ) {
                    Text(
                        text = "코스 도전하기",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCell(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000.0) else "${meters.toInt()} m"

private fun formatDurationShort(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
