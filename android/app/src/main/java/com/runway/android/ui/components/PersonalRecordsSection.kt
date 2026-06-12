package com.runway.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.runway.android.data.running.model.PersonalRecordItem
import com.runway.android.data.running.model.PersonalRecordsResponse
import com.runway.android.ui.components.runwayCardFrame
import com.runway.android.ui.theme.LocalIsDarkTheme
import com.runway.android.ui.theme.SurfaceContainerDark

@Composable
fun PersonalRecordsSection(
    records: PersonalRecordsResponse?,
    onRecordClick: (runId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "개인 최고 기록",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val isDark = LocalIsDarkTheme.current
        if (records == null || records.totalCompletedRuns == 0L) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .runwayCardFrame(MaterialTheme.shapes.extraLarge),
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isDark) SurfaceContainerDark else MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    text = "완료된 러닝 기록이 쌓이면 개인 최고 기록이 표시됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
            return
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            records.longestRun?.let {
                PersonalRecordCard(
                    icon = Icons.Filled.Straighten,
                    label = "가장 긴 러닝",
                    value = formatDistance(it.distanceMeters),
                    sub = formatDuration(it.durationSeconds),
                    item = it,
                    onClick = onRecordClick,
                )
            }
            records.fastestPace?.let {
                PersonalRecordCard(
                    icon = Icons.Filled.Speed,
                    label = "최고 페이스",
                    value = formatPace(it.avgPaceSecondsPerKm),
                    sub = formatDistance(it.distanceMeters),
                    item = it,
                    onClick = onRecordClick,
                )
            }
            records.mostCalories?.let {
                PersonalRecordCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "최고 칼로리",
                    value = "${it.caloriesBurned ?: 0} kcal",
                    sub = formatDistance(it.distanceMeters),
                    item = it,
                    onClick = onRecordClick,
                )
            }
        }
    }
}

@Composable
private fun PersonalRecordCard(
    icon: ImageVector,
    label: String,
    value: String,
    sub: String,
    item: PersonalRecordItem,
    onClick: (runId: String) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .runwayCardFrame(MaterialTheme.shapes.extraLarge),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isDark) SurfaceContainerDark else MaterialTheme.colorScheme.surface,
        onClick = { onClick(item.runId) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDistance(meters: Double?): String {
    if (meters == null) return "--"
    return if (meters >= 1000) "%.1f km".format(meters / 1000) else "${meters.toInt()} m"
}

private fun formatDuration(seconds: Int?): String {
    if (seconds == null) return "--"
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

private fun formatPace(secsPerKm: Int?): String {
    if (secsPerKm == null || secsPerKm <= 0) return "--'--\""
    return "%d'%02d\"/km".format(secsPerKm / 60, secsPerKm % 60)
}
