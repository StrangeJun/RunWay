package com.runway.android.data.reminder

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ReminderPrefsRepository @Inject constructor(
    @Named("reminderDataStore") private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val HOUR = intPreferencesKey("hour")
        val MINUTE = intPreferencesKey("minute")
        val DAYS = stringPreferencesKey("days")
        val PLANS = stringPreferencesKey("plans")
    }

    fun getPrefs(): Flow<ReminderPrefs> = dataStore.data.map { prefs ->
        val days = prefs[Keys.DAYS]
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
        val plans = decodePlans(prefs[Keys.PLANS])
        ReminderPrefs(
            enabled = prefs[Keys.ENABLED] ?: false,
            hour = prefs[Keys.HOUR] ?: 7,
            minute = prefs[Keys.MINUTE] ?: 0,
            enabledDays = days,
            plans = plans,
        )
    }

    suspend fun save(prefs: ReminderPrefs) {
        dataStore.edit { p ->
            p[Keys.ENABLED] = prefs.enabled
            p[Keys.HOUR] = prefs.hour
            p[Keys.MINUTE] = prefs.minute
            p[Keys.DAYS] = prefs.enabledDays.joinToString(",")
            p[Keys.PLANS] = Gson().toJson(prefs.plans.values)
        }
    }

    private fun decodePlans(json: String?): Map<Int, DayRunningPlan> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            Gson()
                .fromJson(json, Array<DayRunningPlan>::class.java)
                .associateBy(DayRunningPlan::dayOfWeek)
        }.getOrDefault(emptyMap())
    }
}
