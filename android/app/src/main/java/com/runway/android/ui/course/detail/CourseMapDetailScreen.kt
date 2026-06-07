package com.runway.android.ui.course.detail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Location
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.runway.android.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
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

@Composable
fun CourseMapDetailScreen(
    onBack: () -> Unit,
    viewModel: CourseMapDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            viewModel.points.size < 2 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "경로 데이터가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> {
                CourseFullMap(
                    points = viewModel.points,
                    isLoop = viewModel.isLoop,
                    context = context,
                )
            }
        }

        // Floating back button
        Surface(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 4.dp,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(10.dp)
                    .size(22.dp),
            )
        }

        // Course name pill
        if (viewModel.courseName.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = viewModel.courseName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CourseFullMap(
    points: List<MapPoint>,
    isLoop: Boolean,
    context: Context,
) {
    val latLngs = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }
    val bounds = remember(points) { points.toLatLngBounds() }
    val cameraPositionState = rememberCameraPositionState()
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryArgb = primaryColor.toArgb()
    val isDark = LocalIsDarkTheme.current
    val mapStyleOptions = remember(isDark) {
        val styleRes = if (isDark) R.raw.map_style_dark else R.raw.map_style_light
        runCatching { MapStyleOptions.loadRawResourceStyle(context, styleRes) }.getOrNull()
    }

    val kmMarkers = remember(points) { calculateKmMarkers(points) }
    val kmIcons = remember(primaryArgb, kmMarkers) {
        kmMarkers.associate { (km, _) -> km to createKmMarkerIcon(context, km, primaryArgb) }
    }
    val startIcon = remember(context) {
        BitmapDescriptorFactory.fromBitmap(createPillMarker(context, "Start", "#22C55E"))
    }
    val finishIcon = remember(context) {
        BitmapDescriptorFactory.fromBitmap(createPillMarker(context, "Finish", "#EF4444"))
    }

    var mapLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        delay(50)
        runCatching {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 80), 300)
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false, mapStyleOptions = mapStyleOptions),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            scrollGesturesEnabled = true,
            zoomGesturesEnabled = true,
            rotationGesturesEnabled = true,
            tiltGesturesEnabled = false,
            compassEnabled = true,
            mapToolbarEnabled = false,
        ),
        onMapLoaded = { mapLoaded = true },
    ) {
        // Route polyline
        Polyline(
            points = latLngs,
            color = primaryColor,
            width = 14f,
            startCap = RoundCap(),
            endCap = RoundCap(),
            jointType = JointType.ROUND,
        )

        // Start marker
        Marker(
            state = MarkerState(position = latLngs.first()),
            title = "Start",
            icon = startIcon,
            anchor = Offset(0.5f, 0.5f),
            zIndex = 3f,
        )

        // Finish marker — last point (loop 코스는 start 위치와 거의 같음)
        Marker(
            state = MarkerState(position = latLngs.last()),
            title = "Finish",
            icon = finishIcon,
            anchor = Offset(0.5f, 0.5f),
            zIndex = 3f,
        )

        // Km markers
        kmMarkers.forEach { (km, point) ->
            val icon = kmIcons[km] ?: return@forEach
            Marker(
                state = MarkerState(position = LatLng(point.latitude, point.longitude)),
                title = "${km}km",
                icon = icon,
                zIndex = 1f,
            )
        }
    }
}

// ─── Utilities ───────────────────────────────────────────────────────────────

/** Every 1km along the polyline, returns (kmNumber, interpolated MapPoint). */
private fun calculateKmMarkers(points: List<MapPoint>): List<Pair<Int, MapPoint>> {
    if (points.size < 2) return emptyList()
    val distResult = FloatArray(1)
    var totalMeters = 0.0
    var nextKm = 1
    val markers = mutableListOf<Pair<Int, MapPoint>>()

    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        Location.distanceBetween(prev.latitude, prev.longitude, curr.latitude, curr.longitude, distResult)
        val segLen = distResult[0].toDouble()
        val segStart = totalMeters
        totalMeters += segLen

        while (nextKm * 1000.0 <= totalMeters) {
            val fraction = if (segLen > 0.0) (nextKm * 1000.0 - segStart) / segLen else 0.0
            val lat = prev.latitude + fraction * (curr.latitude - prev.latitude)
            val lon = prev.longitude + fraction * (curr.longitude - prev.longitude)
            markers.add(Pair(nextKm, MapPoint(lat, lon)))
            nextKm++
        }
    }
    return markers
}

/** Circle bitmap with km number — e.g. "1K", "2K". */
private fun createKmMarkerIcon(context: Context, km: Int, primaryArgb: Int): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val size = (30 * density).toInt().coerceAtLeast(30)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // White border ring
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    // Filled circle
    paint.color = primaryArgb
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - (1.5f * density), paint)

    // Text
    paint.color = android.graphics.Color.WHITE
    paint.textSize = size * 0.40f
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val metrics = paint.fontMetrics
    val textY = size / 2f - (metrics.ascent + metrics.descent) / 2f
    canvas.drawText("${km}K", size / 2f, textY, paint)

    return BitmapDescriptorFactory.fromBitmap(bmp)
}

/** Rounded-pill bitmap matching RouteMapView's Start/Finish style. */
private fun createPillMarker(context: Context, label: String, colorHex: String): Bitmap {
    val density = context.resources.displayMetrics.density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * density
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    val textW = textPaint.measureText(label)
    val padH = 10f * density
    val padV = 6f * density
    val w = (textW + padH * 2).toInt().coerceAtLeast(1)
    val h = (textPaint.textSize + padV * 2).toInt().coerceAtLeast(1)
    val r = h / 2f

    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val bg = Paint(Paint.ANTI_ALIAS_FLAG)

    bg.color = android.graphics.Color.parseColor(colorHex)
    bg.style = Paint.Style.FILL
    canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, bg)

    bg.color = android.graphics.Color.WHITE
    bg.style = Paint.Style.STROKE
    bg.strokeWidth = 1.5f * density
    val sw = bg.strokeWidth / 2
    canvas.drawRoundRect(sw, sw, w - sw, h - sw, r, r, bg)

    textPaint.color = android.graphics.Color.WHITE
    val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(label, w / 2f, textY, textPaint)

    return bmp
}
