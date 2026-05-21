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
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.core.map.MapPoint
import com.runway.android.core.share.ShareUtils
import com.runway.android.core.util.formatDuration
import com.runway.android.core.util.formatPace
import com.runway.android.core.util.formatRunDateFull
import com.runway.android.core.util.formatTime
import com.runway.android.ui.components.RouteMapView
import com.runway.android.ui.components.RunDetailMetricGrid
import com.runway.android.ui.components.RunPaceChart
import com.runway.android.ui.components.RunSplitsCard

@Composable
fun RunDetailScreen(
    onBack: () -> Unit,
    onDeleted: (runId: String) -> Unit = {},
    onShareImage: (runId: String) -> Unit = {},
    viewModel: RunDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    // 삭제 완료 시 콜백 호출 후 뒤로가기
    LaunchedEffect(viewModel.isDeleted) {
        if (viewModel.isDeleted) {
            onDeleted(viewModel.runId)
            onBack()
        }
    }

    // 삭제 확인 다이얼로그
    if (viewModel.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("기록 삭제") },
            text = { Text("이 러닝 기록을 영구적으로 삭제할까요? 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) { Text("취소") }
            },
        )
    }

    // 거리 수정 다이얼로그
    if (viewModel.showTrimDialog) {
        val maxKm = viewModel.maxTrimKm
        AlertDialog(
            onDismissRequest = viewModel::dismissTrimDialog,
            title = { Text("거리 수정") },
            text = {
                Column {
                    Text(
                        text = "현재 기록: ${"%.2f".format(maxKm)} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${"%.2f".format(viewModel.trimTargetKm)} km",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Slider(
                        value = viewModel.trimTargetKm,
                        onValueChange = { viewModel.trimTargetKm = it },
                        valueRange = 0.1f..maxKm,
                        steps = ((maxKm - 0.1f) / 0.1f).toInt().coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "줄이기만 가능 · 시간/페이스는 비례 조정",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmTrim,
                    enabled = viewModel.trimTargetKm < maxKm,
                ) {
                    Text("수정")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissTrimDialog) { Text("취소") }
            },
        )
    }

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
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    val detail = viewModel.detail
                    if (detail != null) {
                        ShareUtils.shareRunSummary(
                            context = context,
                            distanceFormatted = if ((detail.distanceMeters ?: 0.0) >= 1000)
                                "%.2f km".format((detail.distanceMeters ?: 0.0) / 1000)
                            else "${(detail.distanceMeters ?: 0).toInt()} m",
                            duration = formatDuration(detail.durationSeconds),
                            pace = formatPace(detail.avgPaceSecondsPerKm),
                            date = formatRunDateFull(detail.startedAt),
                        )
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TextSnippet,
                    contentDescription = "텍스트 공유",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onShareImage(viewModel.runId) }) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "공유 이미지 만들기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // ⋮ 더보기 메뉴
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "더보기",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("거리 수정") },
                        leadingIcon = {
                            Icon(Icons.Filled.ContentCut, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            viewModel.openTrimDialog()
                        },
                        enabled = viewModel.detail != null,
                    )
                    DropdownMenuItem(
                        text = { Text("삭제", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            viewModel.openDeleteDialog()
                        },
                    )
                }
            }
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

                    // ─── 페이스 차트 ───
                    if (viewModel.chartPoints.isNotEmpty()) {
                        item {
                            RunPaceChart(points = viewModel.chartPoints)
                        }
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
