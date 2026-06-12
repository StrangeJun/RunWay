package com.runway.wear.data

import android.content.Context
import com.runway.wear.model.GoalCompletionAction

data class WatchSettings(
    val voiceGuidanceEnabled: Boolean = true,
    val autoPauseEnabled: Boolean = true,
    val goalCompletionAction: GoalCompletionAction = GoalCompletionAction.PAUSE,
)

class WatchSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "runway_watch_settings",
        Context.MODE_PRIVATE,
    )

    fun load(): WatchSettings = WatchSettings(
        voiceGuidanceEnabled = preferences.getBoolean("voice_guidance", true),
        autoPauseEnabled = preferences.getBoolean("auto_pause", true),
        goalCompletionAction = preferences.getString("goal_completion", null)
            ?.let { runCatching { GoalCompletionAction.valueOf(it) }.getOrNull() }
            ?: GoalCompletionAction.PAUSE,
    )

    fun save(settings: WatchSettings) {
        preferences.edit()
            .putBoolean("voice_guidance", settings.voiceGuidanceEnabled)
            .putBoolean("auto_pause", settings.autoPauseEnabled)
            .putString("goal_completion", settings.goalCompletionAction.name)
            .apply()
    }
}
