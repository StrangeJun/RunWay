package com.runway.android.ui.running.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.core.map.MapPoint
import com.runway.android.core.util.formatRunDateFull
import com.runway.android.core.util.formatTime
import com.runway.android.ui.components.RouteMapView
import com.runway.android.ui.components.RunDetailMetricGrid
import com.runway.android.ui.components.RunSplitsCard

@Composable
fun RunDetailScreen(
    onBack: () -> Unit,
    viewModel: RunDetailViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ─── 상단 바 ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로 가기",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "러닝 상세",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // ─── 콘텐츠 ───
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
            viewModel.hasError -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "기록을 불러오지 못했습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::retry) {
                            Text("다시 시도")
                        }
                    }
                }
            }
            viewModel.detail != null -> {
                val detail = viewModel.detail!!

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ─── 날짜 + 시각 헤더 ───
                    item {
                        Column {
                            Text(
                                text = formatRunDateFull(detail.startedAt),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val startTime = formatTime(detail.startedAt)
                            val endTime = formatTime(detail.endedAt)
                            val timeRange = if (endTime.isNotEmpty()) "$startTime → $endTime"
                                           else startTime
                            Text(
                                text = timeRange,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // ─── 지표 그리드 ───
                    item {
                        RunDetailMetricGrid(
                            distanceMeters = detail.distanceMeters,
                            durationSeconds = detail.durationSeconds,
                            avgPaceSecondsPerKm = detail.avgPaceSecondsPerKm,
                            caloriesBurned = detail.caloriesBurned,
                            avgHeartRateBpm = detail.avgHeartRateBpm,
                        )
                    }

                    // ─── 경로 미리보기 ───
                    item {
                        Text(
                            text = "ROUTE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RouteMapView(
                            points = detail.points.map { MapPoint(it.latitude, it.longitude) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(MaterialTheme.shapes.extraLarge),
                        )
                    }

                    // ─── 1km 구간 기록 ───
                    item {
                        RunSplitsCard(splits = viewModel.splits)
                    }
                }
            }
        }
    }
}
