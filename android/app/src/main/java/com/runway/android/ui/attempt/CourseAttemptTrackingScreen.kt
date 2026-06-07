package com.runway.android.ui.attempt

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
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
import com.runway.android.ui.components.ConfettiCanvas
import com.runway.android.ui.running.RunningCountdownOverlay
import com.runway.android.ui.components.LocationPermissionCard
import com.runway.android.ui.components.RouteMapView
import com.runway.android.ui.components.RunMetricCard
import com.runway.android.ui.components.RunningControlButton
import com.runway.android.ui.components.rememberBatteryOptimizationIgnored

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseAttemptTrackingScreen(
    onNavigateToLeaderboard: (courseId: String, isPR: Boolean, previousBestSeconds: Int?, improvementSeconds: Int?) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CourseAttemptTrackingViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                is AttemptNavEvent.NavigateToLeaderboard -> onNavigateToLeaderboard(
                    event.courseId, event.isPR, event.previousBestSeconds, event.improvementSeconds,
                )
                is AttemptNavEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    val context = LocalContext.current
    var permissionDeniedPermanently by remember { mutableStateOf(false) }
    var showStopSheet by remember { mutableStateOf(false) }
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

    var countdownDone by remember { mutableStateOf(false) }

    LaunchedEffect(countdownDone) {
        if (!countdownDone) return@LaunchedEffect
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
        if (!countdownDone) onNavigateBack() else showStopSheet = true
    }

    if (showStopSheet) {
        StopConfirmSheet(
            progressPercent = viewModel.courseProgressPercent,
            timerText = viewModel.timerText,
            distanceText = viewModel.distanceText,
            paceText = viewModel.paceText,
            isFinishing = viewModel.isFinishing,
            isAbandoning = viewModel.isAbandoning,
            onFinish = { showStopSheet = false; viewModel.finish() },
            onAbandon = { showStopSheet = false; viewModel.abandon() },
            onContinue = { showStopSheet = false },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    viewModel.isPaused -> "일시정지"
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

        // ─── 코스 이탈 경고 배너 ───
        if (viewModel.showDeviationWarning) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                onClick = viewModel::dismissDeviationWarning,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "코스를 이탈했습니다 · 일시정지됨. 코스로 돌아오면 자동 재개됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "닫기",
                        tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

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
            if (viewModel.isFinishing || viewModel.isAbandoning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(60.dp),
                    color = MaterialTheme.colorScheme.error,
                    strokeWidth = 3.dp,
                )
            } else {
                RunningControlButton(
                    icon = Icons.Filled.Stop,
                    onClick = { showStopSheet = true },
                    size = 60.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.error,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            val isRunning = !viewModel.isPaused && !viewModel.isAutoPaused
            if (isRunning) {
                RunningControlButton(
                    icon = Icons.Filled.Pause,
                    onClick = viewModel::pause,
                    size = 84.dp,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                RunningControlButton(
                    icon = Icons.Filled.PlayArrow,
                    onClick = viewModel::resume,
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

    AnimatedVisibility(
        visible = !countdownDone,
        enter = EnterTransition.None,
        exit = fadeOut(tween(350)),
    ) {
        RunningCountdownOverlay(onFinished = { countdownDone = true })
    }

    if (viewModel.courseProgressPercent >= 100) {
        ConfettiCanvas(modifier = Modifier.fillMaxSize())
    }
    } // Box
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopConfirmSheet(
    progressPercent: Int,
    timerText: String,
    distanceText: String,
    paceText: String,
    isFinishing: Boolean,
    isAbandoning: Boolean,
    onFinish: () -> Unit,
    onAbandon: () -> Unit,
    onContinue: () -> Unit,
) {
    val completed = progressPercent >= 100
    ModalBottomSheet(
        onDismissRequest = onContinue,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 완주 여부 배지
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = if (completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = if (completed) "코스 완주!" else "미완주 · ${progressPercent}% 완료",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (completed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }

            // 진행률 바
            LinearProgressIndicator(
                progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (completed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // 지표
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StopStatBlock(label = "TIME", value = timerText)
                StopStatBlock(label = "DIST", value = "${distanceText}km")
                StopStatBlock(label = "PACE", value = "${paceText}/km")
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 저장 버튼
            if (isFinishing) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            } else {
                Surface(
                    onClick = onFinish,
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (completed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                    border = if (!completed) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (completed) "리더보드 확인" else "기록 저장하고 나가기",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (completed) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }

            // 보조 버튼
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onContinue, modifier = Modifier.weight(1f)) {
                    Text("계속 달리기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isAbandoning) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    TextButton(onClick = onAbandon, modifier = Modifier.weight(1f)) {
                        Text("포기", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun StopStatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
    val animatedProgress by animateFloatAsState(
        targetValue = (progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "courseProgress",
    )
    val progressColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = tween(400),
        label = "progressColor",
    )

    Column(modifier = modifier) {
        RouteMapView(
            points = points,
            currentLocation = currentLocation,
            gesturesEnabled = true,
            followCurrentLocation = true,
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
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = progressColor,
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
