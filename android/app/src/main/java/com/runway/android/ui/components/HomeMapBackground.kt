package com.runway.android.ui.components

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private val MapBg = Color(0xFF0F1018)
private val BlockColor = Color(0xFF17181F)
private val RoadMain = Color(0xFF252636)
private val RoadNarrow = Color(0xFF1C1D28)
private val GpsGreen = Color(0xFFA4E168)

@Composable
fun HomeMapBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "map_pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 3.4f,
        animationSpec = InfiniteRepeatableSpec(tween(1500), RepeatMode.Restart),
        label = "pulse_r",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0f,
        animationSpec = InfiniteRepeatableSpec(tween(1500), RepeatMode.Restart),
        label = "pulse_a",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Dark map background
        drawRect(MapBg, size = size)

        // City blocks — slightly lighter rectangles simulating building footprints
        val blocks = listOf(
            floatArrayOf(0.03f, 0.03f, 0.24f, 0.26f),
            floatArrayOf(0.28f, 0.03f, 0.46f, 0.19f),
            floatArrayOf(0.50f, 0.03f, 0.65f, 0.27f),
            floatArrayOf(0.69f, 0.03f, 0.97f, 0.17f),
            floatArrayOf(0.03f, 0.30f, 0.21f, 0.54f),
            floatArrayOf(0.37f, 0.32f, 0.63f, 0.52f),
            floatArrayOf(0.67f, 0.22f, 0.97f, 0.40f),
            floatArrayOf(0.03f, 0.60f, 0.31f, 0.80f),
            floatArrayOf(0.35f, 0.58f, 0.60f, 0.76f),
            floatArrayOf(0.64f, 0.46f, 0.97f, 0.66f),
            floatArrayOf(0.03f, 0.84f, 0.46f, 0.97f),
            floatArrayOf(0.50f, 0.82f, 0.97f, 0.97f),
        )
        blocks.forEach { b ->
            drawRect(
                color = BlockColor,
                topLeft = Offset(b[0] * w, b[1] * h),
                size = Size((b[2] - b[0]) * w, (b[3] - b[1]) * h),
            )
        }

        // Main roads (wider, brighter)
        val mainW = 10.dp.toPx()
        val narrowW = 5.dp.toPx()

        drawLine(RoadMain, Offset(0f, h * 0.275f), Offset(w, h * 0.275f), mainW)
        drawLine(RoadMain, Offset(0f, h * 0.570f), Offset(w, h * 0.570f), mainW)
        drawLine(RoadMain, Offset(0f, h * 0.830f), Offset(w, h * 0.830f), mainW)
        drawLine(RoadMain, Offset(w * 0.255f, 0f), Offset(w * 0.255f, h), mainW)
        drawLine(RoadMain, Offset(w * 0.640f, 0f), Offset(w * 0.640f, h), mainW)

        // Narrow side streets
        drawLine(RoadNarrow, Offset(0f, h * 0.035f), Offset(w, h * 0.035f), narrowW)
        drawLine(RoadNarrow, Offset(0f, h * 0.580f), Offset(w, h * 0.580f), narrowW)
        drawLine(RoadNarrow, Offset(w * 0.035f, 0f), Offset(w * 0.035f, h), narrowW)
        drawLine(RoadNarrow, Offset(w * 0.470f, 0f), Offset(w * 0.470f, h), narrowW)
        drawLine(RoadNarrow, Offset(w * 0.960f, 0f), Offset(w * 0.960f, h), narrowW)

        // Diagonal curved street
        val diagPath = Path().apply {
            moveTo(w * 0.26f, 0f)
            cubicTo(w * 0.32f, h * 0.20f, w * 0.42f, h * 0.34f, w * 0.50f, h * 0.50f)
            cubicTo(w * 0.58f, h * 0.64f, w * 0.63f, h * 0.74f, w * 0.64f, h)
        }
        drawPath(
            diagPath,
            color = RoadNarrow,
            style = Stroke(narrowW, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Current location — center of the map canvas
        val center = Offset(w * 0.50f, h * 0.43f)

        // Outer pulse ring
        drawCircle(GpsGreen.copy(alpha = pulseAlpha * 0.30f), radius = 20.dp.toPx() * pulseRadius, center = center)
        // Inner pulse ring
        drawCircle(GpsGreen.copy(alpha = pulseAlpha * 0.55f), radius = 11.dp.toPx() * pulseRadius, center = center)

        // Accuracy circle (static, faint)
        drawCircle(GpsGreen.copy(alpha = 0.08f), radius = 28.dp.toPx(), center = center)

        // GPS dot: white shell → green fill → white center
        drawCircle(Color.White, radius = 8.dp.toPx(), center = center)
        drawCircle(GpsGreen, radius = 6.dp.toPx(), center = center)
        drawCircle(Color.White, radius = 2.5.dp.toPx(), center = center)
    }
}
