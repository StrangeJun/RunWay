package com.runway.android.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.runway.android.R
import com.runway.android.ui.theme.RunwayTheme

@Composable
fun RunwayLogoAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        PathFinderMarkDrawAnimation(
            progress = clampedProgress,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PathFinderMarkDrawAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val drawProgress = (easeInOut(progress) / 0.78f).coerceIn(0f, 1f)
    val revealProgress = ((progress - 0.70f) / 0.30f).coerceIn(0f, 1f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension * 0.68f
            val left = (size.width - diameter) / 2f
            val top = (size.height - diameter) / 2f
            val scale = diameter / 48f

            drawCircle(
                color = Color(0xFF111318),
                radius = diameter / 2f,
                center = Offset(size.width / 2f, size.height / 2f),
            )

            val mark = Path().apply {
                moveTo(left + 13f * scale, top + 30f * scale)
                lineTo(left + 20f * scale, top + 18f * scale)
                lineTo(left + 25f * scale, top + 24f * scale)
                lineTo(left + 31f * scale, top + 13f * scale)
                lineTo(left + 35f * scale, top + 16f * scale)
                lineTo(left + 26f * scale, top + 35f * scale)
                lineTo(left + 20f * scale, top + 28f * scale)
                lineTo(left + 17f * scale, top + 33f * scale)
                close()
            }
            val measure = PathMeasure().apply { setPath(mark, false) }
            val segment = Path()
            measure.getSegment(0f, measure.length * drawProgress, segment, true)
            drawPath(
                path = segment,
                color = primaryColor,
                style = Stroke(width = 2.4f * scale, cap = StrokeCap.Round),
            )
        }

        Image(
            painter = painterResource(id = R.drawable.app_logo_mark),
            contentDescription = "PathFinder Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize(0.68f)
                .graphicsLayer {
                    alpha = easeOut(revealProgress)
                    val revealScale = 0.94f + 0.06f * revealProgress
                    scaleX = revealScale
                    scaleY = revealScale
                },
        )
    }
}

private fun easeOut(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return 1f - (1f - t) * (1f - t)
}

private fun easeInOut(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PathFinderLogoAnimationPreview() {
    RunwayTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            RunwayLogoAnimation(progress = 0.6f)
        }
    }
}
