package com.runway.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runway.android.data.user.model.AchievementItem
import com.runway.android.ui.theme.DisplayFontFamily

private val CardGreen = Color(0xFFB8FF00)
private val CardSurface = Color(0xFF0B100E)

@Composable
fun AchievementCard(
    item: AchievementItem,
    modifier: Modifier = Modifier,
) {
    val unlocked = item.unlocked
    val progress = if (item.target <= 0) 0f else {
        (item.progress.toFloat() / item.target.toFloat()).coerceIn(0f, 1f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (unlocked) 1f else 0.82f)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    if (unlocked) {
                        listOf(
                            CardGreen.copy(alpha = 0.72f),
                            CardGreen.copy(alpha = 0.18f),
                            CardGreen.copy(alpha = 0.06f),
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent,
                        )
                    },
                ),
                shape = MaterialTheme.shapes.extraLarge,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = CardSurface.copy(alpha = if (unlocked) 0.98f else 0.88f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(
                        if (unlocked) CardGreen.copy(alpha = 0.12f)
                        else Color.White.copy(alpha = 0.045f),
                        CircleShape,
                    )
                    .border(
                        1.dp,
                        if (unlocked) CardGreen.copy(alpha = 0.52f)
                        else Color.White.copy(alpha = 0.12f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = achievementEmoji(item.code),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.alpha(if (unlocked) 1f else 0.38f),
                )
                if (!unlocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .background(Color(0xFF181D1B), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = Color.White.copy(alpha = 0.45f),
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (unlocked) Color.White else Color.White.copy(alpha = 0.68f),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (unlocked) {
                        Row(
                            modifier = Modifier
                                .background(
                                    CardGreen.copy(alpha = 0.13f),
                                    RoundedCornerShape(50),
                                )
                                .border(
                                    1.dp,
                                    CardGreen.copy(alpha = 0.36f),
                                    RoundedCornerShape(50),
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = CardGreen,
                            )
                            Text(
                                text = "달성",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CardGreen,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = if (unlocked) 0.52f else 0.38f),
                )

                if (!unlocked) {
                    Spacer(Modifier.height(11.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp),
                        color = CardGreen.copy(alpha = 0.82f),
                        trackColor = Color.White.copy(alpha = 0.08f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "PROGRESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.3f),
                            letterSpacing = 0.8.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = formatProgress(item),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CardGreen.copy(alpha = 0.72f),
                        )
                    }
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
        item.code.startsWith("STREAK_") -> "${item.progress} / ${item.target}일"
        else -> "${item.progress} / ${item.target}"
    }
}
