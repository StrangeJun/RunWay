package com.runway.android.ui.running

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.core.location.GpsStatus
import com.runway.android.ui.components.LocationPermissionCard
import com.runway.android.ui.components.RouteMapPlaceholder
import com.runway.android.ui.components.RunMetricCard
import com.runway.android.ui.components.RunningControlButton

@Composable
fun RunningTrackingScreen(
    onFinish: (runId: String?, elapsedSeconds: Int, distanceKm: Float) -> Unit,
    onBack: () -> Unit,
    viewModel: RunningTrackingViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.navigateToResult.collect { result ->
            onFinish(result.runId, result.elapsedSeconds, result.distanceKm)
        }
    }

    val context = LocalContext.current
    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    // Requests location (required) + notification (optional, Android 13+) together.
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
            // Notification permission denial is silently ignored — tracking still works.
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            viewModel.startTracking()
        } else {
            val permissions = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            permissionLauncher.launch(permissions)
        }
    }

    val isRunning = viewModel.runningState == RunningState.RUNNING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ─── Status row ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordingPill(isRunning = isRunning)
            Text(
                text = when {
                    viewModel.isConnecting -> "Connecting..."
                    viewModel.gpsStatus == GpsStatus.PERMISSION_REQUIRED -> "위치 권한 필요"
                    viewModel.gpsStatus == GpsStatus.WAITING_FOR_FIX -> "GPS 신호 수신 중..."
                    !isRunning -> "Paused"
                    else -> "GPS · Active"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ─── Timer ───
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

        // ─── Metric grid ───
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

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Route map / Permission card ───
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
            RouteMapPlaceholder(
                isAnimated = isRunning,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── Controls ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (viewModel.isFinishing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(60.dp),
                    color = MaterialTheme.colorScheme.error,
                    strokeWidth = 3.dp,
                )
            } else {
                RunningControlButton(
                    icon = Icons.Filled.Stop,
                    onClick = viewModel::finish,
                    size = 60.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.error,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

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
}

@Composable
private fun RecordingPill(isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = InfiniteRepeatableSpec(tween(700), repeatMode = RepeatMode.Reverse),
        label = "dot_alpha",
    )

    val pillColor = if (isRunning) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, pillColor.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(
                    color = pillColor.copy(alpha = if (isRunning) dotAlpha else 0.5f),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isRunning) "RECORDING" else "PAUSED",
                style = MaterialTheme.typography.labelLarge,
                color = pillColor,
            )
        }
    }
}
