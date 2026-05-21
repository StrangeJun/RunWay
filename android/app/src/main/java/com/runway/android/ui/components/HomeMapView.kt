package com.runway.android.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.runway.android.R
import com.runway.android.core.map.MapPoint
import com.runway.android.ui.theme.RunwayGreen

@SuppressLint("MissingPermission")
@Composable
fun HomeMapView(
    currentLocation: MapPoint?,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.5665, 126.9780), 13f)
    }

    val isDark = isSystemInDarkTheme()
    val mapStyleOptions = remember(isDark) {
        if (isDark) runCatching { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) }.getOrNull()
        else null
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15.5f),
                durationMs = 900,
            )
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false,
                mapStyleOptions = mapStyleOptions,
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                rotationGesturesEnabled = false,
                tiltGesturesEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
            ),
        )

        if (currentLocation != null && hasLocationPermission) {
            PulsingLocationDot(
                color = RunwayGreen,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun PulsingLocationDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "gps_pulse")

    val ring1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring1",
    )
    val ring2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut, delayMillis = 700),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring2",
    )
    val ring3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut, delayMillis = 1400),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring3",
    )

    Canvas(modifier = modifier.size(88.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f

        // 파동 링 3개 (순차적으로 퍼짐)
        for ((progress, delay) in listOf(ring1 to 0, ring2 to 700, ring3 to 1400)) {
            if (progress > 0f) {
                drawCircle(
                    color = color.copy(alpha = (1f - progress) * 0.35f),
                    radius = maxRadius * progress,
                    center = center,
                )
                drawCircle(
                    color = color.copy(alpha = (1f - progress) * 0.55f),
                    radius = maxRadius * progress,
                    center = center,
                    style = Stroke(1.5.dp.toPx()),
                )
            }
        }

        // 흰 테두리 링 (GPS 수신 표시)
        drawCircle(Color.White, 10.dp.toPx(), center)
        drawCircle(Color.White.copy(alpha = 0.4f), 13.dp.toPx(), center, style = Stroke(1.dp.toPx()))

        // 컨셉 색 중앙 dot
        drawCircle(color, 7.dp.toPx(), center)
    }
}
