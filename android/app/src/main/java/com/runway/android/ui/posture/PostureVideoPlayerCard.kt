package com.runway.android.ui.posture

import android.net.Uri
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
import com.runway.android.core.posture.SKEL_L_ANKLE
import com.runway.android.core.posture.SKEL_L_ELBOW
import com.runway.android.core.posture.SKEL_L_HIP
import com.runway.android.core.posture.SKEL_L_KNEE
import com.runway.android.core.posture.SKEL_L_SHOULDER
import com.runway.android.core.posture.SKEL_L_WRIST
import com.runway.android.core.posture.SKEL_NOSE
import com.runway.android.core.posture.SKEL_R_ANKLE
import com.runway.android.core.posture.SKEL_R_ELBOW
import com.runway.android.core.posture.SKEL_R_HIP
import com.runway.android.core.posture.SKEL_R_KNEE
import com.runway.android.core.posture.SKEL_R_SHOULDER
import com.runway.android.core.posture.SKEL_R_WRIST
import com.runway.android.core.posture.PostureVideoFrame
import com.runway.android.core.posture.SkeletonPoint
import com.runway.android.core.posture.interpolateFrame
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.sqrt

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
    val preferredSide = remember(sortedFrames) { sortedFrames.preferredVisibleSide() }
    // Smoother keyed to videoFrames: if a different recording is loaded, prev state is cleared.
    val smoother = remember(videoFrames) { PostureSkeletonSmoother() }
    val legTracker = remember(videoFrames, preferredSide) {
        LockedLegTracker(trackLeftSide = preferredSide == VisibleSide.Left)
    }

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
        currentFrame = interpolated?.let {
            val smoothed = smoother.smooth(it.pts)
            it.copy(pts = legTracker.track(smoothed))
        }
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
                        useLeftSide = preferredSide == VisibleSide.Left,
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
                        legTracker.reset()
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
                                    legTracker.reset()
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
    useLeftSide: Boolean,
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

        val pts = frame.pts
        if (pts.isEmpty()) return@Canvas

        val hipMidX = if (pts.size > SKEL_R_HIP) {
            (pts[SKEL_L_HIP].x + pts[SKEL_R_HIP].x) / 2f
        } else {
            0.5f
        }
        val facingRight = pts.getOrNull(SKEL_NOSE)?.let { it.x > hipMidX } ?: true

        fun displayPoint(index: Int): Offset {
            val p = pts[index]
            val backwards = if (facingRight) -1f else 1f
            val correctionX = when (index) {
                SKEL_L_KNEE, SKEL_R_KNEE -> 0.026f * backwards
                SKEL_L_ANKLE, SKEL_R_ANKLE -> 0.038f * backwards
                else -> 0f
            }
            val x = (p.x + correctionX).coerceIn(0f, 1f)
            return Offset(x * videoWidth * scale + dx, p.y * videoHeight * scale + dy)
        }
        fun vis(p: SkeletonPoint) = p.v > 0.45f

        val leftColor = Color(0xFFFFB020)
        val rightColor = Color(0xFF00E676)
        val leftJoints = listOf(SKEL_L_SHOULDER, SKEL_L_ELBOW, SKEL_L_WRIST, SKEL_L_HIP, SKEL_L_KNEE, SKEL_L_ANKLE)
        val rightJoints = listOf(SKEL_R_SHOULDER, SKEL_R_ELBOW, SKEL_R_WRIST, SKEL_R_HIP, SKEL_R_KNEE, SKEL_R_ANKLE)

        fun drawEdges(edges: List<Pair<Int, Int>>, color: Color, highlighted: Boolean) {
            for ((a, b) in edges) {
                if (a >= pts.size || b >= pts.size) continue
                val pa = pts[a]; val pb = pts[b]
                if (!vis(pa) || !vis(pb)) continue
                drawLine(
                    color = Color(0xCC000000),
                    start = displayPoint(a),
                    end = displayPoint(b),
                    strokeWidth = if (highlighted) 6f else 4f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color.copy(alpha = if (highlighted) 0.95f else 0.68f),
                    start = displayPoint(a),
                    end = displayPoint(b),
                    strokeWidth = if (highlighted) 3.6f else 2.6f,
                    cap = StrokeCap.Round,
                )
            }
        }

        fun drawJoints(indices: List<Int>, color: Color, highlighted: Boolean) {
            for (idx in indices) {
                if (idx >= pts.size) continue
                val p = pts[idx]
                if (!vis(p)) continue
                drawCircle(
                    color = Color.White.copy(alpha = if (highlighted) 0.95f else 0.72f),
                    radius = if (highlighted) 6f else 5f,
                    center = displayPoint(idx),
                )
                drawCircle(
                    color = color,
                    radius = if (highlighted) 4f else 3.2f,
                    center = displayPoint(idx),
                )
            }
        }

        fun drawLegLabel(label: String, position: Offset?, color: Color) {
            if (position == null) return
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = android.graphics.Color.WHITE
                textSize = 13.dp.toPx()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color.toArgb()
            }
            drawContext.canvas.nativeCanvas.apply {
                drawCircle(position.x + 14.dp.toPx(), position.y - 14.dp.toPx(), 10.dp.toPx(), bgPaint)
                drawText(label, position.x + 14.dp.toPx(), position.y - 9.dp.toPx(), paint)
            }
        }

        for ((a, b) in SKELETON_EDGES_BILATERAL) {
            if (a >= pts.size || b >= pts.size) continue
            val pa = pts[a]; val pb = pts[b]
            if (!vis(pa) || !vis(pb)) continue
            drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = displayPoint(a),
                end = displayPoint(b),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round,
            )
        }

        drawEdges(SKELETON_EDGES_LEFT, leftColor, highlighted = useLeftSide)
        drawEdges(SKELETON_EDGES_RIGHT, rightColor, highlighted = !useLeftSide)
        drawJoints(leftJoints, leftColor, highlighted = useLeftSide)
        drawJoints(rightJoints, rightColor, highlighted = !useLeftSide)
        if (SKEL_NOSE < pts.size && vis(pts[SKEL_NOSE])) {
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 5f, center = displayPoint(SKEL_NOSE))
            drawCircle(color = Color(0xFFA4E168), radius = 3.2f, center = displayPoint(SKEL_NOSE))
        }

        drawLegLabel("L", pts.getOrNull(SKEL_L_ANKLE)?.takeIf(::vis)?.let { displayPoint(SKEL_L_ANKLE) }, leftColor)
        drawLegLabel("R", pts.getOrNull(SKEL_R_ANKLE)?.takeIf(::vis)?.let { displayPoint(SKEL_R_ANKLE) }, rightColor)
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private enum class VisibleSide { Left, Right }

private fun List<PostureVideoFrame>.preferredVisibleSide(): VisibleSide {
    var leftScore = 0f
    var rightScore = 0f
    var counted = 0

    for (frame in this) {
        val pts = frame.pts
        if (pts.size <= SKEL_R_ANKLE) continue
        leftScore += (
            pts[SKEL_L_SHOULDER].v +
                pts[SKEL_L_HIP].v +
                pts[SKEL_L_KNEE].v +
                pts[SKEL_L_ANKLE].v
            ) / 4f
        rightScore += (
            pts[SKEL_R_SHOULDER].v +
                pts[SKEL_R_HIP].v +
                pts[SKEL_R_KNEE].v +
                pts[SKEL_R_ANKLE].v
            ) / 4f
        counted++
    }

    if (counted == 0) return VisibleSide.Right
    return if (leftScore >= rightScore) VisibleSide.Left else VisibleSide.Right
}

private class LockedLegTracker(
    private val trackLeftSide: Boolean,
) {
    private var previousKnee: SkeletonPoint? = null
    private var previousAnkle: SkeletonPoint? = null
    private var kneeVelocityX = 0f
    private var kneeVelocityY = 0f
    private var ankleVelocityX = 0f
    private var ankleVelocityY = 0f

    fun track(points: List<SkeletonPoint>): List<SkeletonPoint> {
        if (points.size <= SKEL_R_ANKLE) return points

        val targetKneeIndex = if (trackLeftSide) SKEL_L_KNEE else SKEL_R_KNEE
        val targetAnkleIndex = if (trackLeftSide) SKEL_L_ANKLE else SKEL_R_ANKLE
        val targetHipIndex = if (trackLeftSide) SKEL_L_HIP else SKEL_R_HIP
        val targetShoulderIndex = if (trackLeftSide) SKEL_L_SHOULDER else SKEL_R_SHOULDER
        val hipMidX = (points[SKEL_L_HIP].x + points[SKEL_R_HIP].x) / 2f
        val facingRight = points[SKEL_NOSE].x > hipMidX

        val prevKnee = previousKnee
        val prevAnkle = previousAnkle
        if (prevKnee == null || prevAnkle == null) {
            previousKnee = points[targetKneeIndex]
            previousAnkle = points[targetAnkleIndex]
            return points
        }

        val predictedKnee = SkeletonPoint(
            x = (prevKnee.x + kneeVelocityX).coerceIn(0f, 1f),
            y = (prevKnee.y + kneeVelocityY).coerceIn(0f, 1f),
            v = prevKnee.v,
        )
        val predictedAnkle = SkeletonPoint(
            x = (prevAnkle.x + ankleVelocityX).coerceIn(0f, 1f),
            y = (prevAnkle.y + ankleVelocityY).coerceIn(0f, 1f),
            v = prevAnkle.v,
        )

        val selected = LegCandidate(
            knee = points[targetKneeIndex],
            ankle = points[targetAnkleIndex],
            hip = points[targetHipIndex],
        )
            .takeIf {
                it.isUsable(
                    predictedKnee = predictedKnee,
                    predictedAnkle = predictedAnkle,
                    previousAnkle = prevAnkle,
                    ankleVelocityX = ankleVelocityX,
                    facingRight = facingRight,
                )
            }
            ?: LegCandidate(predictedKnee, predictedAnkle, points[targetHipIndex])

        val stableKnee = blendTrackedPoint(prevKnee, selected.knee, alpha = 0.42f, maxStep = 0.16f)
        val stableAnkle = blendTrackedPoint(prevAnkle, selected.ankle, alpha = 0.68f, maxStep = 0.24f)

        kneeVelocityX = (stableKnee.x - prevKnee.x).coerceIn(-0.08f, 0.08f)
        kneeVelocityY = (stableKnee.y - prevKnee.y).coerceIn(-0.08f, 0.08f)
        ankleVelocityX = (stableAnkle.x - prevAnkle.x).coerceIn(-0.14f, 0.14f)
        ankleVelocityY = (stableAnkle.y - prevAnkle.y).coerceIn(-0.14f, 0.14f)

        previousKnee = stableKnee
        previousAnkle = stableAnkle

        return points.toMutableList().apply {
            this[targetKneeIndex] = stableKnee
            this[targetAnkleIndex] = stableAnkle
            this[targetHipIndex] = points[targetHipIndex]
            this[targetShoulderIndex] = points[targetShoulderIndex]
        }
    }

    fun reset() {
        previousKnee = null
        previousAnkle = null
        kneeVelocityX = 0f
        kneeVelocityY = 0f
        ankleVelocityX = 0f
        ankleVelocityY = 0f
    }

    private fun blendTrackedPoint(
        previous: SkeletonPoint,
        current: SkeletonPoint,
        alpha: Float,
        maxStep: Float,
    ): SkeletonPoint =
        SkeletonPoint(
            x = previous.x + (current.x - previous.x).coerceIn(-maxStep, maxStep) * alpha,
            y = previous.y + (current.y - previous.y).coerceIn(-maxStep, maxStep) * alpha,
            v = previous.v + (current.v - previous.v) * 0.4f,
        )
}

private data class LegCandidate(
    val knee: SkeletonPoint,
    val ankle: SkeletonPoint,
    val hip: SkeletonPoint,
) {
    fun isUsable(
        predictedKnee: SkeletonPoint,
        predictedAnkle: SkeletonPoint,
        previousAnkle: SkeletonPoint,
        ankleVelocityX: Float,
        facingRight: Boolean,
    ): Boolean {
        if (knee.v < 0.58f || ankle.v < 0.58f) return false
        if (distance(knee, predictedKnee) > 0.34f) return false
        if (distance(ankle, predictedAnkle) > 0.46f) return false
        if (movesAgainstBackwardStride(
                current = ankle,
                previous = previousAnkle,
                predicted = predictedAnkle,
                velocityX = ankleVelocityX,
                facingRight = facingRight,
            )
        ) {
            return false
        }
        val thigh = distance(hip, knee).coerceAtLeast(0.04f)
        val lowerLeg = distance(knee, ankle)
        return lowerLeg <= thigh * 2.35f
    }

    private fun movesAgainstBackwardStride(
        current: SkeletonPoint,
        previous: SkeletonPoint,
        predicted: SkeletonPoint,
        velocityX: Float,
        facingRight: Boolean,
    ): Boolean {
        val backwardSign = if (facingRight) -1f else 1f
        val velocityBackward = velocityX * backwardSign
        val predictedBackward = (predicted.x - previous.x) * backwardSign
        val currentBackward = (current.x - previous.x) * backwardSign

        return velocityBackward > 0.015f &&
            predictedBackward > 0f &&
            currentBackward < -0.07f
    }
}

private fun distance(a: SkeletonPoint, b: SkeletonPoint): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}
