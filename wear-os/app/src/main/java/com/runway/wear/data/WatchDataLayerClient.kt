package com.runway.wear.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.runway.wear.model.GoalCompletionAction
import com.runway.wear.model.IntervalTarget
import com.runway.wear.model.RunGoal
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

object WatchPaths {
    const val COMMAND = "/runway/watch/command"
    const val STATE = "/runway/watch/state"
    const val COURSE_START = "/runway/watch/course-start"
    const val AUTH_REQUEST = "/runway/watch/auth/request"
    const val AUTH_STATE = "/runway/watch/auth/state"
    const val RUN_UPLOAD = "/runway/watch/run-upload"
    const val RUN_UPLOAD_ACK = "/runway/watch/run-upload-ack"
    const val COURSE_REQUEST = "/runway/watch/course-request"
    const val COURSE_CATALOG = "/runway/watch/course-catalog"
}

class WatchDataLayerClient(context: Context) {
    private val applicationContext = context.applicationContext
    private val nodeClient = Wearable.getNodeClient(applicationContext)
    private val messageClient = Wearable.getMessageClient(applicationContext)
    private val dataClient = Wearable.getDataClient(applicationContext)

    suspend fun isPhoneConnected(): Boolean = runCatching {
        nodeClient.connectedNodes.await().isNotEmpty()
    }.getOrDefault(false)

    suspend fun requestAuthState(): Boolean = sendMessage(
        path = WatchPaths.AUTH_REQUEST,
        payload = ByteArray(0),
    )

    suspend fun sendStart(goal: RunGoal): Boolean = sendCommand(
        when (goal) {
            RunGoal.Free -> JSONObject().put("type", "START_FREE_RUN")
            is RunGoal.Time -> JSONObject()
                .put("type", "START_TIME_GOAL_RUN")
                .put("targetMinutes", goal.minutes)
                .putCompletionAction(goal.completionAction)
            is RunGoal.Distance -> JSONObject()
                .put("type", "START_DISTANCE_GOAL_RUN")
                .put("targetMeters", goal.meters)
                .putCompletionAction(goal.completionAction)
            is RunGoal.Interval -> JSONObject()
                .put("type", "START_INTERVAL_RUN")
                .put("sets", goal.sets)
                .putTarget("work", goal.work)
                .putTarget("recovery", goal.recovery)
                .putCompletionAction(goal.completionAction)
        },
    )

    suspend fun pause(): Boolean = sendCommand(JSONObject().put("type", "PAUSE_RUN"))
    suspend fun resume(): Boolean = sendCommand(JSONObject().put("type", "RESUME_RUN"))
    suspend fun finish(): Boolean = sendCommand(JSONObject().put("type", "FINISH_RUN"))
    suspend fun abandon(): Boolean = sendCommand(JSONObject().put("type", "ABANDON_RUN"))

    suspend fun requestNearbyCourses(latitude: Double, longitude: Double): Boolean = sendMessage(
        WatchPaths.COURSE_REQUEST,
        JSONObject()
            .put("latitude", latitude)
            .put("longitude", longitude)
            .toString()
            .encodeToByteArray(),
    )

    suspend fun syncPendingRuns(store: PendingWatchRunStore): Int {
        if (!isPhoneConnected()) return 0
        var sent = 0
        store.all().forEach { run ->
            val request = PutDataMapRequest.create("${WatchPaths.RUN_UPLOAD}/${run.localId}").apply {
                dataMap.putString("localId", run.localId)
                dataMap.putAsset("run", Asset.createFromBytes(run.toJson().toString().encodeToByteArray()))
            }.asPutDataRequest().setUrgent()
            if (runCatching { dataClient.putDataItem(request).await() }.isSuccess) {
                sent++
            }
        }
        return sent
    }

    private suspend fun sendCommand(payload: JSONObject): Boolean =
        sendMessage(WatchPaths.COMMAND, payload.toString().encodeToByteArray())

    private suspend fun sendMessage(path: String, payload: ByteArray): Boolean = runCatching {
        val nodes = nodeClient.connectedNodes.await()
        if (nodes.isEmpty()) return false
        nodes.forEach { node ->
            messageClient.sendMessage(
                node.id,
                path,
                payload,
            ).await()
        }
        true
    }.getOrDefault(false)

    private fun JSONObject.putCompletionAction(action: GoalCompletionAction): JSONObject =
        put("completionAction", action.name)

    private fun JSONObject.putTarget(prefix: String, target: IntervalTarget): JSONObject {
        return when (target) {
            is IntervalTarget.Time -> put("${prefix}Type", "TIME")
                .put("${prefix}Seconds", target.seconds)
            is IntervalTarget.Distance -> put("${prefix}Type", "DISTANCE")
                .put("${prefix}Meters", target.meters)
        }
    }
}
