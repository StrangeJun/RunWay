package com.runway.android.ui.attempt

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.core.location.GpsStatus
import com.runway.android.ui.components.BatteryOptimizationCard
import com.runway.android.ui.components.LocationPermissionCard
import com.runway.android.ui.components.RouteMapView
import com.runway.android.ui.components.RunMetricCard
import com.runway.android.ui.components.RunningControlButton
import com.runway.android.ui.components.rememberBatteryOptimizationIgnored

@Composable
fun CourseAttemptTrackingScreen(
    onNavigateToLeaderboard: (courseId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CourseAttemptTrackingViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                is AttemptNavEvent.NavigateToLeaderboard -> onNavigateToLeaderboard(event.courseId)
                is AttemptNavEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    val context = LocalContext.current
    var permissionDeniedPermanently by remember { mutableStateOf(false) }
    var showAbandonDialog by remember { mutableStateOf(false) }
    var showBatteryCard by remember { mutableStateOf(true) }
    val isBatteryOptimizationIgnored = rememberBatteryOptimizationIgnored()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            viewModel.startTracking()
        } else {
            val activity = context as? Activity
            val shouldShow = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ?: false
            permissionDeniedPermanently = !shouldShow
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasActivityRecognition = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_GRANTED

        if ((hasFine || hasCoarse) && hasActivityRecognition) {
            viewModel.startTracking()
        } else {
            val permissions = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            permissionLauncher.launch(permissions)
        }
    }

    BackHandler(enabled = !viewModel.isFinishing && !viewModel.isAbandoning) {
        showAbandonDialog = true
    }

    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("도전을 포기하시겠어요?") },
            text = { Text("현재 진행 중인 코스 도전이 중단됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showAbandonDialog = false
                    viewModel.abandon()
                }) {
                    Text("포기", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonDialog = false }) {
                    Text("계속하기")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ─── 상단: 코스 도전 레이블 ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoursePill()
            Text(
                text = when {
                    viewModel.gpsStatus == GpsStatus.PERMISSION_REQUIRED -> "위치 권한 필요"
                    viewModel.gpsStatus == GpsStatus.WAITING_FOR_FIX -> "GPS 신호 수신 중..."
                    viewModel.isAutoPaused -> "자동 일시정지 중"
                    else -> "GPS · Active"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ─── 타이머 ───
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "TIME",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = viewModel.timerText,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── 지표 그리드 ───
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                RunMetricCard(
                    label = "DISTANCE",
                    value = viewModel.distanceText,
                    unit = "km",
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                RunMetricCard(
                    label = "PACE",
                    value = viewModel.paceText,
                    unit = "/km",
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                RunMetricCard(
                    label = "SPEED",
                    value = viewModel.speedText,
                    unit = "km/h",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ─── Milestone banner ───
        viewModel.milestoneMessage?.let { msg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ─── Battery optimization card ───
        if (showBatteryCard && !isBatteryOptimizationIgnored && viewModel.gpsStatus == GpsStatus.ACTIVE) {
            BatteryOptimizationCard(
                onDismiss = { showBatteryCard = false },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ─── 경로 미리보기 / 권한 카드 ───
        if (viewModel.gpsStatus == GpsStatus.PERMISSION_REQUIRED) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                LocationPermissionCard(
                    isPermanentlyDenied = permissionDeniedPermanently,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    },
                )
            }
        } else {
            CourseMapPanel(
                points = viewModel.coursePoints,
                currentLocation = viewModel.currentLocationPoint,
                trackStatus = viewModel.trackStatus,
                nearestDistanceMeters = viewModel.nearestCourseDistanceMeters,
                progressPercent = viewModel.courseProgressPercent,
                remainingDistanceText = viewModel.remainingDistanceText,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── 컨트롤 버튼 ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (viewModel.isAbandoning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.error,
                    strokeWidth = 3.dp,
                )
            } else {
                RunningControlButton(
                    icon = Icons.Filled.Close,
                    onClick = { showAbandonDialog = true },
                    size = 56.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.error,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            if (viewModel.isFinishing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(84.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                )
            } else {
                RunningControlButton(
                    icon = Icons.Filled.CheckCircle,
                    onClick = viewModel::finish,
                    size = 84.dp,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        if (viewModel.finishError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = viewModel.finishError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            )
        }

        Spacer(modifier = Modifier.height(44.dp))
    }
}

@Composable
private fun CourseMapPanel(
    points: List<com.runway.android.core.map.MapPoint>,
    currentLocation: com.runway.android.core.map.MapPoint?,
    trackStatus: CourseTrackStatus,
    nearestDistanceMeters: Double?,
    progressPercent: Int,
    remainingDistanceText: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        RouteMapView(
            points = points,
            currentLocation = currentLocation,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, statusColor(trackStatus).copy(alpha = 0.45f)),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = statusTitle(trackStatus),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor(trackStatus),
                        )
                        Text(
                            text = statusDetail(trackStatus, nearestDistanceMeters),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${progressPercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "남은 ${remainingDistanceText}km",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun statusColor(status: CourseTrackStatus): Color = when (status) {
    CourseTrackStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    CourseTrackStatus.ON_COURSE -> MaterialTheme.colorScheme.primary
    CourseTrackStatus.NEAR_COURSE -> com.runway.android.ui.theme.WarningYellow
    CourseTrackStatus.OFF_COURSE -> MaterialTheme.colorScheme.error
}

private fun statusTitle(status: CourseTrackStatus): String = when (status) {
    CourseTrackStatus.UNKNOWN -> "코스 확인 중"
    CourseTrackStatus.ON_COURSE -> "코스 위를 달리는 중"
    CourseTrackStatus.NEAR_COURSE -> "코스에서 조금 벗어남"
    CourseTrackStatus.OFF_COURSE -> "코스 이탈"
}

private fun statusDetail(status: CourseTrackStatus, meters: Double?): String {
    val distance = meters?.let { "%.0fm".format(it) } ?: "--m"
    return when (status) {
        CourseTrackStatus.UNKNOWN -> "현재 위치와 코스를 맞추고 있어요"
        CourseTrackStatus.ON_COURSE -> "코스와의 거리 $distance"
        CourseTrackStatus.NEAR_COURSE -> "코스와의 거리 $distance · 방향을 확인하세요"
        CourseTrackStatus.OFF_COURSE -> "코스와의 거리 $distance · 지도에서 복귀 경로를 확인하세요"
    }
}

@Composable
private fun CoursePill() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
    ) {
        Text(
            text = "COURSE ATTEMPT",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}
