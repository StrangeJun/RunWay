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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.runway.android.R
import com.runway.android.core.map.MapPoint
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch

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
    var selectedCourse by remember { mutableStateOf<NearbyCourseItem?>(null) }
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val surfaceArgb = MaterialTheme.colorScheme.surface.toArgb()
    val markerIcons = remember(primaryArgb, surfaceArgb) {
        SportMarkerIcons(
            normal = createSportMarkerIcon(context, primaryArgb, surfaceArgb, selected = false),
            selected = createSportMarkerIcon(context, primaryArgb, surfaceArgb, selected = true),
        )
    }
    val markerStates = remember(courses) {
        courses.associate { course ->
            course.courseId to MarkerState(
                position = LatLng(course.startPoint.latitude, course.startPoint.longitude),
            )
        }
    }

    LaunchedEffect(Unit) {
        cameraPositionState.move(
            CameraUpdateFactory.newLatLngZoom(KOREA_CENTER, 6.55f),
        )
    }

    LaunchedEffect(courses) {
        selectedCourse = selectedCourse?.let { selected ->
            courses.firstOrNull { it.courseId == selected.courseId }
        }
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
            onMapClick = { selectedCourse = null },
        ) {
            courses.forEach { course ->
                val selected = course.courseId == selectedCourse?.courseId
                val markerState = markerStates.getValue(course.courseId)
                MarkerInfoWindow(
                    state = markerState,
                    anchor = Offset(0.5f, 0.92f),
                    infoWindowAnchor = Offset(0.5f, 0.05f),
                    icon = if (selected) markerIcons.selected else markerIcons.normal,
                    zIndex = if (selected) 2f else 1f,
                    onClick = { marker ->
                        selectedCourse = course
                        marker.showInfoWindow()
                        true
                    },
                    onInfoWindowClick = { onCourseClick(course.courseId) },
                ) {
                    CourseInfoBubble(course = course)
                }
            }
        }

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
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "전국 ${courses.size}개 코스",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

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
                        color = MaterialTheme.colorScheme.primary,
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

    }
}

@Composable
private fun CourseInfoBubble(
    course: NearbyCourseItem,
) {
    val bubbleColor = MaterialTheme.colorScheme.surface
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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

private data class SportMarkerIcons(
    val normal: BitmapDescriptor,
    val selected: BitmapDescriptor,
)

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

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x59000000
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) surfaceColor else 0xE6FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = (if (selected) 2.5f else 1.3f) * density
    }
    val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryColor
    }

    val circleDiameter = width * 0.88f
    val circleLeft = (width - circleDiameter) / 2f
    val circleTop = density
    val circleRect = RectF(
        circleLeft,
        circleTop,
        circleLeft + circleDiameter,
        circleTop + circleDiameter,
    )

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

    val clipPath = Path().apply {
        addOval(circleRect, Path.Direction.CW)
    }
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
    if (distanceMeters < 1000) {
        "${distanceMeters.toInt()}m"
    } else {
        "%.1fkm".format(distanceMeters / 1000.0)
    }

private val KOREA_CENTER = LatLng(36.35, 127.8)
