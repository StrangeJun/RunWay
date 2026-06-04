package com.runway.android.core.posture

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PosturePoseAnalyzer(private val context: Context) {

    suspend fun extractAngles(videoUri: Uri, targetFps: Int = 7): List<PostureFrameAngles> =
        withContext(Dispatchers.Default) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: return@withContext emptyList()

                val intervalMs = (1000L / targetFps).coerceAtLeast(100L)
                val landmarker = buildLandmarker()
                val frames = mutableListOf<PostureFrameAngles>()

                var timeMs = 0L
                while (timeMs < durationMs) {
                    val bitmap = retriever.getFrameAtTime(
                        timeMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    )
                    if (bitmap != null) {
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        val result = landmarker.detect(mpImage)
                        result.landmarks().firstOrNull()?.let { landmarkList ->
                            PostureAngleCalculator.compute(landmarkList)?.let { frames.add(it) }
                        }
                        bitmap.recycle()
                    }
                    timeMs += intervalMs
                }
                landmarker.close()
                frames
            } finally {
                retriever.release()
            }
        }

    private fun buildLandmarker(): PoseLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()
        return PoseLandmarker.createFromOptions(context, options)
    }
}
