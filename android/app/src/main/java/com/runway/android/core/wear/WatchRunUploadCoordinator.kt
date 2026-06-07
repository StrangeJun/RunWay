package com.runway.android.core.wear

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.Wearable
import com.runway.android.core.datastore.TokenDataStore
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.running.model.FinishRunRequest
import com.runway.android.data.running.model.RunPointRequest
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.data.running.model.StartRunRequest
import com.runway.android.data.attempt.model.FinishAttemptRequest
import com.runway.android.data.attempt.model.StartAttemptRequest
import com.runway.android.domain.attempt.CourseAttemptRepository
import com.runway.android.domain.running.RunningRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchRunUploadCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenDataStore: TokenDataStore,
    private val runningRepository: RunningRepository,
    private val courseAttemptRepository: CourseAttemptRepository,
) {
    private val preferences = context.getSharedPreferences(
        "watch_run_uploads",
        Context.MODE_PRIVATE,
    )

    suspend fun upload(payload: JSONObject, dataItemUri: Uri) {
        if (tokenDataStore.accessTokenFlow.first() == null) return

        val localId = payload.getString("localId")
        if (preferences.getBoolean(completedKey(localId), false)) {
            acknowledge(localId, dataItemUri)
            return
        }

        val courseId = payload.optString("courseId").takeUnless { it.isBlank() || it == "null" }
        val runId = preferences.getString(runIdKey(localId), null) ?: run {
            if (courseId != null) {
                when (val result = courseAttemptRepository.startAttempt(
                    courseId,
                    StartAttemptRequest(payload.getString("startedAt")),
                )) {
                    is NetworkResult.Success -> {
                        preferences.edit()
                            .putString(runIdKey(localId), result.data.runningRecordId)
                            .putString(attemptIdKey(localId), result.data.courseAttemptId)
                            .commit()
                        result.data.runningRecordId
                    }
                    else -> return
                }
            } else {
                when (val result = runningRepository.startRun(
                    StartRunRequest(payload.getString("startedAt")),
                )) {
                    is NetworkResult.Success -> result.data.runId.also {
                        preferences.edit().putString(runIdKey(localId), it).commit()
                    }
                    else -> return
                }
            }
        }

        val pointsJson = payload.optJSONArray("points")
        var offset = preferences.getInt(offsetKey(localId), 0)
        while (pointsJson != null && offset < pointsJson.length()) {
            val end = (offset + POINT_BATCH_SIZE).coerceAtMost(pointsJson.length())
            val points = (offset until end).map { index ->
                val point = pointsJson.getJSONObject(index)
                RunPointRequest(
                    sequence = point.getInt("sequence"),
                    latitude = point.getDouble("latitude"),
                    longitude = point.getDouble("longitude"),
                    altitudeMeters = point.getDouble("altitudeMeters"),
                    speedMps = point.getDouble("speedMps"),
                    recordedAt = point.getString("recordedAt"),
                )
            }
            if (runningRepository.savePoints(runId, SavePointsRequest(points)) !is NetworkResult.Success) {
                return
            }
            offset = end
            preferences.edit().putInt(offsetKey(localId), offset).commit()
        }

        val endedAt = payload.getString("endedAt")
        val distanceMeters = payload.getDouble("distanceMeters")
        val durationSeconds = payload.getInt("durationSeconds")
        val avgPace = payload.getInt("avgPaceSecondsPerKm")
        val calories = payload.getInt("caloriesBurned")
        val avgHeartRate = payload.optInt("avgHeartRateBpm").takeIf {
            payload.has("avgHeartRateBpm") && !payload.isNull("avgHeartRateBpm")
        }
        val finishResult = if (courseId != null) {
            val attemptId = preferences.getString(attemptIdKey(localId), null) ?: return
            courseAttemptRepository.finishAttempt(
                attemptId,
                FinishAttemptRequest(
                    endedAt = endedAt,
                    distanceMeters = distanceMeters,
                    durationSeconds = durationSeconds,
                    avgPaceSecondsPerKm = avgPace,
                    caloriesBurned = calories,
                    avgHeartRateBpm = avgHeartRate,
                ),
            )
        } else {
            runningRepository.finishRun(
                runId,
                FinishRunRequest(
                    endedAt = endedAt,
                    distanceMeters = distanceMeters,
                    durationSeconds = durationSeconds,
                    avgPaceSecondsPerKm = avgPace,
                    caloriesBurned = calories,
                    avgHeartRateBpm = avgHeartRate,
                ),
            )
        }
        if (finishResult !is NetworkResult.Success) return

        preferences.edit()
            .putBoolean(completedKey(localId), true)
            .remove(runIdKey(localId))
            .remove(offsetKey(localId))
            .remove(attemptIdKey(localId))
            .commit()
        acknowledge(localId, dataItemUri)
    }

    private suspend fun acknowledge(localId: String, dataItemUri: Uri) {
        Wearable.getDataClient(context).deleteDataItems(dataItemUri).await()
        val payload = JSONObject().put("localId", localId).toString().encodeToByteArray()
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        nodes.forEach { node ->
            Wearable.getMessageClient(context)
                .sendMessage(node.id, WatchCommandListenerService.RUN_UPLOAD_ACK_PATH, payload)
                .await()
        }
    }

    private fun runIdKey(localId: String) = "run_id_$localId"
    private fun offsetKey(localId: String) = "offset_$localId"
    private fun attemptIdKey(localId: String) = "attempt_id_$localId"
    private fun completedKey(localId: String) = "completed_$localId"

    private companion object {
        const val POINT_BATCH_SIZE = 50
    }
}
