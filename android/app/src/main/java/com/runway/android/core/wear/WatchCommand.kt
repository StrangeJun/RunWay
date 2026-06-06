package com.runway.android.core.wear

import com.google.gson.JsonParser

sealed interface WatchCommand {
    data object StartFreeRun : WatchCommand
    data class StartTimeGoalRun(val targetMinutes: Int) : WatchCommand
    data class StartDistanceGoalRun(val targetMeters: Int) : WatchCommand
    data class StartIntervalRun(
        val workSeconds: Int,
        val restSeconds: Int,
        val sets: Int,
    ) : WatchCommand
    data object PauseRun : WatchCommand
    data object ResumeRun : WatchCommand
    data object FinishRun : WatchCommand
    data object AbandonRun : WatchCommand
}

object WatchCommandParser {
    fun parse(payload: ByteArray): WatchCommand? = runCatching {
        val json = JsonParser.parseString(payload.decodeToString()).asJsonObject
        when (json.get("type").asString) {
            "START_FREE_RUN" -> WatchCommand.StartFreeRun
            "START_TIME_GOAL_RUN" -> WatchCommand.StartTimeGoalRun(
                targetMinutes = json.get("targetMinutes").asInt.coerceIn(1, 360),
            )
            "START_DISTANCE_GOAL_RUN" -> WatchCommand.StartDistanceGoalRun(
                targetMeters = json.get("targetMeters").asInt.coerceIn(500, 50_000),
            )
            "START_INTERVAL_RUN" -> WatchCommand.StartIntervalRun(
                workSeconds = json.get("workSeconds").asInt.coerceIn(60, 3_600),
                restSeconds = json.get("restSeconds").asInt.coerceIn(60, 1_800),
                sets = json.get("sets").asInt.coerceIn(1, 20),
            )
            "PAUSE_RUN" -> WatchCommand.PauseRun
            "RESUME_RUN" -> WatchCommand.ResumeRun
            "FINISH_RUN" -> WatchCommand.FinishRun
            "ABANDON_RUN" -> WatchCommand.AbandonRun
            else -> null
        }
    }.getOrNull()
}
