package com.runway.android.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.toArgb
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
    gesturesEnabled: Boolean = false,
    showKilometerMarkers: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    if (points.size >= 2) {
        RouteGoogleMap(
            points = points,
            currentLocation = currentLocation,
            modifier = modifier,
            gesturesEnabled = gesturesEnabled,
            showKilometerMarkers = showKilometerMarkers,
            onClick = onClick,
        )
    } else {
        RouteCanvasFallback(modifier = modifier)
    }
}

@Composable
private fun RouteGoogleMap(
    points: List<MapPoint>,
    currentLocation: MapPoint?,
    modifier: Modifier,
    gesturesEnabled: Boolean,
    showKilometerMarkers: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val latLngs = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }
    val bounds = remember(points) { points.toLatLngBounds() }
    val cameraPositionState = rememberCameraPositionState()
    val primaryColor = MaterialTheme.colorScheme.primary
    val kilometerMarkers = remember(points, showKilometerMarkers) {
        if (showKilometerMarkers) points.calculateKilometerMarkers() else emptyList()
    }
    val primaryColorArgb = primaryColor.toArgb()
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
            zoomControlsEnabled = gesturesEnabled,
            scrollGesturesEnabled = gesturesEnabled,
            zoomGesturesEnabled = gesturesEnabled,
            rotationGesturesEnabled = gesturesEnabled,
            tiltGesturesEnabled = gesturesEnabled,
            compassEnabled = gesturesEnabled,
            mapToolbarEnabled = false,
        ),
        onMapClick = { onClick?.invoke() },
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
        kilometerMarkers.forEach { marker ->
            val icon = remember(marker.km, primaryColorArgb) {
                BitmapDescriptorFactory.fromBitmap(
                    createKmMarkerBitmap(context, marker.km, primaryColorArgb)
                )
            }
            Marker(
                state = MarkerState(position = marker.position),
                icon = icon,
                title = "${marker.km}km",
                zIndex = 1f,
                anchor = Offset(0.5f, 0.5f),
            )
        }
        val startIcon = remember(context) {
            BitmapDescriptorFactory.fromBitmap(createStartMarkerBitmap(context))
        }
        val endIcon = remember(context) {
            BitmapDescriptorFactory.fromBitmap(createEndMarkerBitmap(context))
        }
        Marker(
            state = MarkerState(position = latLngs.first()),
            icon = startIcon,
            title = "출발",
            anchor = Offset(0.5f, 0.5f),
            zIndex = 2f,
        )
        Marker(
            state = MarkerState(position = latLngs.last()),
            icon = endIcon,
            title = "도착",
            anchor = Offset(0.5f, 0.5f),
            zIndex = 2f,
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

private data class KilometerMarker(
    val km: Int,
    val position: LatLng,
)

private fun List<MapPoint>.calculateKilometerMarkers(): List<KilometerMarker> {
    if (size < 2) return emptyList()

    val markers = mutableListOf<KilometerMarker>()
    var cumulativeMeters = 0.0
    var nextKilometer = 1

    for (i in 1 until size) {
        val start = this[i - 1]
        val end = this[i]
        val segmentMeters = distanceMeters(start, end)
        if (segmentMeters <= 0.0) continue

        while (cumulativeMeters + segmentMeters >= nextKilometer * 1000.0) {
            val targetIntoSegment = nextKilometer * 1000.0 - cumulativeMeters
            val fraction = (targetIntoSegment / segmentMeters).coerceIn(0.0, 1.0)
            markers.add(
                KilometerMarker(
                    km = nextKilometer,
                    position = LatLng(
                        start.latitude + (end.latitude - start.latitude) * fraction,
                        start.longitude + (end.longitude - start.longitude) * fraction,
                    ),
                )
            )
            nextKilometer++
        }

        cumulativeMeters += segmentMeters
    }

    return markers
}

private fun createKmMarkerBitmap(context: Context, km: Int, fillColor: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = (28 * density).toInt().coerceAtLeast(28)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)

    // Filled circle
    paint.color = fillColor
    paint.style = AndroidPaint.Style.FILL
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

    // White border
    paint.color = android.graphics.Color.WHITE
    paint.style = AndroidPaint.Style.STROKE
    paint.strokeWidth = density * 2f
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - density, paint)

    // km number
    paint.style = AndroidPaint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    paint.textSize = sizePx * 0.42f
    paint.textAlign = AndroidPaint.Align.CENTER
    val textY = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(km.toString(), sizePx / 2f, textY, paint)

    return bitmap
}

private fun createRoutePillMarker(context: Context, label: String, colorHex: String): Bitmap {
    val density = context.resources.displayMetrics.density
    val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * density
        isFakeBoldText = true
        textAlign = AndroidPaint.Align.CENTER
    }
    val textW = textPaint.measureText(label)
    val padH = 10f * density
    val padV = 6f * density
    val w = (textW + padH * 2).toInt().coerceAtLeast(1)
    val h = (textPaint.textSize + padV * 2).toInt().coerceAtLeast(1)
    val r = h / 2f

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val bgPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)

    // Filled pill
    bgPaint.color = android.graphics.Color.parseColor(colorHex)
    bgPaint.style = AndroidPaint.Style.FILL
    canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, bgPaint)

    // White border
    bgPaint.color = android.graphics.Color.WHITE
    bgPaint.style = AndroidPaint.Style.STROKE
    bgPaint.strokeWidth = 1.5f * density
    canvas.drawRoundRect(
        bgPaint.strokeWidth / 2, bgPaint.strokeWidth / 2,
        w - bgPaint.strokeWidth / 2, h - bgPaint.strokeWidth / 2,
        r, r, bgPaint,
    )

    // Label
    textPaint.color = android.graphics.Color.WHITE
    val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(label, w / 2f, textY, textPaint)

    return bitmap
}

private fun createStartMarkerBitmap(context: Context): Bitmap =
    createRoutePillMarker(context, "Start", "#22C55E")

private fun createEndMarkerBitmap(context: Context): Bitmap =
    createRoutePillMarker(context, "Finish", "#EF4444")

private fun distanceMeters(start: MapPoint, end: MapPoint): Double {
    if (start.latitude == end.latitude && start.longitude == end.longitude) return 0.0
    val result = FloatArray(1)
    Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, result)
    return result[0].toDouble()
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
