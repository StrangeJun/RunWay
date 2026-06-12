package com.runway.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class WeeklyStats(
    val distanceKm: String = "24.6",
    val runs: String = "4",
    val avgPace: String = "5'12\"",
    val calories: String = "1,820",
)

@Composable
fun WeeklyStatsCard(
    stats: WeeklyStats,
    modifier: Modifier = Modifier,
) {
    var triggered by remember { mutableStateOf(false) }
    val distTarget = stats.distanceKm.toFloatOrNull() ?: 0f
    LaunchedEffect(stats.distanceKm) { triggered = true }
    val animatedDist by animateFloatAsState(
        targetValue = if (triggered) distTarget else 0f,
        animationSpec = tween(900),
        label = "weeklyDist",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .runwayCardFrame(MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "주간 진행 상황",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.1f".format(animatedDist),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 48.sp,
                            lineHeight = 44.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "km",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Runs: ${stats.runs}", style = MaterialTheme.typography.labelSmall)
                    Text("Pace: ${stats.avgPace}", style = MaterialTheme.typography.labelSmall)
                    Text("Cal: ${stats.calories}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
