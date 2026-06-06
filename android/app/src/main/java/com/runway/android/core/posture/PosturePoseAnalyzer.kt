package com.runway.android.core.posture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "PostureAnalyzer"

data class PostureAnalysisOutput(
    val frames: List<PostureFrameAngles>,
    val videoFrames: List<PostureVideoFrame>,
    val videoPath: String?,
    val videoWidth: Int,
    val videoHeight: Int,
)

class PosturePoseAnalyzer(private val context: Context) {

    suspend fun extractAnalysis(videoUri: Uri, analysisId: String, targetFps: Int = 10): PostureAnalysisOutput =
        withContext(Dispatchers.Default) {
            val analysisStartedAt = SystemClock.elapsedRealtime()
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)

                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: return@withContext emptyOutput()

                val rawWidth  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rotation  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

                // Display dimensions: swap when the stored pixel data is 90°/270° rotated
                // relative to the display orientation. ExoPlayer applies the same swap
                // automatically, so videoWidth×videoHeight matches what the user sees.
                val (videoWidth, videoHeight) =
                    if (rotation == 90 || rotation == 270) rawHeight to rawWidth else rawWidth to rawHeight

                // BUG FIX: compute analysis dimensions from DISPLAY dimensions, not raw.
                //
                // On API 27+, MediaMetadataRetriever.getScaledFrameAtTime() automatically
                // applies METADATA_KEY_VIDEO_ROTATION before scaling (documented behavior).
                // If we passed raw dimensions (e.g. 512×288 landscape for a portrait video),
                // the function would rotate-then-scale the frame to fit within that landscape
                // box, returning a tiny 162×288 portrait bitmap instead of the expected 288×512.
                // Then applying rotationMatrix again caused a second rotation (double-rotation),
                // producing a 288×162 landscape blob that MediaPipe could not locate correctly.
                //
                // Fix: pass display-oriented dimensions (videoWidth, videoHeight) so the
                // auto-rotated result fills the analysis frame correctly.  Manual rotation is
                // still applied for the API 26 fallback path (getFrameAtTime does not rotate).
                val (analysisWidth, analysisHeight) = scaledAnalysisSize(videoWidth, videoHeight)
                // 각도 계산 시 x 성분을 물리 비율로 환산하기 위한 aspect ratio
                val aspectRatio = if (videoHeight > 0) videoWidth.toFloat() / videoHeight else 1f

                val sourceLabel = if (videoUri.scheme == "file") "app-recorded" else "gallery"
                Log.d(TAG, "=== Pose Analysis Start ===")
                Log.d(TAG, "source=$sourceLabel  uri.scheme=${videoUri.scheme}")
                Log.d(TAG, "rawWidth=$rawWidth  rawHeight=$rawHeight  rotationDegrees=$rotation")
                Log.d(TAG, "videoWidth=$videoWidth  videoHeight=$videoHeight  (display dims)")
                Log.d(TAG, "analysisWidth=$analysisWidth  analysisHeight=$analysisHeight  api=${Build.VERSION.SDK_INT}")

                val videoPath = saveVideo(videoUri, analysisId)

                val safeFps = targetFps.coerceIn(1, MAX_ANALYSIS_FPS)
                val requestedIntervalMs = 1000L / safeFps
                val boundedIntervalMs = (durationMs / MAX_ANALYSIS_FRAMES)
                    .coerceAtLeast(1L)
                val intervalMs = maxOf(requestedIntervalMs, boundedIntervalMs)

                // running-form-analyzer 방식 파이프라인:
                //   raw MediaPipe (RunningMode.VIDEO 내부 Kalman 포함)
                //   → One Euro Filter (경량 평활화, 재생 시 jitter 제거)
                //   → PostureAngleCalculator (pixel 좌표 기반, confidence >= 0.30)
                // LegSwapCorrector / PostureLandmarkCorrector 는 더 이상 사용하지 않는다.
                val landmarker  = buildLandmarker()
                val frames      = mutableListOf<PostureFrameAngles>()
                val videoFrames = mutableListOf<PostureVideoFrame>()
                val smoother    = PostureSkeletonSmoother()

                try {
                    var timeMs = 0L
                    while (
                        timeMs < durationMs &&
                        SystemClock.elapsedRealtime() - analysisStartedAt < ANALYSIS_TIME_BUDGET_MS
                    ) {
                        // analysisFrameAt handles rotation internally:
                        //   API 27+: getScaledFrameAtTime auto-applies rotation; result is
                        //            already in display orientation.
                        //   API 26:  getFrameAtTime + manual rotate + scale.
                        // Either way the returned bitmap is in display orientation — no
                        // additional rotation step is needed here.
                        val bitmap = retriever.analysisFrameAt(
                            timeUs = timeMs * 1000L,
                            displayWidth = analysisWidth,
                            displayHeight = analysisHeight,
                            rotation = rotation,
                        )

                        if (bitmap != null) {
                            val mpImage = BitmapImageBuilder(bitmap).build()
                            // detectForVideo() requires strictly increasing timestamps.
                            val result = landmarker.detectForVideo(mpImage, timeMs)
                            result.landmarks().firstOrNull()?.let { landmarkList ->
                                val rawPts = KEY_LANDMARK_INDICES.map { idx ->
                                    val lm = landmarkList[idx]
                                    SkeletonPoint(lm.x(), lm.y(), lm.visibility().orElse(0f))
                                }
                                // 가벼운 평활화 (재생 오버레이 jitter 제거)
                                val skeletonPts = smoother.smooth(rawPts, timeMs)
                                videoFrames.add(PostureVideoFrame(timeMs, skeletonPts))

                                PostureAngleCalculator.compute(skeletonPts, aspectRatio)?.let { angles ->
                                    frames.add(angles.copy(timestampMs = timeMs, landmarks = skeletonPts))
                                }
                            }
                            bitmap.recycle()
                        }
                        timeMs += intervalMs
                    }
                } finally {
                    landmarker.close()
                }

                Log.d(TAG, "=== Analysis Complete: ${frames.size} angle frames, ${videoFrames.size} video frames ===")
                PostureAnalysisOutput(frames, videoFrames, videoPath, videoWidth, videoHeight)
            } finally {
                retriever.release()
            }
        }

    /**
     * Extracts and returns a single analysis frame in **display orientation**
     * (i.e. after applying any rotation from the container metadata).
     *
     * [displayWidth] × [displayHeight]: target size in display (post-rotation) coordinates.
     * [rotation]: degrees from [MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION].
     *
     * On API 27+ ([Build.VERSION_CODES.O_MR1] and above) [getScaledFrameAtTime] applies the
     * container rotation automatically before scaling, so the caller must NOT rotate again.
     *
     * On API 26 [getFrameAtTime] returns the raw stored frame; rotation is applied here
     * manually before scaling.
     */
    private fun MediaMetadataRetriever.analysisFrameAt(
        timeUs: Long,
        displayWidth: Int,
        displayHeight: Int,
        rotation: Int,
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && displayWidth > 0 && displayHeight > 0) {
            // API 27+: getScaledFrameAtTime automatically applies METADATA_KEY_VIDEO_ROTATION
            // before scaling.  Passing display-oriented dimensions means the returned bitmap
            // is sized and oriented correctly for MediaPipe — no further rotation needed.
            return getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST,
                displayWidth,
                displayHeight,
            )
        }

        // API 26 fallback: getFrameAtTime returns raw stored orientation (no rotation applied).
        val source = getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null

        // Rotate to display orientation.
        val rotated = if (rotation != 0) {
            val m = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, m, true)
                .also { if (it !== source) source.recycle() }
        } else {
            source
        }

        // Scale to target analysis dimensions.
        if (displayWidth <= 0 || displayHeight <= 0 ||
            (rotated.width <= displayWidth && rotated.height <= displayHeight)
        ) {
            return rotated
        }
        return Bitmap.createScaledBitmap(rotated, displayWidth, displayHeight, true).also {
            if (it !== rotated) rotated.recycle()
        }
    }

    private fun scaledAnalysisSize(width: Int, height: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return width to height
        val longestSide = maxOf(width, height)
        if (longestSide <= MAX_ANALYSIS_DIMENSION) return width to height

        val scale = MAX_ANALYSIS_DIMENSION.toFloat() / longestSide
        return (width * scale).toInt().coerceAtLeast(1) to
            (height * scale).toInt().coerceAtLeast(1)
    }

    private fun saveVideo(videoUri: Uri, id: String): String? = runCatching {
        val dir  = File(context.filesDir, "posture").also { it.mkdirs() }
        val dest = File(dir, "$id.mp4")
        if (videoUri.scheme == "file") {
            File(videoUri.path!!).copyTo(dest, overwrite = true)
        } else {
            // error() throws inside runCatching, causing getOrNull() to return null
            // if the content resolver cannot open the stream.
            context.contentResolver.openInputStream(videoUri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Could not open input stream for $videoUri")
        }
        dest.absolutePath
    }.getOrNull()

    private fun buildLandmarker(): PoseLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)   // enables inter-frame Kalman tracking
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()
        return PoseLandmarker.createFromOptions(context, options)
    }

    private fun emptyOutput() = PostureAnalysisOutput(emptyList(), emptyList(), null, 0, 0)

    private companion object {
        const val MAX_ANALYSIS_FPS = 10
        const val MAX_ANALYSIS_FRAMES = 120
        const val MAX_ANALYSIS_DIMENSION = 512
        const val ANALYSIS_TIME_BUDGET_MS = 50_000L
    }
}
