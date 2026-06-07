package com.runway.wear.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PendingWatchRun(
    val localId: String,
    val courseId: String?,
    val startedAt: String,
    val endedAt: String,
    val distanceMeters: Double,
    val durationSeconds: Int,
    val avgPaceSecondsPerKm: Int,
    val caloriesBurned: Int,
    val avgHeartRateBpm: Int?,
    val points: List<WatchRunPoint>,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("localId", localId)
        .put("courseId", courseId)
        .put("startedAt", startedAt)
        .put("endedAt", endedAt)
        .put("distanceMeters", distanceMeters)
        .put("durationSeconds", durationSeconds)
        .put("avgPaceSecondsPerKm", avgPaceSecondsPerKm)
        .put("caloriesBurned", caloriesBurned)
        .put("avgHeartRateBpm", avgHeartRateBpm)
        .put("points", JSONArray().apply { points.forEach { put(it.toJson()) } })

    companion object {
        fun create(
            startedAt: String,
            courseId: String? = null,
            endedAt: String,
            distanceMeters: Double,
            durationSeconds: Int,
            avgPaceSecondsPerKm: Int,
            caloriesBurned: Int,
            avgHeartRateBpm: Int?,
            points: List<WatchRunPoint>,
        ) = PendingWatchRun(
            localId = UUID.randomUUID().toString(),
            courseId = courseId,
            startedAt = startedAt,
            endedAt = endedAt,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            avgPaceSecondsPerKm = avgPaceSecondsPerKm,
            caloriesBurned = caloriesBurned,
            avgHeartRateBpm = avgHeartRateBpm,
            points = points,
        )

        fun fromJson(json: JSONObject) = PendingWatchRun(
            localId = json.getString("localId"),
            courseId = json.optString("courseId").takeUnless { it.isBlank() || it == "null" },
            startedAt = json.getString("startedAt"),
            endedAt = json.getString("endedAt"),
            distanceMeters = json.getDouble("distanceMeters"),
            durationSeconds = json.getInt("durationSeconds"),
            avgPaceSecondsPerKm = json.getInt("avgPaceSecondsPerKm"),
            caloriesBurned = json.getInt("caloriesBurned"),
            avgHeartRateBpm = json.optInt("avgHeartRateBpm").takeIf {
                json.has("avgHeartRateBpm") && !json.isNull("avgHeartRateBpm")
            },
            points = json.optJSONArray("points")?.let { pointsJson ->
                buildList {
                    for (index in 0 until pointsJson.length()) {
                        add(WatchRunPoint.fromJson(pointsJson.getJSONObject(index)))
                    }
                }
            }.orEmpty(),
        )
    }
}

data class WatchRunPoint(
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val speedMps: Double,
    val recordedAt: String,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("sequence", sequence)
        .put("latitude", latitude)
        .put("longitude", longitude)
        .put("altitudeMeters", altitudeMeters)
        .put("speedMps", speedMps)
        .put("recordedAt", recordedAt)

    companion object {
        fun fromJson(json: JSONObject) = WatchRunPoint(
            sequence = json.getInt("sequence"),
            latitude = json.getDouble("latitude"),
            longitude = json.getDouble("longitude"),
            altitudeMeters = json.getDouble("altitudeMeters"),
            speedMps = json.getDouble("speedMps"),
            recordedAt = json.getString("recordedAt"),
        )
    }
}

class PendingWatchRunStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "pathfinder_pending_watch_runs",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun all(): List<PendingWatchRun> {
        val json = runCatching { JSONArray(preferences.getString(KEY_RUNS, "[]")) }
            .getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until json.length()) {
                runCatching { PendingWatchRun.fromJson(json.getJSONObject(index)) }
                    .getOrNull()
                    ?.let(::add)
            }
        }
    }

    @Synchronized
    fun add(run: PendingWatchRun) {
        save(all().filterNot { it.localId == run.localId } + run)
    }

    @Synchronized
    fun remove(localId: String) {
        save(all().filterNot { it.localId == localId })
    }

    private fun save(runs: List<PendingWatchRun>) {
        val json = JSONArray().apply { runs.forEach { put(it.toJson()) } }
        preferences.edit().putString(KEY_RUNS, json.toString()).commit()
    }

    private companion object {
        const val KEY_RUNS = "runs"
    }
}
