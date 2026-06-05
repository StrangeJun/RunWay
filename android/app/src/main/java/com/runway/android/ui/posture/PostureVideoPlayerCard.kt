package com.runway.android.ui.posture

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.runway.android.core.posture.PostureSkeletonSmoother
import com.runway.android.core.posture.SKELETON_EDGES_BILATERAL
import com.runway.android.core.posture.SKELETON_EDGES_LEFT
import com.runway.android.core.posture.SKELETON_EDGES_RIGHT
import com.runway.android.core.posture.SKELETON_JOINT_INDICES
import com.runway.android.core.posture.SKEL_L_HIP
import com.runway.android.core.posture.SKEL_L_KNEE
import com.runway.android.core.posture.SKEL_L_SHOULDER
import com.runway.android.core.posture.SKEL_R_HIP
import com.runway.android.core.posture.SKEL_R_KNEE
import com.runway.android.core.posture.SKEL_R_SHOULDER
import com.runway.android.core.posture.PostureVideoFrame
import com.runway.android.core.posture.SkeletonPoint
import com.runway.android.core.posture.interpolateFrame
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun PostureVideoPlayerCard(
    videoPath: String,
    videoFrames: List<PostureVideoFrame>,
    videoWidth: Int,
    videoHeight: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val videoFile = remember(videoPath) { File(videoPath) }
    if (!videoFile.exists()) return

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(videoFile)))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    // Pre-sort frames once. Both sortedBy and the smoother are keyed to videoFrames
    // so they reset automatically if the data source changes.
    val sortedFrames = remember(videoFrames) { videoFrames.sortedBy { it.t } }
    // Smoother keyed to videoFrames: if a different recording is loaded, prev state is cleared.
    val smoother = remember(videoFrames) { PostureSkeletonSmoother() }

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration        by remember { mutableStateOf(1L) }
    var isPlaying       by remember { mutableStateOf(false) }
    var isEnded         by remember { mutableStateOf(false) }
    var playbackSpeed   by remember { mutableFloatStateOf(1f) }

    // currentFrame is updated as a side-effect (LaunchedEffect), NOT during composition,
    // so smoother.smooth() mutation and the interpolation are both Compose-safe.
    var currentFrame by remember { mutableStateOf<PostureVideoFrame?>(null) }

    // 80ms polling loop for playback position
    LaunchedEffect(exoPlayer) {
        while (true) {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration
            if (dur > 0) duration = dur
            isPlaying       = exoPlayer.isPlaying
            isEnded         = !exoPlayer.isPlaying && pos >= duration - 100
            currentPosition = pos
            delay(80)
        }
    }

    // Update skeleton frame whenever playback position changes.
    // Binary search interpolation + EMA smoothing happen here, outside composition.
    LaunchedEffect(currentPosition) {
        val interpolated = interpolateFrame(sortedFrames, currentPosition)
        currentFrame = interpolated?.copy(pts = smoother.smooth(interpolated.pts))
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Black,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.matchParentSize(),
                )

                val frame = currentFrame
                if (frame != null && videoWidth > 0 && videoHeight > 0) {
                    SkeletonOverlay(
                        frame = frame,
                        videoWidth = videoWidth,
                        videoHeight = videoHeight,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .background(Color(0xFF111111))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { frac ->
                        exoPlayer.seekTo((frac * duration).toLong())
                        smoother.reset()   // clear EMA state so no bleed across seek
                        isEnded = false
                    },
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = {
                            when {
                                isEnded -> {
                                    exoPlayer.seekTo(0)
                                    smoother.reset()
                                    exoPlayer.play()
                                    isEnded = false
                                }
                                isPlaying -> exoPlayer.pause()
                                else -> exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape),
                    ) {
                        Icon(
                            imageVector = when {
                                isEnded -> Icons.Filled.Replay
                                isPlaying -> Icons.Filled.Pause
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }

                    Text(
                        text = "${formatMs(currentPosition)} / ${formatMs(duration)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.25f to "0.25x", 0.5f to "0.5x", 1f to "1x").forEach { (speed, label) ->
                            SpeedChip(
                                label = label,
                                selected = playbackSpeed == speed,
                                onClick = { playbackSpeed = speed },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (selected) Color.Black else Color.White,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SkeletonOverlay(
    frame: PostureVideoFrame,
    videoWidth: Int,
    videoHeight: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val vAspect = videoWidth.toFloat() / videoHeight
        val cAspect = size.width / size.height

        val scale: Float; val dx: Float; val dy: Float
        if (vAspect > cAspect) {
            scale = size.width / videoWidth;  dx = 0f
            dy = (size.height - videoHeight * scale) / 2f
        } else {
            scale = size.height / videoHeight; dy = 0f
            dx = (size.width - videoWidth * scale) / 2f
        }

        fun pt(p: SkeletonPoint) = Offset(p.x * videoWidth * scale + dx, p.y * videoHeight * scale + dy)
        fun vis(p: SkeletonPoint) = p.v > 0.45f

        val pts = frame.pts
        if (pts.isEmpty()) return@Canvas

        fun safeVis(idx: Int) = if (idx < pts.size) pts[idx].v else 0f

        // Running video is always a side-profile shot. Draw only the side whose
        // key joints (shoulder, hip, knee) have higher average visibility — the
        // far side is estimated by MediaPipe and creates confusing crossed lines.
        val leftVis  = (safeVis(SKEL_L_SHOULDER) + safeVis(SKEL_L_HIP) + safeVis(SKEL_L_KNEE)) / 3f
        val rightVis = (safeVis(SKEL_R_SHOULDER) + safeVis(SKEL_R_HIP) + safeVis(SKEL_R_KNEE)) / 3f
        val sideEdges = if (leftVis >= rightVis) SKELETON_EDGES_LEFT else SKELETON_EDGES_RIGHT

        for ((a, b) in SKELETON_EDGES_BILATERAL + sideEdges) {
            if (a >= pts.size || b >= pts.size) continue
            val pa = pts[a]; val pb = pts[b]
            if (!vis(pa) || !vis(pb)) continue
            drawLine(
                color = Color(0xCCFFFFFF), start = pt(pa), end = pt(pb),
                strokeWidth = 3.5f, cap = StrokeCap.Round,
            )
        }

        for (idx in SKELETON_JOINT_INDICES) {
            if (idx >= pts.size) continue
            val p = pts[idx]
            if (!vis(p)) continue
            drawCircle(color = Color(0xFF00E676), radius = 6f, center = pt(p))
            drawCircle(color = Color.White,       radius = 3f, center = pt(p))
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
