package com.runway.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import com.runway.android.ui.theme.LocalIsDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.runway.android.R
import com.runway.android.core.map.MapPoint
import com.runway.android.core.map.toLatLngBounds

/**
 * Shows a GoogleMap with the route polyline when [points] has ≥ 2 entries.
 * Falls back to a Canvas route drawing when < 2 points.
 * [currentLocation] overlays an azure marker for live GPS position when non-null.
 */
@Composable
fun RouteMapView(
    points: List<MapPoint>,
    modifier: Modifier = Modifier,
    currentLocation: MapPoint? = null,
) {
    if (points.size >= 2) {
        RouteGoogleMap(points = points, currentLocation = currentLocation, modifier = modifier)
    } else {
        RouteCanvasFallback(modifier = modifier)
    }
}

@Composable
private fun RouteGoogleMap(
    points: List<MapPoint>,
    currentLocation: MapPoint?,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val latLngs = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }
    val bounds = remember(points) { points.toLatLngBounds() }
    val cameraPositionState = rememberCameraPositionState()
    val primaryColor = MaterialTheme.colorScheme.primary
    val currentLatLng = remember(currentLocation) {
        currentLocation?.let { LatLng(it.latitude, it.longitude) }
    }
    val isDark = LocalIsDarkTheme.current
    val mapStyleOptions = remember(isDark) {
        val styleRes = if (isDark) R.raw.map_style_dark else R.raw.map_style_light
        runCatching { MapStyleOptions.loadRawResourceStyle(context, styleRes) }.getOrNull()
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false, mapStyleOptions = mapStyleOptions),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            scrollGesturesEnabled = false,
            zoomGesturesEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false,
        ),
        onMapLoaded = {
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 48))
        },
    ) {
        Polyline(
            points = latLngs,
            color = primaryColor,
            width = 8f,
            startCap = RoundCap(),
            endCap = RoundCap(),
            jointType = JointType.ROUND,
        )
        Marker(
            state = MarkerState(position = latLngs.first()),
            title = "출발",
        )
        Marker(
            state = MarkerState(position = latLngs.last()),
            title = "도착",
        )
        if (currentLatLng != null) {
            Marker(
                state = MarkerState(position = currentLatLng),
                title = "현재 위치",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
            )
        }
    }
}

@Composable
private fun RouteCanvasFallback(modifier: Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.background(bgColor)) {
        val w = size.width
        val h = size.height
        val strokePx = 2.5.dp.toPx()
        val dotPx = 5.dp.toPx()

        val gridColor = primaryColor.copy(alpha = 0.10f)
        val gridStep = 24.dp.toPx()
        var xi = 0f
        while (xi <= w) { drawLine(gridColor, Offset(xi, 0f), Offset(xi, h), 1f); xi += gridStep }
        var yi = 0f
        while (yi <= h) { drawLine(gridColor, Offset(0f, yi), Offset(w, yi), 1f); yi += gridStep }

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
