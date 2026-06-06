package com.runway.android.core.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.runway.android.core.tracking.RunTrackingState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchStateSender @Inject constructor(
    @ApplicationContext context: Context,
) {
    companion object {
        const val STATE_PATH = "/runway/watch/state"
    }

    private val nodeClient = Wearable.getNodeClient(context)
    private val messageClient = Wearable.getMessageClient(context)

    suspend fun sendState(
        state: RunTrackingState,
        runId: String?,
        status: String,
        goal: WatchRunGoal?,
        error: String? = null,
    ) {
        val payload = JSONObject()
            .put("status", status)
            .put("runId", runId)
            .put("elapsedSeconds", state.elapsedSeconds)
            .put("distanceMeters", state.distanceMeters)
            .put("paceMinPerKm", calculatePace(state))
            .put("cadenceSpm", state.cadenceSpm)
            .put("hasGpsFix", state.hasFirstFix)
            .put("isPaused", state.isPaused)
            .put("error", error)
            .apply { putGoal(goal) }
            .toString()
            .encodeToByteArray()

        runCatching {
            nodeClient.connectedNodes.await().forEach { node ->
                messageClient.sendMessage(node.id, STATE_PATH, payload).await()
            }
        }
    }

    private fun calculatePace(state: RunTrackingState): Double {
        if (state.distanceMeters < 20.0 || state.elapsedSeconds <= 0) return 0.0
        return state.elapsedSeconds / 60.0 / (state.distanceMeters / 1000.0)
    }

    private fun JSONObject.putGoal(goal: WatchRunGoal?) {
        when (goal) {
            null, WatchRunGoal.Free -> put("goalType", "FREE")
            is WatchRunGoal.Time -> {
                put("goalType", "TIME")
                put("targetMinutes", goal.targetMinutes)
            }
            is WatchRunGoal.Distance -> {
                put("goalType", "DISTANCE")
                put("targetMeters", goal.targetMeters)
            }
            is WatchRunGoal.Interval -> {
                put("goalType", "INTERVAL")
                put("workSeconds", goal.workSeconds)
                put("restSeconds", goal.restSeconds)
                put("sets", goal.sets)
            }
        }
    }
}
