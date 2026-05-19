package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runway.android.core.util.formatDistance
import com.runway.android.core.util.formatDuration
import com.runway.android.core.util.formatPace

@Composable
fun RunDetailMetricGrid(
    distanceMeters: Double?,
    durationSeconds: Int?,
    avgPaceSecondsPerKm: Int?,
    caloriesBurned: Int?,
    avgHeartRateBpm: Int?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "STATS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            MetricRow(label = "Distance", value = formatDistance(distanceMeters))
            MetricDivider()
            MetricRow(label = "Duration", value = formatDuration(durationSeconds))
            MetricDivider()
            MetricRow(label = "Avg Pace", value = formatPace(avgPaceSecondsPerKm))

            caloriesBurned?.let { cal ->
                MetricDivider()
                MetricRow(label = "Calories", value = "$cal kcal")
            }
            avgHeartRateBpm?.let { hr ->
                MetricDivider()
                MetricRow(label = "Avg Heart Rate", value = "$hr bpm")
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MetricDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
