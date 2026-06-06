package com.runway.android.ui.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.runway.android.R
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.ui.theme.LocalIsDarkTheme

@Composable
fun DiscoverCourseMap(
    courses: List<NearbyCourseItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onCourseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val mapStyleOptions = remember(isDark) {
        val styleRes = if (isDark) R.raw.map_style_dark else R.raw.map_style_light
        runCatching { MapStyleOptions.loadRawResourceStyle(context, styleRes) }.getOrNull()
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(KOREA_CENTER, 6.55f)
    }
    var selectedCourse by remember { mutableStateOf<NearbyCourseItem?>(null) }

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
                isMyLocationEnabled = false,
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
                Marker(
                    state = MarkerState(
                        position = LatLng(
                            course.startPoint.latitude,
                            course.startPoint.longitude,
                        ),
                    ),
                    title = course.name,
                    snippet = formatCourseDistance(course.distanceMeters),
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (selected) BitmapDescriptorFactory.HUE_ORANGE
                        else BitmapDescriptorFactory.HUE_GREEN,
                    ),
                    zIndex = if (selected) 2f else 1f,
                    onClick = {
                        selectedCourse = course
                        true
                    },
                )
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

        selectedCourse?.let { course ->
            SelectedCoursePanel(
                course = course,
                onClick = { onCourseClick(course.courseId) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun SelectedCoursePanel(
    course: NearbyCourseItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Filled.Route,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(formatCourseDistance(course.distanceMeters))
                        append(" · ")
                        append(if (course.isLoop) "루프 코스" else "일반 코스")
                        append(" · 완주 ")
                        append(course.completionCount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "코스 상세 보기",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatCourseDistance(distanceMeters: Double): String =
    if (distanceMeters < 1000) {
        "${distanceMeters.toInt()}m"
    } else {
        "%.1fkm".format(distanceMeters / 1000.0)
    }

private val KOREA_CENTER = LatLng(36.35, 127.8)
