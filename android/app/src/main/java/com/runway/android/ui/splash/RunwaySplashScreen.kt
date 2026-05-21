package com.runway.android.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        RunwayLogoAnimation(progress = progress.value)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111119)
@Composable
private fun RunwaySplashScreenPreview() {
    RunwayTheme {
        RunwaySplashScreen(onAnimationFinished = {})
    }
}
