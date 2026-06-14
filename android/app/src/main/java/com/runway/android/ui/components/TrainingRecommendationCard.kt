package com.runway.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runway.android.domain.training.TrainingRecommendation
import com.runway.android.domain.training.TrainingRecommendationStatus
import com.runway.android.domain.training.TrainingSession

@Composable
fun TrainingRecommendationCard(
    recommendation: TrainingRecommendation,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                            MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ),
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(11.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.plan.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${recommendation.plan.weeklyRuns}회 · 안전한 1주 구성",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = recommendation.plan.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (recommendation.status == TrainingRecommendationStatus.INSUFFICIENT_DATA) {
                ReadinessProgress(
                    label = "유효 러닝",
                    valueText = "${recommendation.readiness.validRunCount} / " +
                        "${recommendation.readiness.requiredRunCount}회",
                    progress = recommendation.readiness.validRunCount.toFloat() /
                        recommendation.readiness.requiredRunCount,
                )
                ReadinessProgress(
                    label = "최근 2주 거리",
                    valueText = "%.1f / %.0fkm".format(
                        recommendation.readiness.totalDistanceMeters / 1_000.0,
                        recommendation.readiness.requiredDistanceMeters / 1_000.0,
                    ),
                    progress = (
                        recommendation.readiness.totalDistanceMeters /
                            recommendation.readiness.requiredDistanceMeters
                        ).toFloat(),
                )
            }

            recommendation.metrics?.let { metrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetricChip(
                        label = "평균 페이스",
                        value = metrics.averagePaceSecondsPerKm?.let(::formatPace) ?: "--",
                        modifier = Modifier.weight(1f),
                    )
                    MetricChip(
                        label = "주간 거리",
                        value = "%.1fkm".format(metrics.weeklyDistanceMeters / 1_000.0),
                        modifier = Modifier.weight(1f),
                    )
                    MetricChip(
                        label = "주간 횟수",
                        value = "%.1f회".format(metrics.runsPerWeek),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recommendation.plan.sessions.forEach { session ->
                    TrainingSessionRow(session)
                }
            }

            Text(
                text = "훈련 추천은 참고용입니다. 통증이나 부상이 있다면 운동을 중단하고 전문가와 상담하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReadinessProgress(
    label: String,
    valueText: String,
    progress: Float,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(7.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrainingSessionRow(session: TrainingSession) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = session.type.name.take(1),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = sessionTargetText(session),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun sessionTargetText(session: TrainingSession): String = buildList {
    session.targetDistanceMeters?.takeIf { it > 0 }?.let {
        add("%.1fkm".format(it / 1_000.0))
    }
    session.targetDurationMinutes?.let { add("${it}분") }
    session.targetPaceText?.let(::add)
}.joinToString(" · ")

private fun formatPace(seconds: Int): String =
    "%d'%02d\"".format(seconds / 60, seconds % 60)
