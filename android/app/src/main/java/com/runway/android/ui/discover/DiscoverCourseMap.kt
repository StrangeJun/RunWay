package com.runway.android.ui.discover

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.runway.android.R
import com.runway.android.core.map.MapPoint
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.pow

// 화면에서 이 픽셀 이내의 마커를 하나의 클러스터로 묶는다
private const val CLUSTER_THRESHOLD_PX = 72.0

private data class CourseCluster(
    val id: String,
    val courses: List<NearbyCourseItem>,
    val center: LatLng,
)

private fun clusterCourses(courses: List<NearbyCourseItem>, zoom: Float): List<CourseCluster> {
    if (courses.isEmpty()) return emptyList()
    val pixelsPerDeg = 256.0 * 2.0.pow(zoom.toDouble()) / 360.0
    val cellDeg = CLUSTER_THRESHOLD_PX / pixelsPerDeg
    val grid = LinkedHashMap<String, MutableList<NearbyCourseItem>>()
    courses.forEach { course ->
        val key = "${floor(course.startPoint.latitude / cellDeg).toLong()}," +
            "${floor(course.startPoint.longitude / cellDeg).toLong()}"
        grid.getOrPut(key) { mutableListOf() }.add(course)
    }
    return grid.entries.map { (key, list) ->
        CourseCluster(
            id = key,
            courses = list,
            center = LatLng(
                list.sumOf { it.startPoint.latitude } / list.size,
                list.sumOf { it.startPoint.longitude } / list.size,
            ),
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DiscoverCourseMap(
    courses: List<NearbyCourseItem>,
    isLoading: Boolean,
    errorMessage: String?,
    currentLocation: MapPoint?,
    onCourseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val mapStyleOptions = remember(isDark) {
        val styleRes = if (isDark) R.raw.map_style_discover_dark else R.raw.map_style_light
        runCatching { MapStyleOptions.loadRawResourceStyle(context, styleRes) }.getOrNull()
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(KOREA_CENTER, 6.55f)
    }
    val coroutineScope = rememberCoroutineScope()

    // 단일 코스 선택 상태 (기존 말풍선)
    var selectedSingle by remember { mutableStateOf<NearbyCourseItem?>(null) }
    // 클러스터 선택 상태 → 하단 패널
    var selectedCluster by remember { mutableStateOf<CourseCluster?>(null) }

    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val surfaceArgb = MaterialTheme.colorScheme.surface.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary

    val markerIcons = remember(primaryArgb, surfaceArgb) {
        SportMarkerIcons(
            normal = createSportMarkerIcon(context, primaryArgb, surfaceArgb, selected = false),
            selected = createSportMarkerIcon(context, primaryArgb, surfaceArgb, selected = true),
        )
    }

    // 줌 레벨 변화 감지
    val zoom by remember { derivedStateOf { cameraPositionState.position.zoom } }

    // 줌 레벨에 따라 클러스터 재계산
    val clusters by remember(courses, zoom) {
        derivedStateOf { clusterCourses(courses, zoom) }
    }

    // 카메라 이동·줌 시 클러스터 패널 닫기
    LaunchedEffect(zoom) {
        selectedCluster = null
        selectedSingle = null
    }

    LaunchedEffect(Unit) {
        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(KOREA_CENTER, 6.55f))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = currentLocation != null,
                mapStyleOptions = mapStyleOptions,
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
                rotationGesturesEnabled = true,
                tiltGesturesEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
            ),
            onMapClick = {
                selectedSingle = null
                selectedCluster = null
            },
        ) {
            clusters.forEach { cluster ->
                if (cluster.courses.size == 1) {
                    // ── 단일 마커: 기존 말풍선 ──────────────────────────
                    val course = cluster.courses.first()
                    val selected = course.courseId == selectedSingle?.courseId
                    val state = remember(course.courseId) {
                        MarkerState(position = cluster.center)
                    }
                    MarkerInfoWindow(
                        state = state,
                        anchor = Offset(0.5f, 0.92f),
                        infoWindowAnchor = Offset(0.5f, 0.05f),
                        icon = if (selected) markerIcons.selected else markerIcons.normal,
                        zIndex = if (selected) 2f else 1f,
                        onClick = { marker ->
                            selectedSingle = course
                            selectedCluster = null
                            marker.showInfoWindow()
                            true
                        },
                        onInfoWindowClick = { onCourseClick(course.courseId) },
                    ) {
                        CourseInfoBubble(course = course)
                    }
                } else {
                    // ── 클러스터 마커: 숫자 뱃지 ───────────────────────
                    val isSelected = cluster.id == selectedCluster?.id
                    val icon = remember(primaryArgb, cluster.courses.size, isSelected) {
                        createClusterIcon(context, primaryArgb, cluster.courses.size, isSelected)
                    }
                    val state = remember(cluster.id) {
                        MarkerState(position = cluster.center)
                    }
                    Marker(
                        state = state,
                        anchor = Offset(0.5f, 0.5f),
                        icon = icon,
                        zIndex = if (isSelected) 3f else 2f,
                        onClick = {
                            selectedCluster = if (isSelected) null else cluster
                            selectedSingle = null
                            true
                        },
                    )
                }
            }
        }

        // ── 코스 개수 칩 ──────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = primaryColor,
                )
                Text(
                    text = "전국 ${courses.size}개 코스",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // ── 내 위치 버튼 ──────────────────────────────────────────────
        if (currentLocation != null) {
            Surface(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(currentLocation.latitude, currentLocation.longitude),
                                15f,
                            ),
                            durationMs = 700,
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 4.dp,
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "내 위치로 이동",
                    tint = primaryColor,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // ── 로딩 / 에러 ───────────────────────────────────────────────
        when {
            isLoading -> {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 4.dp,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(18.dp),
                        color = primaryColor,
                    )
                }
            }
            errorMessage != null -> {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    )
                }
            }
        }

        // ── 클러스터 하단 패널 ─────────────────────────────────────────
        AnimatedVisibility(
            visible = selectedCluster != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            selectedCluster?.let { cluster ->
                ClusterPanel(
                    cluster = cluster,
                    onCourseClick = onCourseClick,
                    onDismiss = { selectedCluster = null },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 클러스터 하단 패널
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClusterPanel(
    cluster: CourseCluster,
    onCourseClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)) {
            // 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "이 위치에 ${cluster.courses.size}개 코스",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "닫기",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 가로 스크롤 코스 카드 목록
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(cluster.courses, key = { it.courseId }) { course ->
                    ClusterCourseCard(
                        course = course,
                        onClick = { onCourseClick(course.courseId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClusterCourseCard(
    course: NearbyCourseItem,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 190.dp, max = 240.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Route,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${formatCourseDistance(course.distanceMeters)} · " +
                    "${if (course.isLoop) "루프" else "일반"} · 완주 ${course.completionCount}회",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "탭하여 상세 보기 →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 단일 마커 말풍선
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CourseInfoBubble(course: NearbyCourseItem) {
    val bubbleColor = MaterialTheme.colorScheme.surface
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.widthIn(min = 180.dp, max = 250.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${formatCourseDistance(course.distanceMeters)} · " +
                        "${if (course.isLoop) "루프 코스" else "일반 코스"} · 완주 ${course.completionCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "눌러서 코스 상세 보기",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Canvas(
            modifier = Modifier
                .widthIn(min = 18.dp, max = 18.dp)
                .height(9.dp),
        ) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, color = bubbleColor)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 아이콘 생성
// ─────────────────────────────────────────────────────────────────────────────

private data class SportMarkerIcons(
    val normal: BitmapDescriptor,
    val selected: BitmapDescriptor,
)

/** 클러스터 마커: 원형 뱃지 + 숫자 */
private fun createClusterIcon(
    context: Context,
    primaryColor: Int,
    count: Int,
    selected: Boolean,
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val baseSize = if (selected) 46 else 40
    val sizePx = (baseSize * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val radius = sizePx / 2f - density

    // 그림자
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44000000 }
    canvas.drawCircle(cx + density, cy + density * 1.5f, radius, shadowPaint)

    // 배경 원
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
    canvas.drawCircle(cx, cy, radius, fillPaint)

    // 흰 테두리
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) 0xFFFFFFFF.toInt() else 0xCCFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = (if (selected) 2.8f else 2f) * density
    }
    canvas.drawCircle(cx, cy, radius - density, borderPaint)

    // 숫자 텍스트
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = (if (count >= 10) 14f else 16f) * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(count.toString(), cx, textY, textPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/** 단일 코스 핀 마커 */
private fun createSportMarkerIcon(
    context: Context,
    primaryColor: Int,
    surfaceColor: Int,
    selected: Boolean,
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val width = ((if (selected) 33 else 29) * density).toInt()
    val height = ((if (selected) 37 else 33) * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val logo = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_foreground)

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x59000000 }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) surfaceColor else 0xE6FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = (if (selected) 2.5f else 1.3f) * density
    }
    val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }

    val circleDiameter = width * 0.88f
    val circleLeft = (width - circleDiameter) / 2f
    val circleTop = density
    val circleRect = RectF(circleLeft, circleTop, circleLeft + circleDiameter, circleTop + circleDiameter)

    val tail = Path().apply {
        moveTo(width * 0.35f, circleRect.bottom - density)
        lineTo(width * 0.65f, circleRect.bottom - density)
        lineTo(width * 0.5f, height - density)
        close()
    }
    canvas.drawPath(tail, shadowPaint)
    canvas.drawPath(tail, tailPaint)

    canvas.drawCircle(
        circleRect.centerX() + density,
        circleRect.centerY() + 1.8f * density,
        circleDiameter / 2f,
        shadowPaint,
    )

    val clipPath = Path().apply { addOval(circleRect, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(clipPath)
    canvas.drawBitmap(
        logo,
        Rect(0, 0, logo.width, logo.height),
        circleRect,
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
    )
    canvas.restore()

    canvas.drawOval(circleRect, borderPaint)
    if (selected) {
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * density
        }
        canvas.drawOval(
            RectF(
                circleRect.left - density,
                circleRect.top - density,
                circleRect.right + density,
                circleRect.bottom + density,
            ),
            accentPaint,
        )
    }

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun formatCourseDistance(distanceMeters: Double): String =
    if (distanceMeters < 1000) "${distanceMeters.toInt()}m"
    else "%.1fkm".format(distanceMeters / 1000.0)

private val KOREA_CENTER = LatLng(36.35, 127.8)
