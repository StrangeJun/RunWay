package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.runway.android.core.running.RunChartPoint

@Composable
fun RunPaceChart(
    points: List<RunChartPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val textMeasurer = rememberTextMeasurer()

    val minPace = remember(points) { points.minOf { it.paceSecondsPerKm } }
    val maxPace = remember(points) { points.maxOf { it.paceSecondsPerKm } }
    val paceRange = remember(minPace, maxPace) { (maxPace - minPace).coerceAtLeast(60) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "페이스 변화",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                val w = size.width
                val h = size.height
                val paddingLeft = 48.dp.toPx()
                val paddingBottom = 20.dp.toPx()
                val chartW = w - paddingLeft
                val chartH = h - paddingBottom

                // Grid lines (3 horizontal)
                val gridSteps = 3
                for (step in 0..gridSteps) {
                    val y = chartH * step / gridSteps
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(w, y),
                        strokeWidth = 0.5.dp.toPx(),
                    )
                    // Y-axis label: pace increases downward (slower = higher y)
                    val paceAtStep = maxPace - (paceRange * step / gridSteps)
                    val label = formatPaceLabel(paceAtStep)
                    val measured = textMeasurer.measure(label, style = labelStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        style = labelStyle.copy(color = labelColor),
                        topLeft = Offset(0f, y - measured.size.height / 2f),
                    )
                }

                // Pace line
                val maxElapsed = points.last().elapsedSeconds.toFloat().coerceAtLeast(1f)
                val path = Path()
                points.forEachIndexed { i, pt ->
                    val x = paddingLeft + (pt.elapsedSeconds / maxElapsed) * chartW
                    // higher pace (slower) → higher y
                    val normalized = (pt.paceSecondsPerKm - minPace).toFloat() / paceRange
                    val y = chartH * normalized
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}

private fun formatPaceLabel(secsPerKm: Int): String {
    val m = secsPerKm / 60
    val s = secsPerKm % 60
    return "%d'%02d\"".format(m, s)
}
