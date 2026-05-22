package com.runway.android.ui.running

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val CountdownBackground = Color(0xFF07070E)

@Composable
fun RunningCountdownOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var count by remember { mutableIntStateOf(3) }
    val arcProgress = remember { Animatable(0f) }
    val context = LocalContext.current

    // Arc sweeps over 900ms per count; resets on each change
    LaunchedEffect(count) {
        arcProgress.snapTo(0f)
        if (count > 0) {
            arcProgress.animateTo(1f, animationSpec = tween(900, easing = LinearEasing))
        }
    }

    LaunchedEffect(Unit) {
        vibrateCountdownTick(context)
        delay(1_000)
        count = 2
        vibrateCountdownTick(context)
        delay(1_000)
        count = 1
        vibrateCountdownTick(context)
        delay(1_000)
        count = 0
        vibrateCountdownGo(context)
        delay(500)
        onFinished()
    }

    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CountdownBackground),
        contentAlignment = Alignment.Center,
    ) {
        // Subtle brand mark at top
        Text(
            text = "RUNWAY",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 5.sp,
            color = Color.White.copy(alpha = 0.10f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp),
        )

        // Ring + number
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                // Dim background ring
                drawArc(
                    color = primary.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke,
                )
                // Bright sweeping ring
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = 360f * arcProgress.value,
                    useCenter = false,
                    style = stroke,
                )
            }

            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    // New number slams in from large scale; old shrinks out
                    scaleIn(
                        initialScale = 1.5f,
                        animationSpec = tween(260),
                    ) + fadeIn(tween(180)) togetherWith
                        scaleOut(
                            targetScale = 0.7f,
                            animationSpec = tween(220),
                        ) + fadeOut(tween(160))
                },
                label = "countdown_number",
            ) { c ->
                Text(
                    text = if (c == 0) "GO!" else c.toString(),
                    fontSize = if (c == 0) 72.sp else 116.sp,
                    fontWeight = FontWeight.Black,
                    color = if (c == 0) primary else Color.White,
                )
            }
        }

    }
}

@Suppress("DEPRECATION")
private fun vibrateCountdownTick(context: Context) {
    getVibrator(context).vibrate(
        VibrationEffect.createOneShot(38, VibrationEffect.DEFAULT_AMPLITUDE)
    )
}

@Suppress("DEPRECATION")
private fun vibrateCountdownGo(context: Context) {
    getVibrator(context).vibrate(
        VibrationEffect.createWaveform(longArrayOf(0, 80, 45, 130), -1)
    )
}

@Suppress("DEPRECATION")
private fun getVibrator(context: Context): Vibrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
