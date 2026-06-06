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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.runway.android.core.posture.RunningFormStrikeDetector
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
import android.util.Log
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

private const val TAG_OVERLAY = "PostureOverlay"

// ── GaitKeeper colors ──────────────────────────────────────────────────────────
private val C_TORSO     = Color(0xFF3B82F6)  // blue
private val C_ARM       = Color(0xFF10B981)  // green
private val C_LEFT_LEG  = Color(0xFFF59E0B)  // amber
private val C_RIGHT_LEG = Color(0xFFEF4444)  // red
private val C_JOINT_BG  = Color(0xFF111827)  // dark fill
private const val SCORE_THRESH = 0.3f

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
    DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }

    val sortedFrames = remember(videoFrames) { videoFrames.sortedBy { it.t } }

    // running-form-analyzer FootStrikeDetector — one per ankle
    val strikeDetectorL = remember(videoFrames) { RunningFormStrikeDetector() }
    val strikeDetectorR = remember(videoFrames) { RunningFormStrikeDetector() }

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration        by remember { mutableStateOf(1L) }
    var isPlaying       by remember { mutableStateOf(false) }
    var isEnded         by remember { mutableStateOf(false) }
    var playbackSpeed   by remember { mutableFloatStateOf(1f) }

    var currentFrame    by remember { mutableStateOf<PostureVideoFrame?>(null) }
    var overlayAngles   by remember { mutableStateOf<OverlayAngles?>(null) }
    var strikeAgeL      by remember { mutableIntStateOf(Int.MAX_VALUE) }
    var strikeAgeR      by remember { mutableIntStateOf(Int.MAX_VALUE) }

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

    LaunchedEffect(currentPosition) {
        val interpolated = interpolateFrame(sortedFrames, currentPosition)
        currentFrame = interpolated?.let {
            // 분석 파이프라인에서 이미 LegSwapCorrector + One Euro Filter 가 완료되었으므로
            // 재생 시점에는 추가 post-processing 을 하지 않는다(이중 처리 시 0.25x 에서 큰 지연).
            val pts = it.pts

            val lAnkle = pts.getOrNull(SKEL_L_ANKLE)
            val rAnkle = pts.getOrNull(SKEL_R_ANKLE)
            if (lAnkle != null && strikeDetectorL.update(lAnkle, it.t)) strikeAgeL = 0
            if (rAnkle != null && strikeDetectorR.update(rAnkle, it.t)) strikeAgeR = 0
            strikeAgeL++; strikeAgeR++

            overlayAngles = computeOverlayAngles(pts, videoWidth, videoHeight)

            it
        }
    }

    LaunchedEffect(playbackSpeed) { exoPlayer.setPlaybackSpeed(playbackSpeed) }

    val textMeasurer = rememberTextMeasurer()

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
                        overlayAngles = overlayAngles,
                        strikeAgeL = strikeAgeL,
                        strikeAgeR = strikeAgeR,
                        textMeasurer = textMeasurer,
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
                        strikeDetectorL.reset(); strikeDetectorR.reset()
                        strikeAgeL = Int.MAX_VALUE; strikeAgeR = Int.MAX_VALUE
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
                                    strikeDetectorL.reset(); strikeDetectorR.reset()
                                    strikeAgeL = Int.MAX_VALUE; strikeAgeR = Int.MAX_VALUE
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

// ── Overlay angles computed from smoothed skeleton (running-form-analyzer port) ─

private data class OverlayAngles(
    val trunkAngle: Float?,
    val kneeAngleL: Float?,
    val kneeAngleR: Float?,
    val shankAngleL: Float?,
    val shankAngleR: Float?,
    val elbowAngle: Float?,
)

/**
 * 오버레이 각도 계산. [videoWidth]/[videoHeight] 로 aspect ratio 를 보정해
 * normalized x·y 좌표의 물리 비율 불일치를 제거한다.
 */
private fun computeOverlayAngles(
    pts: List<SkeletonPoint>,
    videoWidth: Int = 1,
    videoHeight: Int = 1,
): OverlayAngles {
    val ar = if (videoHeight > 0) videoWidth.toFloat() / videoHeight else 1f
    fun ok(idx: Int) = idx < pts.size && pts[idx].v >= SCORE_THRESH

    fun angle3(aIdx: Int, bIdx: Int, cIdx: Int): Float? {
        if (!ok(aIdx) || !ok(bIdx) || !ok(cIdx)) return null
        val a = pts[aIdx]; val b = pts[bIdx]; val c = pts[cIdx]
        val ax = (a.x - b.x) * ar; val ay = a.y - b.y
        val cx = (c.x - b.x) * ar; val cy = c.y - b.y
        val dot = ax * cx + ay * cy
        val mag = sqrt((ax*ax+ay*ay) * (cx*cx+cy*cy))
        if (mag < 1e-6f) return null
        return Math.toDegrees(acos((dot/mag).coerceIn(-1f, 1f)).toDouble()).toFloat()
    }

    fun shank(kneeIdx: Int, ankleIdx: Int): Float? {
        if (!ok(kneeIdx) || !ok(ankleIdx)) return null
        val k = pts[kneeIdx]; val a = pts[ankleIdx]
        val shinX = (a.x - k.x) * ar; val shinY = a.y - k.y
        val mag = sqrt(shinX*shinX + shinY*shinY)
        if (mag < 1e-6f) return null
        return Math.toDegrees(acos((shinY/mag).coerceIn(-1f, 1f)).toDouble()).toFloat()
    }

    val trunkAngle = run {
        if (!ok(SKEL_L_SHOULDER)||!ok(SKEL_R_SHOULDER)||!ok(SKEL_L_HIP)||!ok(SKEL_R_HIP)) null
        else {
            val dx = ((pts[SKEL_L_SHOULDER].x + pts[SKEL_R_SHOULDER].x)/2f -
                      (pts[SKEL_L_HIP].x      + pts[SKEL_R_HIP].x)     /2f) * ar
            val dy =  (pts[SKEL_L_HIP].y      + pts[SKEL_R_HIP].y)     /2f -
                      (pts[SKEL_L_SHOULDER].y  + pts[SKEL_R_SHOULDER].y)/2f
            if (sqrt(dx*dx+dy*dy) < 1e-6f) null
            else Math.toDegrees(Math.atan2(dx.toDouble(), dy.toDouble())).toFloat()
        }
    }

    return OverlayAngles(
        trunkAngle  = trunkAngle,
        kneeAngleL  = angle3(SKEL_L_HIP, SKEL_L_KNEE, SKEL_L_ANKLE),
        kneeAngleR  = angle3(SKEL_R_HIP, SKEL_R_KNEE, SKEL_R_ANKLE),
        shankAngleL = shank(SKEL_L_KNEE, SKEL_L_ANKLE),
        shankAngleR = shank(SKEL_R_KNEE, SKEL_R_ANKLE),
        elbowAngle  = run {
            val lv = if (pts.size > SKEL_L_ELBOW) pts[SKEL_L_ELBOW].v else 0f
            val rv = if (pts.size > SKEL_R_ELBOW) pts[SKEL_R_ELBOW].v else 0f
            if (lv >= rv) angle3(SKEL_L_SHOULDER, SKEL_L_ELBOW, SKEL_L_WRIST)
            else          angle3(SKEL_R_SHOULDER, SKEL_R_ELBOW, SKEL_R_WRIST)
        },
    )
}

// ── Skeleton overlay (GaitKeeper visual + running-form-analyzer angles) ────────

@Composable
private fun SkeletonOverlay(
    frame: PostureVideoFrame,
    videoWidth: Int,
    videoHeight: Int,
    overlayAngles: OverlayAngles?,
    strikeAgeL: Int,
    strikeAgeR: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val vAspect = videoWidth.toFloat() / videoHeight
        val cAspect = size.width / size.height
        val scale: Float; val dx: Float; val dy: Float
        if (vAspect > cAspect) {
            scale = size.width / videoWidth; dx = 0f
            dy = (size.height - videoHeight * scale) / 2f
            Log.d(TAG_OVERLAY, "overlay(letterbox-h): canvas=${size.width.toInt()}×${size.height.toInt()} " +
                "video=${videoWidth}×${videoHeight} scale=${"%.4f".format(scale)} " +
                "displayedRect=[0,${dy.toInt()},${size.width.toInt()},${(size.height-dy).toInt()}]")
        } else {
            scale = size.height / videoHeight; dy = 0f
            dx = (size.width - videoWidth * scale) / 2f
            Log.d(TAG_OVERLAY, "overlay(letterbox-v): canvas=${size.width.toInt()}×${size.height.toInt()} " +
                "video=${videoWidth}×${videoHeight} scale=${"%.4f".format(scale)} " +
                "displayedRect=[${dx.toInt()},0,${(size.width-dx).toInt()},${size.height.toInt()}]")
        }

        val pts = frame.pts
        if (pts.isEmpty()) return@Canvas

        fun toOff(idx: Int): Offset {
            val p = pts[idx]
            return Offset(p.x * videoWidth * scale + dx, p.y * videoHeight * scale + dy)
        }
        fun vis(idx: Int) = idx < pts.size && pts[idx].v >= SCORE_THRESH

        // ── Skeleton edges (GaitKeeper colors) ────────────────────────────────

        fun edge(a: Int, b: Int, color: Color, thick: Float = 3f) {
            if (!vis(a) || !vis(b)) return
            val oA = toOff(a); val oB = toOff(b)
            drawLine(Color.Black, oA, oB, thick + 3f, cap = StrokeCap.Round)
            drawLine(color,       oA, oB, thick,      cap = StrokeCap.Round)
        }

        fun joint(idx: Int, color: Color, r: Float = 6f) {
            if (!vis(idx)) return
            drawCircle(C_JOINT_BG, r,     toOff(idx))
            drawCircle(Color.White, r,    toOff(idx), style = Stroke(1.5f))
            drawCircle(color,      r-2f,  toOff(idx))
        }

        // Torso (blue)
        edge(SKEL_L_SHOULDER, SKEL_R_SHOULDER, C_TORSO)
        edge(SKEL_L_SHOULDER, SKEL_L_HIP, C_TORSO)
        edge(SKEL_R_SHOULDER, SKEL_R_HIP, C_TORSO)
        edge(SKEL_L_HIP,      SKEL_R_HIP, C_TORSO)

        // Arms (green)
        edge(SKEL_L_SHOULDER, SKEL_L_ELBOW, C_ARM)
        edge(SKEL_L_ELBOW,    SKEL_L_WRIST, C_ARM)
        edge(SKEL_R_SHOULDER, SKEL_R_ELBOW, C_ARM)
        edge(SKEL_R_ELBOW,    SKEL_R_WRIST, C_ARM)

        // Left leg (amber)
        edge(SKEL_L_HIP, SKEL_L_KNEE, C_LEFT_LEG, 4f)
        edge(SKEL_L_KNEE, SKEL_L_ANKLE, C_LEFT_LEG, 4f)

        // Right leg (red)
        edge(SKEL_R_HIP, SKEL_R_KNEE, C_RIGHT_LEG, 4f)
        edge(SKEL_R_KNEE, SKEL_R_ANKLE, C_RIGHT_LEG, 4f)

        // Joints
        listOf(SKEL_NOSE, SKEL_L_SHOULDER, SKEL_R_SHOULDER).forEach { joint(it, C_TORSO) }
        listOf(SKEL_L_ELBOW, SKEL_L_WRIST, SKEL_R_ELBOW, SKEL_R_WRIST).forEach { joint(it, C_ARM) }
        listOf(SKEL_L_HIP, SKEL_L_KNEE, SKEL_L_ANKLE).forEach { joint(it, C_LEFT_LEG) }
        listOf(SKEL_R_HIP, SKEL_R_KNEE, SKEL_R_ANKLE).forEach { joint(it, C_RIGHT_LEG) }

        // ── GaitKeeper-style strike arrows (running-form-analyzer triggers) ──
        val maxAge = 8
        if (vis(SKEL_L_ANKLE) && strikeAgeL < maxAge) {
            drawStrikeArrow(toOff(SKEL_L_ANKLE), C_LEFT_LEG, strikeAgeL.toFloat() / maxAge)
        }
        if (vis(SKEL_R_ANKLE) && strikeAgeR < maxAge) {
            drawStrikeArrow(toOff(SKEL_R_ANKLE), C_RIGHT_LEG, strikeAgeR.toFloat() / maxAge)
        }

        // ── running-form-analyzer angle overlay panel ─────────────────────────
        val angles = overlayAngles ?: return@Canvas
        drawAnglePanel(angles, textMeasurer)
    }
}

private fun DrawScope.drawStrikeArrow(ankle: Offset, color: Color, ageFraction: Float) {
    val alpha = (1f - ageFraction).coerceIn(0f, 1f)
    val arrowH = 36f; val arrowW = 18f
    val tipY = ankle.y - 8f
    // Shaft
    drawLine(
        color.copy(alpha = alpha),
        start = Offset(ankle.x, tipY - arrowH),
        end   = Offset(ankle.x, tipY - 10f),
        strokeWidth = 4f, cap = StrokeCap.Round,
    )
    // Downward triangle
    val path = Path().apply {
        moveTo(ankle.x, tipY)
        lineTo(ankle.x - arrowW / 2f, tipY - 14f)
        lineTo(ankle.x + arrowW / 2f, tipY - 14f)
        close()
    }
    drawPath(path, color.copy(alpha = alpha))
}

private fun DrawScope.drawAnglePanel(
    angles: OverlayAngles,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val lines = buildList {
        angles.trunkAngle?.let  { add("Trunk   ${it.fmt()}°") }
        angles.kneeAngleL?.let  { add("Knee L  ${it.fmt()}°") }
        angles.kneeAngleR?.let  { add("Knee R  ${it.fmt()}°") }
        angles.shankAngleL?.let { add("Shank L ${it.fmt()}°") }
        angles.shankAngleR?.let { add("Shank R ${it.fmt()}°") }
        angles.elbowAngle?.let  { add("Elbow   ${it.fmt()}°") }
    }
    if (lines.isEmpty()) return

    val style = TextStyle(fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)

    // Measure actual text dimensions so layout never overlaps
    val measured = lines.map { textMeasurer.measure(it, style) }
    val textH  = measured.first().size.height.toFloat()
    val lineH  = textH * 1.25f
    val padX   = 10f; val padY = 8f
    val panelW = (measured.maxOf { it.size.width.toFloat() }) + padX * 2
    val panelH = lines.size * lineH + padY * 2

    drawRect(
        Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(6f, 6f),
        size = Size(panelW, panelH),
    )

    measured.forEachIndexed { i, m ->
        drawText(m, topLeft = Offset(6f + padX, 6f + padY + i * lineH))
    }
}

private fun Float.fmt() = "%.0f".format(this)

// ── Speed chip ────────────────────────────────────────────────────────────────

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

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
