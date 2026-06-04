package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    kilometerElapsedSeconds: List<Int> = emptyList(),
    averagePaceSecondsPerKm: Int? = null,
    bestPaceSecondsPerKm: Int? = null,
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
                    .height(148.dp),
            ) {
                val w = size.width
                val h = size.height
                val paddingLeft = 48.dp.toPx()
                val paddingBottom = 34.dp.toPx()
                val chartW = w - paddingLeft
                val chartH = h - paddingBottom
                val maxElapsed = points.last().elapsedSeconds.toFloat().coerceAtLeast(1f)

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

                kilometerElapsedSeconds.forEach { elapsedSeconds ->
                    val x = paddingLeft + (elapsedSeconds / maxElapsed) * chartW
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, chartH),
                        strokeWidth = 0.5.dp.toPx(),
                    )
                    val label = formatElapsedTimeLabel(elapsedSeconds)
                    val measured = textMeasurer.measure(label, style = labelStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        style = labelStyle.copy(color = labelColor),
                        topLeft = Offset(
                            x = (x - measured.size.width / 2f).coerceIn(paddingLeft, w - measured.size.width),
                            y = chartH + 8.dp.toPx(),
                        ),
                    )
                }

                // Pace line
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
            if (averagePaceSecondsPerKm != null || bestPaceSecondsPerKm != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PaceSummary(
                        label = "평균 페이스",
                        value = formatPaceLabel(averagePaceSecondsPerKm),
                        modifier = Modifier.weight(1f),
                    )
                    PaceSummary(
                        label = "최고 페이스",
                        value = formatPaceLabel(bestPaceSecondsPerKm),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PaceSummary(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatPaceLabel(secsPerKm: Int?): String {
    if (secsPerKm == null || secsPerKm <= 0) return "--'--\""
    val m = secsPerKm / 60
    val s = secsPerKm % 60
    return "%d'%02d\"".format(m, s)
}

private fun formatElapsedTimeLabel(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
