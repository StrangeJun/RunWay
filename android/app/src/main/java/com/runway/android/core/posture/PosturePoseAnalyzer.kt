package com.runway.android.core.posture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PostureAnalysisOutput(
    val frames: List<PostureFrameAngles>,
    val videoFrames: List<PostureVideoFrame>,
    val videoPath: String?,
    val videoWidth: Int,
    val videoHeight: Int,
)

class PosturePoseAnalyzer(private val context: Context) {

    suspend fun extractAnalysis(videoUri: Uri, analysisId: String, targetFps: Int = 15): PostureAnalysisOutput =
        withContext(Dispatchers.Default) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)

                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: return@withContext emptyOutput()

                val rawWidth  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rotation  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

                // Swap dimensions when the video is rotated 90°/270° so the overlay
                // coordinate space matches what ExoPlayer shows on screen.
                val (videoWidth, videoHeight) =
                    if (rotation == 90 || rotation == 270) rawHeight to rawWidth else rawWidth to rawHeight

                val videoPath = saveVideo(videoUri, analysisId)

                val safeFps    = targetFps.coerceAtLeast(1)
                val intervalMs = 1000L / safeFps
                val rotationMatrix = if (rotation != 0) Matrix().apply { postRotate(rotation.toFloat()) } else null

                // RunningMode.VIDEO enables inter-frame Kalman tracking so landmark
                // positions are stabilised across the frame sequence.
                val landmarker  = buildLandmarker()
                val frames      = mutableListOf<PostureFrameAngles>()
                val videoFrames = mutableListOf<PostureVideoFrame>()

                try {
                    var timeMs = 0L
                    while (timeMs < durationMs) {
                        val rawBitmap: Bitmap? = retriever.getFrameAtTime(
                            timeMs * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST,
                        )

                        if (rawBitmap != null) {
                            val bitmap = if (rotationMatrix != null) {
                                Bitmap.createBitmap(
                                    rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, rotationMatrix, true,
                                ).also { rawBitmap.recycle() }
                            } else rawBitmap

                            val mpImage = BitmapImageBuilder(bitmap).build()
                            // detectForVideo() requires strictly increasing timestamps.
                            val result = landmarker.detectForVideo(mpImage, timeMs)
                            result.landmarks().firstOrNull()?.let { landmarkList ->
                                val skeletonPts = KEY_LANDMARK_INDICES.map { idx ->
                                    val lm = landmarkList[idx]
                                    SkeletonPoint(lm.x(), lm.y(), lm.visibility().orElse(0f))
                                }
                                videoFrames.add(PostureVideoFrame(timeMs, skeletonPts))

                                PostureAngleCalculator.compute(landmarkList)?.let { angles ->
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

                PostureAnalysisOutput(frames, videoFrames, videoPath, videoWidth, videoHeight)
            } finally {
                retriever.release()
            }
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
}
