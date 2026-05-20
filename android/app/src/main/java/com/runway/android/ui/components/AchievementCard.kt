package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runway.android.data.user.model.AchievementItem

@Composable
fun AchievementCard(
    item: AchievementItem,
    modifier: Modifier = Modifier,
) {
    val unlocked = item.unlocked
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (unlocked) 1.5.dp else 1.dp,
            color = if (unlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 배지 아이콘 (텍스트 기반 이모지)
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = achievementEmoji(item.code),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (unlocked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                    )
                    if (unlocked) {
                        Text(
                            text = "달성",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!unlocked && item.target > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (item.progress.toFloat() / item.target.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatProgress(item),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun achievementEmoji(code: String): String = when (code) {
    "FIRST_RUN" -> "🏃"
    "TOTAL_10K" -> "🥉"
    "TOTAL_50K" -> "🥈"
    "TOTAL_100K" -> "🥇"
    "STREAK_3" -> "🔥"
    "STREAK_7" -> "⚡"
    "FIRST_COURSE_CREATED" -> "🗺️"
    "FIRST_COURSE_COMPLETED" -> "🏁"
    else -> "🏅"
}

private fun formatProgress(item: AchievementItem): String {
    return when {
        item.code.startsWith("TOTAL_") -> {
            val progressKm = "%.1f".format(item.progress / 1000.0)
            val targetKm = "%.0f".format(item.target / 1000.0)
            "$progressKm / $targetKm km"
        }
        item.code.startsWith("STREAK_") -> "${item.progress} / ${item.target} days"
        else -> "${item.progress} / ${item.target}"
    }
}
