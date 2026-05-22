package com.runway.android.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.data.running.model.RunningStatsResponse
import com.runway.android.ui.components.StatsSummaryCard
import com.runway.android.ui.components.StreakCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "통계",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        // 기간 선택 칩
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            viewModel.periodLabels.forEachIndexed { index, label ->
                FilterChip(
                    selected = viewModel.selectedPeriodIndex == index,
                    onClick = { viewModel.selectPeriod(index) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            viewModel.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = viewModel.errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = viewModel::retry) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            viewModel.stats != null -> {
                StatsContent(
                    stats = viewModel.stats!!,
                    periodLabel = viewModel.periodLabels[viewModel.selectedPeriodIndex],
                )
            }
        }
        } // PullToRefreshBox
    }
}

@Composable
private fun StatsContent(
    stats: RunningStatsResponse,
    periodLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // streak 카드
        StreakCard(
            currentStreak = stats.currentStreakDays,
            longestStreak = stats.longestStreakDays,
            modifier = Modifier.fillMaxWidth(),
        )

        // 핵심 지표 그리드
        StatsSummaryCard(
            label = "총 러닝",
            value = "${stats.totalRuns}회",
        )
        StatsSummaryCard(
            label = "총 거리",
            value = formatDistance(stats.totalDistanceMeters),
        )
        StatsSummaryCard(
            label = "총 시간",
            value = formatDuration(stats.totalDurationSeconds),
        )
        StatsSummaryCard(
            label = "총 칼로리",
            value = "${stats.totalCaloriesBurned} kcal",
        )
        StatsSummaryCard(
            label = "평균 페이스",
            value = formatPace(stats.averagePaceSecondsPerKm),
        )
        StatsSummaryCard(
            label = "최장 러닝",
            value = formatDistance(stats.longestRunMeters),
        )
        StatsSummaryCard(
            label = "활동일",
            value = "${stats.activeDays}일",
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters < 1000) "${meters.toInt()} m"
    else "${"%.1f".format(meters / 1000.0)} km"
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun formatPace(secsPerKm: Int): String {
    if (secsPerKm <= 0) return "--'--\""
    return "%d'%02d\"".format(secsPerKm / 60, secsPerKm % 60)
}
