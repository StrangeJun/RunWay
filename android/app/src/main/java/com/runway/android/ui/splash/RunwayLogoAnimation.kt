package com.runway.android.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runway.android.ui.theme.BackgroundDark
import com.runway.android.ui.theme.RunwayGreen
import com.runway.android.ui.theme.RunwayTheme
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun RunwayLogoAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val wordmarkProgress = ((clampedProgress - 0.68f) / 0.24f).coerceIn(0f, 1f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LogoMarkCanvas(
            progress = clampedProgress,
            modifier = Modifier.size(172.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "RunWay",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
            modifier = Modifier
                .alpha(easeOut(wordmarkProgress))
                .graphicsLayer {
                    translationY = (1f - easeOut(wordmarkProgress)) * 18.dp.toPx()
                },
        )
    }
}

@Composable
private fun LogoMarkCanvas(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.42f

        val circleProgress = ((progress - 0.46f) / 0.24f).coerceIn(0f, 1f)
        val circleAlpha = easeOut(circleProgress)
        val circleScale = 0.82f + circleAlpha * 0.18f
        drawCircle(
            color = primary.copy(alpha = 0.14f * circleAlpha),
            radius = radius * circleScale,
            center = center,
            style = Stroke(width = 13.dp.toPx()),
        )
        drawCircle(
            color = primary.copy(alpha = 0.62f * circleAlpha),
            radius = radius * circleScale,
            center = center,
            style = Stroke(width = 2.5.dp.toPx()),
        )

        val start = Offset(size.width * 0.346f, size.height * 0.392f)
        val end = Offset(size.width * 0.688f, size.height * 0.594f)
        val routeProgress = ((progress - 0.2f) / 0.46f).coerceIn(0f, 1f)
        val fullRoutePath = buildLogoRoutePath(size.width, size.height)
        val routePath = fullRoutePath.revealed(easeInOut(routeProgress))

        drawPath(
            path = routePath,
            color = primary.copy(alpha = 0.2f),
            style = Stroke(
                width = 15.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        drawPath(
            path = routePath,
            brush = Brush.linearGradient(
                colors = listOf(primary.copy(alpha = 0.75f), primary),
                start = start,
                end = end,
            ),
            style = Stroke(
                width = 5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        val pulse = (sin((progress * 5.8f * PI).toFloat()) + 1f) / 2f
        val dotScale = 0.8f + pulse * 0.35f
        drawCircle(
            color = primary.copy(alpha = 0.18f),
            radius = 25.dp.toPx() * dotScale,
            center = start,
        )
        drawCircle(
            color = primary.copy(alpha = 0.46f),
            radius = 15.dp.toPx() * dotScale,
            center = start,
        )
        drawCircle(
            color = primary,
            radius = 7.dp.toPx(),
            center = start,
        )

        val pinProgress = ((progress - 0.58f) / 0.18f).coerceIn(0f, 1f)
        drawLocationPin(
            center = end,
            scale = 0.72f + easeOut(pinProgress) * 0.28f,
            alpha = easeOut(pinProgress),
            color = primary,
            cutoutColor = background,
            highlightColor = onBackground,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLocationPin(
    center: Offset,
    scale: Float,
    alpha: Float,
    color: Color,
    cutoutColor: Color,
    highlightColor: Color,
) {
    if (alpha <= 0f) return

    val headRadius = 15.dp.toPx() * scale
    val tip = Offset(center.x, center.y + 32.dp.toPx() * scale)
    val pinPath = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                center = center,
                radius = headRadius,
            ),
        )
        moveTo(center.x - 10.dp.toPx() * scale, center.y + 10.dp.toPx() * scale)
        quadraticTo(center.x, tip.y, center.x + 10.dp.toPx() * scale, center.y + 10.dp.toPx() * scale)
        close()
    }

    drawPath(path = pinPath, color = color.copy(alpha = 0.24f * alpha), style = Stroke(width = 9.dp.toPx()))
    drawPath(path = pinPath, color = color.copy(alpha = alpha))
    drawCircle(
        color = cutoutColor.copy(alpha = alpha),
        radius = 6.dp.toPx() * scale,
        center = center,
    )
    drawArc(
        color = highlightColor.copy(alpha = 0.32f * alpha),
        startAngle = 205f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(center.x - headRadius * 0.72f, center.y - headRadius * 0.74f),
        size = Size(headRadius * 1.1f, headRadius * 1.1f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun buildLogoRoutePath(width: Float, height: Float): Path {
    fun x(value: Float) = width * value
    fun y(value: Float) = height * value

    return Path().apply {
        moveTo(x(0.346f), y(0.392f))
        lineTo(x(0.346f), y(0.324f))
        cubicTo(
            x(0.346f),
            y(0.284f),
            x(0.378f),
            y(0.284f),
            x(0.408f),
            y(0.284f),
        )
        lineTo(x(0.548f), y(0.284f))
        cubicTo(
            x(0.644f),
            y(0.284f),
            x(0.652f),
            y(0.361f),
            x(0.652f),
            y(0.386f),
        )
        cubicTo(
            x(0.652f),
            y(0.462f),
            x(0.604f),
            y(0.488f),
            x(0.542f),
            y(0.488f),
        )
        lineTo(x(0.444f), y(0.484f))
        cubicTo(
            x(0.397f),
            y(0.484f),
            x(0.390f),
            y(0.512f),
            x(0.390f),
            y(0.548f),
        )
        lineTo(x(0.390f), y(0.604f))
        cubicTo(
            x(0.390f),
            y(0.626f),
            x(0.407f),
            y(0.640f),
            x(0.430f),
            y(0.640f),
        )
        lineTo(x(0.505f), y(0.640f))
        cubicTo(
            x(0.538f),
            y(0.640f),
            x(0.548f),
            y(0.618f),
            x(0.524f),
            y(0.590f),
        )
        cubicTo(
            x(0.505f),
            y(0.568f),
            x(0.493f),
            y(0.544f),
            x(0.512f),
            y(0.522f),
        )
        cubicTo(
            x(0.532f),
            y(0.500f),
            x(0.560f),
            y(0.520f),
            x(0.586f),
            y(0.548f),
        )
        lineTo(x(0.688f), y(0.666f))
    }
}

private fun Path.revealed(progress: Float): Path {
    val revealedPath = Path()
    val measure = PathMeasure()
    measure.setPath(this, false)
    measure.getSegment(
        startDistance = 0f,
        stopDistance = measure.length * progress.coerceIn(0f, 1f),
        destination = revealedPath,
        startWithMoveTo = true,
    )
    return revealedPath
}

private fun easeOut(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return 1f - (1f - t) * (1f - t)
}

private fun easeInOut(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Preview(showBackground = true, backgroundColor = 0xFF111119)
@Composable
private fun RunwayLogoAnimationPreview() {
    RunwayTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center,
        ) {
            RunwayLogoAnimation(progress = 1f)
        }
    }
}
