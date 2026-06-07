package com.runway.android.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.runway.android.BuildConfig
import com.runway.android.ui.theme.RunwayTheme

private const val SplashAnimationDurationMillis = 2_000

@Composable
fun RunwaySplashScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        try {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = SplashAnimationDurationMillis,
                    easing = LinearEasing,
                ),
            )
        } finally {
            onAnimationFinished()
        }
    }

    val taglineAlpha = ((progress.value - 0.85f) / 0.15f).coerceIn(0f, 1f)
    val brandProgress = ((progress.value - 0.68f) / 0.24f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RunwayLogoAnimation(progress = progress.value)
            Text(
                text = BuildConfig.APP_NAME,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = brandProgress
                    translationY = (1f - brandProgress) * 18.dp.toPx()
                },
            )
        }
        Text(
            text = "Run your way.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .alpha(taglineAlpha)
                .height(48.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111119)
@Composable
private fun RunwaySplashScreenPreview() {
    RunwayTheme {
        RunwaySplashScreen(onAnimationFinished = {})
    }
}
