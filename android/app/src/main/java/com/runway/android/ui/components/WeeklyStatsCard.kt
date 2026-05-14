package com.runway.android.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primary),
    ) {
        // Decorative circle — partially bleeds off the top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(128.dp)
                .offset(x = 32.dp, y = (-32).dp)
                .background(Color.Black.copy(alpha = 0.1f), CircleShape),
        )

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "THIS WEEK",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stats.distanceKm,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "km",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp)) {
                WeeklyStatItem(label = "RUNS", value = stats.runs)
                WeeklyStatItem(label = "PACE", value = stats.avgPace)
                WeeklyStatItem(label = "CAL", value = stats.calories)
            }
        }
    }
}

@Composable
private fun WeeklyStatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
