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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
        val scale = h * 0.63f

        drawRect(Color.Black.copy(alpha = 0.30f))

        val ink = Color.White.copy(alpha = 0.90f)
        val sw = 3.8f.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash)

        // ── HEAD ──
        val headR = scale * 0.053f
        val headCy = h * 0.175f
        drawCircle(ink, headR, Offset(cx, headCy), style = stroke)

        // ── SKELETON ANCHORS (slight forward lean ~7°) ──
        val lean = scale * 0.048f
        val shX = cx - lean * 0.35f;  val shY = headCy + headR * 1.72f   // shoulder
        val hipX = cx + lean * 0.35f; val hipY = shY + scale * 0.258f    // hip

        // ── TORSO OUTLINE (closed path with width) ──
        val tw = scale * 0.030f   // half-width
        val torso = Path().apply {
            moveTo(shX - tw, shY)
            cubicTo(
                shX - tw * 1.15f, shY + scale * 0.085f,
                hipX - tw * 0.85f, shY + scale * 0.175f,
                hipX - tw * 0.55f, hipY,
            )
            lineTo(hipX + tw * 0.55f, hipY)
            cubicTo(
                hipX + tw * 0.85f, shY + scale * 0.175f,
                shX + tw * 1.15f, shY + scale * 0.085f,
                shX + tw, shY,
            )
            close()
        }
        drawPath(torso, ink, style = stroke)

        // ── LEFT ARM — forward swing ──
        val eXL = shX - scale * 0.107f; val eYL = shY + scale * 0.130f   // elbow
        val wXL = shX - scale * 0.080f; val wYL = eYL - scale * 0.108f   // wrist
        drawPath(Path().apply {
            moveTo(shX - tw * 0.5f, shY)
            cubicTo(shX - scale * 0.055f, shY + scale * 0.048f, eXL + scale * 0.01f, eYL - scale * 0.022f, eXL, eYL)
            cubicTo(eXL - scale * 0.008f, eYL + scale * 0.010f, wXL - scale * 0.010f, wYL + scale * 0.032f, wXL, wYL)
        }, ink, style = stroke)

        // ── RIGHT ARM — back swing ──
        val eXR = shX + scale * 0.098f; val eYR = shY + scale * 0.126f
        val wXR = shX + scale * 0.076f; val wYR = eYR + scale * 0.096f
        drawPath(Path().apply {
            moveTo(shX + tw * 0.5f, shY)
            cubicTo(shX + scale * 0.050f, shY + scale * 0.045f, eXR - scale * 0.010f, eYR - scale * 0.020f, eXR, eYR)
            cubicTo(eXR + scale * 0.008f, eYR + scale * 0.010f, wXR + scale * 0.010f, wYR - scale * 0.028f, wXR, wYR)
        }, ink, style = stroke)

        // ── LEFT LEG — swing phase (knee lifted) ──
        val kXL = hipX - scale * 0.073f; val kYL = hipY + scale * 0.152f  // knee
        val aXL = kXL + scale * 0.056f; val aYL = kYL + scale * 0.148f   // ankle
        drawPath(Path().apply {
            moveTo(hipX - tw * 0.32f, hipY)
            cubicTo(hipX - scale * 0.040f, hipY + scale * 0.058f, kXL + scale * 0.018f, kYL - scale * 0.038f, kXL, kYL)
            cubicTo(kXL - scale * 0.010f, kYL + scale * 0.040f, aXL - scale * 0.018f, aYL - scale * 0.040f, aXL, aYL)
        }, ink, style = stroke)

        // ── RIGHT LEG — stance / push-off ──
        val kXR = hipX + scale * 0.038f; val kYR = hipY + scale * 0.177f
        val aXR = kXR - scale * 0.024f; val aYR = kYR + scale * 0.180f
        drawPath(Path().apply {
            moveTo(hipX + tw * 0.32f, hipY)
            cubicTo(hipX + scale * 0.036f, hipY + scale * 0.064f, kXR + scale * 0.010f, kYR - scale * 0.038f, kXR, kYR)
            cubicTo(kXR - scale * 0.005f, kYR + scale * 0.042f, aXR + scale * 0.010f, aYR - scale * 0.048f, aXR, aYR)
        }, ink, style = stroke)

        // ── FOOT (stance foot flat on ground) ──
        drawPath(Path().apply {
            moveTo(aXR - scale * 0.005f, aYR)
            cubicTo(aXR + scale * 0.008f, aYR + scale * 0.012f, aXR + scale * 0.052f, aYR + scale * 0.010f, aXR + scale * 0.065f, aYR - scale * 0.002f)
        }, ink, style = stroke)
    }
}
