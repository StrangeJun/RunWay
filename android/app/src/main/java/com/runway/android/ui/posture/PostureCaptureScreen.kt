package com.runway.android.ui.posture

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.camera.video.FileOutputOptions
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

private const val MAX_RECORDING_SECONDS = 15
private const val MIN_RECORDING_SECONDS = 10

@Composable
fun PostureCaptureScreen(
    onVideoReady: (Uri) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showGuide by remember { mutableStateOf(true) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (isRecording && elapsedSeconds < MAX_RECORDING_SECONDS) {
                delay(1000)
                elapsedSeconds++
            }
            if (isRecording) activeRecording?.stop()
        }
    }

    val previewView = remember { PreviewView(context) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }

    if (hasCameraPermission) {
        DisposableEffect(lifecycleOwner) {
            val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    videoCapture,
                )
            }, ContextCompat.getMainExecutor(context))
            onDispose {
                runCatching {
                    androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context).get().unbindAll()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }

        if (showGuide && !isRecording) {
            PostureSilhouetteGuide(modifier = Modifier.fillMaxSize())
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!isRecording && showGuide) {
                Text(
                    "카메라를 옆면 허리 높이에 두고\n전신이 화면에 들어오도록 맞추세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { showGuide = false },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), MaterialTheme.shapes.extraLarge)
                        .padding(horizontal = 8.dp),
                ) {
                    Text("준비됐어요", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!showGuide && !isRecording) {
                RecordButton(
                    onClick = {
                        val vc = videoCapture ?: return@RecordButton
                        // Save to app-private cache dir — never appears in user gallery
                        val videoFile = java.io.File(
                            context.cacheDir,
                            "posture_${System.currentTimeMillis()}.mp4",
                        )
                        val outputOptions = FileOutputOptions.Builder(videoFile).build()

                        activeRecording = vc.output.prepareRecording(context, outputOptions)
                            .start(ContextCompat.getMainExecutor(context)) { event ->
                                when (event) {
                                    is VideoRecordEvent.Start -> isRecording = true
                                    is VideoRecordEvent.Finalize -> {
                                        isRecording = false
                                        if (!event.hasError()) {
                                            onVideoReady(Uri.fromFile(videoFile))
                                        } else {
                                            videoFile.delete()
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                    },
                    isRecording = false,
                )
            }

            if (isRecording) {
                val progress = elapsedSeconds / MAX_RECORDING_SECONDS.toFloat()
                val isSufficient = elapsedSeconds >= MIN_RECORDING_SECONDS
                Text(
                    "${elapsedSeconds}초 / ${MAX_RECORDING_SECONDS}초",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSufficient) Color(0xFF22C55E) else Color.White,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (isSufficient) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
                if (!isSufficient) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "10초 이상 녹화하면 더 정확합니다",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
                Spacer(Modifier.height(16.dp))
                RecordButton(
                    onClick = { activeRecording?.stop() },
                    isRecording = true,
                )
            }
        }
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .background(
                if (isRecording) MaterialTheme.colorScheme.error else Color.White,
                CircleShape,
            ),
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
            contentDescription = if (isRecording) "녹화 중지" else "녹화 시작",
            tint = if (isRecording) Color.White else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun PostureSilhouetteGuide(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val scale = h * 0.65f

        val overlayColor = Color.Black.copy(alpha = 0.35f)
        drawRect(overlayColor)

        val strokeColor = Color.White.copy(alpha = 0.85f)
        val strokeW = 3.dp.toPx()
        val stroke = Stroke(width = strokeW, cap = StrokeCap.Round)

        // head
        val headR = scale * 0.055f
        val headCy = h * 0.18f
        drawCircle(strokeColor, headR, Offset(cx, headCy), style = stroke)

        // torso — slight forward lean ~7 degrees
        val shoulderY = headCy + headR * 1.6f
        val hipY = shoulderY + scale * 0.25f
        val leanOffset = scale * 0.06f
        val shoulderX = cx - leanOffset * 0.3f
        val hipX = cx + leanOffset * 0.3f
        drawLine(strokeColor, Offset(shoulderX, shoulderY), Offset(hipX, hipY), strokeW, StrokeCap.Round)

        // arms — left arm swinging forward
        val elbowXL = shoulderX - scale * 0.10f
        val elbowYL = shoulderY + scale * 0.13f
        val wristXL = shoulderX - scale * 0.08f
        val wristYL = elbowYL - scale * 0.10f
        drawLine(strokeColor, Offset(shoulderX, shoulderY), Offset(elbowXL, elbowYL), strokeW, StrokeCap.Round)
        drawLine(strokeColor, Offset(elbowXL, elbowYL), Offset(wristXL, wristYL), strokeW, StrokeCap.Round)

        // arms — right arm swinging back
        val elbowXR = shoulderX + scale * 0.09f
        val elbowYR = shoulderY + scale * 0.13f
        val wristXR = shoulderX + scale * 0.07f
        val wristYR = elbowYR + scale * 0.09f
        drawLine(strokeColor, Offset(shoulderX, shoulderY), Offset(elbowXR, elbowYR), strokeW, StrokeCap.Round)
        drawLine(strokeColor, Offset(elbowXR, elbowYR), Offset(wristXR, wristYR), strokeW, StrokeCap.Round)

        // left leg — swing phase, knee up
        val kneeXL = hipX - scale * 0.07f
        val kneeYL = hipY + scale * 0.15f
        val ankleXL = kneeXL + scale * 0.05f
        val ankleYL = kneeYL + scale * 0.15f
        drawLine(strokeColor, Offset(hipX, hipY), Offset(kneeXL, kneeYL), strokeW, StrokeCap.Round)
        drawLine(strokeColor, Offset(kneeXL, kneeYL), Offset(ankleXL, ankleYL), strokeW, StrokeCap.Round)

        // right leg — stance phase
        val kneeXR = hipX + scale * 0.04f
        val kneeYR = hipY + scale * 0.17f
        val ankleXR = kneeXR - scale * 0.03f
        val ankleYR = kneeYR + scale * 0.17f
        drawLine(strokeColor, Offset(hipX, hipY), Offset(kneeXR, kneeYR), strokeW, StrokeCap.Round)
        drawLine(strokeColor, Offset(kneeXR, kneeYR), Offset(ankleXR, ankleYR), strokeW, StrokeCap.Round)
    }
}
