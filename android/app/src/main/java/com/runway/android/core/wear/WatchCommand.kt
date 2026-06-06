package com.runway.android.core.wear

import com.google.gson.JsonParser

sealed interface WatchCommand {
    data object StartFreeRun : WatchCommand
    data class StartTimeGoalRun(
        val targetMinutes: Int,
        val completionAction: WatchGoalCompletionAction,
    ) : WatchCommand
    data class StartDistanceGoalRun(
        val targetMeters: Int,
        val completionAction: WatchGoalCompletionAction,
    ) : WatchCommand
    data class StartIntervalRun(
        val work: WatchIntervalTarget,
        val recovery: WatchIntervalTarget,
        val sets: Int,
        val completionAction: WatchGoalCompletionAction,
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
                completionAction = json.completionAction(),
            )
            "START_DISTANCE_GOAL_RUN" -> WatchCommand.StartDistanceGoalRun(
                targetMeters = json.get("targetMeters").asInt.coerceIn(500, 50_000),
                completionAction = json.completionAction(),
            )
            "START_INTERVAL_RUN" -> WatchCommand.StartIntervalRun(
                work = json.intervalTarget("work", legacySecondsKey = "workSeconds"),
                recovery = json.intervalTarget("recovery", legacySecondsKey = "restSeconds"),
                sets = json.get("sets").asInt.coerceIn(1, 20),
                completionAction = json.completionAction(),
            )
            "PAUSE_RUN" -> WatchCommand.PauseRun
            "RESUME_RUN" -> WatchCommand.ResumeRun
            "FINISH_RUN" -> WatchCommand.FinishRun
            "ABANDON_RUN" -> WatchCommand.AbandonRun
            else -> null
        }
    }.getOrNull()

    private fun com.google.gson.JsonObject.completionAction(): WatchGoalCompletionAction =
        get("completionAction")?.asString
            ?.let { runCatching { WatchGoalCompletionAction.valueOf(it) }.getOrNull() }
            ?: WatchGoalCompletionAction.CONTINUE

    private fun com.google.gson.JsonObject.intervalTarget(
        prefix: String,
        legacySecondsKey: String,
    ): WatchIntervalTarget {
        return when (get("${prefix}Type")?.asString) {
            "DISTANCE" -> WatchIntervalTarget.Distance(
                get("${prefix}Meters").asInt.coerceIn(100, 50_000),
            )
            else -> WatchIntervalTarget.Time(
                (get("${prefix}Seconds") ?: get(legacySecondsKey)).asInt.coerceIn(10, 3_600),
            )
        }
    }
}
