package com.runway.android.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
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
        modifier = modifier.size(320.dp),
        contentAlignment = Alignment.Center
    ) {
        PathFinderImageRevealAnimation(
            progress = clampedProgress,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PathFinderImageRevealAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // 진행도에 따른 Easing 적용
    val easedProgress = easeInOut(progress)

    Image(
        painter = painterResource(id = R.drawable.app_logo),
        contentDescription = "PathFinder Logo",
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(primaryColor), // 테마의 포인트 색상으로 로고 렌더링
        modifier = modifier
            .graphicsLayer {
                // 부드러운 페이드인 효과
                val easedProgress = easeOut(progress)
                alpha = easedProgress
                
                // 살짝 커지는 스케일 효과
                val scale = 0.9f + (0.1f * easedProgress)
                scaleX = scale
                scaleY = scale
            }
    )
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
private fun PathFinderExactLogoAnimationPreview() {
    RunwayTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            RunwayLogoAnimation(progress = 0.6f)
        }
    }
}
