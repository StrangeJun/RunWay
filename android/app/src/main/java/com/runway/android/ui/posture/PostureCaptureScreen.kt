package com.runway.android.ui.posture

import android.Manifest
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.runway.android.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_RECORDING_SECONDS = 15
private const val MIN_RECORDING_SECONDS = 10

@Composable
fun PostureCaptureScreen(
    onVideoReady: (Uri) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showGuide by remember { mutableStateOf(true) }
    var countdownValue by remember { mutableStateOf<Int?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var hasFrontCamera by remember { mutableStateOf(false) }
    var isCameraReady by remember { mutableStateOf(false) }

    val toneGen = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }
    DisposableEffect(Unit) { onDispose { toneGen.release() } }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Auto-stop timer when recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (isRecording && elapsedSeconds < MAX_RECORDING_SECONDS) {
                delay(1000)
                elapsedSeconds++
            }
            if (isRecording) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                activeRecording?.stop()
            }
        }
    }

    val previewView = remember { PreviewView(context) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }

    if (hasCameraPermission) {
        DisposableEffect(lifecycleOwner, lensFacing) {
            isCameraReady = false
            videoCapture = null
            val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                hasFrontCamera = runCatching {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                }.getOrDefault(false)
                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                val preview = androidx.camera.core.Preview.Builder().build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                val capture = VideoCapture.withOutput(recorder)
                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        capture,
                    )
                }.onSuccess {
                    videoCapture = capture
                    isCameraReady = true
                }.onFailure {
                    if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        lensFacing = CameraSelector.LENS_FACING_BACK
                    }
                }
            }, ContextCompat.getMainExecutor(context))
            onDispose {
                runCatching {
                    androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context).get().unbindAll()
                }
            }
        }
    }

    // Starts countdown then recording
    fun startCountdownAndRecord() {
        val vc = videoCapture ?: return
        scope.launch {
            showGuide = false
            for (i in 5 downTo 1) {
                countdownValue = i
                delay(1000)
            }
            countdownValue = null

            // Start beep
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)

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
                            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                            isRecording = false
                            if (!event.hasError()) onVideoReady(Uri.fromFile(videoFile))
                            else videoFile.delete()
                        }
                        else -> Unit
                    }
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }

        // Silhouette guide (hide while recording)
        if (showGuide && countdownValue == null && !isRecording) {
            PostureSilhouetteGuide(modifier = Modifier.fillMaxSize())
        }

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
        }

        if (hasCameraPermission && hasFrontCamera) {
            val canSwitchCamera = !isRecording && countdownValue == null
            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                enabled = canSwitchCamera,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        "후면 카메라로 전환"
                    } else {
                        "셀카 카메라로 전환"
                    },
                    tint = Color.White.copy(alpha = if (canSwitchCamera) 1f else 0.45f),
                )
            }
        }

        // Countdown overlay
        AnimatedContent(
            targetState = countdownValue,
            modifier = Modifier.align(Alignment.Center),
            transitionSpec = {
                (scaleIn(initialScale = 2.2f, animationSpec = tween(200)) +
                        fadeIn(animationSpec = tween(150))) togetherWith
                        (scaleOut(targetScale = 0.4f, animationSpec = tween(300)) +
                                fadeOut(animationSpec = tween(200)))
            },
            label = "countdown",
        ) { value ->
            if (value != null) {
                Text(
                    text = value.toString(),
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Guide phase — show instructions + 분석 시작 button
            if (showGuide && countdownValue == null && !isRecording) {
                Text(
                    "카메라를 옆면 허리 높이에 두고\n전신이 화면에 들어오도록 맞추세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { startCountdownAndRecord() },
                    enabled = isCameraReady,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = if (isCameraReady) 0.85f else 0.35f,
                            ),
                            MaterialTheme.shapes.extraLarge,
                        )
                        .padding(horizontal = 8.dp),
                ) {
                    Text("분석 시작", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            // Recording phase
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
                    onClick = {
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                        activeRecording?.stop()
                    },
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
    Box(modifier = modifier.background(Color.Black.copy(alpha = 0.28f))) {
        Image(
            painter = painterResource(R.drawable.runner_silhouette_guide),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        )
    }
}
