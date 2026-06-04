package com.runway.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay

private val confettiColors = listOf(
    Color(0xFFA4E168), // lime
    Color(0xFFFFD700), // gold
    Color(0xFFFF6B6B), // coral
    Color(0xFF64B5F6), // blue
    Color(0xFFCE93D8), // purple
    Color(0xFFFFB74D), // orange
)

private data class Particle(
    val x0: Float,       // normalized start x [0, 1]
    val velX: Float,     // normalized velocity
    val velY: Float,     // initial upward velocity (negative = up)
    val color: Color,
    val size: Float,     // px
    val rotationSpeed: Float,
    val phase: Float,    // time offset so particles don't all start simultaneously
)

@Composable
fun ConfettiCanvas(modifier: Modifier = Modifier) {
    val particles = remember {
        List(28) { i ->
            Particle(
                x0 = (i * 0.037f + (i % 5) * 0.13f) % 1f,
                velX = ((i % 7) - 3) * 0.00018f,
                velY = -0.0008f - (i % 4) * 0.00015f,
                color = confettiColors[i % confettiColors.size],
                size = 14f + (i % 5) * 4f,
                rotationSpeed = 120f + (i % 6) * 40f,
                phase = (i * 80L).toFloat(),
            )
        }
    }

    var elapsed by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (elapsed < 2200f) {
            elapsed = (System.currentTimeMillis() - start).toFloat()
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val gravity = 0.0000012f * h

        particles.forEach { p ->
            val t = (elapsed - p.phase).coerceAtLeast(0f)
            if (t <= 0f) return@forEach
            val x = (p.x0 + p.velX * t) * w
            val y = p.velY * t * h + 0.5f * gravity * t * t * h
            val screenY = h * 0.08f + y
            if (screenY > h + 40f) return@forEach

            val alpha = (1f - (t / 2200f)).coerceIn(0f, 1f)
            val rot = (p.rotationSpeed * t / 1000f) % 360f

            rotate(degrees = rot, pivot = Offset(x, screenY)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2f, screenY - p.size / 4f),
                    size = Size(p.size, p.size / 2f),
                )
            }
        }
    }
}
