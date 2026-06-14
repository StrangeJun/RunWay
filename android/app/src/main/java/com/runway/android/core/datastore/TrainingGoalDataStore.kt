package com.runway.android.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.runway.android.domain.training.TrainingGoal
import com.runway.android.domain.training.TrainingGoalType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TrainingGoalDataStore @Inject constructor(
    @Named("settingsDataStore") private val dataStore: DataStore<Preferences>,
) {
    private companion object {
        val TYPE = stringPreferencesKey("training_goal_type")
        val DISTANCE = doublePreferencesKey("training_goal_distance_km")
        val DATE = stringPreferencesKey("training_goal_date")
        val TIME = intPreferencesKey("training_goal_time_seconds")
        val RUNS = intPreferencesKey("training_goal_runs_per_week")
        val DAYS = stringPreferencesKey("training_goal_available_days")
    }

    val goalFlow: Flow<TrainingGoal?> = dataStore.data.map { preferences ->
        val rawType = preferences[TYPE] ?: return@map null
        val type = runCatching { TrainingGoalType.valueOf(rawType) }.getOrNull()
            ?: return@map null
        TrainingGoal(
            goalType = type,
            goalDistanceKm = preferences[DISTANCE],
            goalDate = preferences[DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            goalTimeSeconds = preferences[TIME],
            preferredRunsPerWeek = preferences[RUNS] ?: 3,
            availableDays = preferences[DAYS]
                ?.split(",")
                ?.mapNotNull(String::toIntOrNull)
                ?.filter { it in 1..7 }
                ?.toSet()
                .orEmpty(),
        )
    }

    suspend fun save(goal: TrainingGoal) {
        dataStore.edit { preferences ->
            preferences[TYPE] = goal.goalType.name
            goal.goalDistanceKm?.let { preferences[DISTANCE] = it } ?: preferences.remove(DISTANCE)
            goal.goalDate?.let { preferences[DATE] = it.toString() } ?: preferences.remove(DATE)
            goal.goalTimeSeconds?.let { preferences[TIME] = it } ?: preferences.remove(TIME)
            preferences[RUNS] = goal.preferredRunsPerWeek
            preferences[DAYS] = goal.availableDays.sorted().joinToString(",")
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(TYPE)
            preferences.remove(DISTANCE)
            preferences.remove(DATE)
            preferences.remove(TIME)
            preferences.remove(RUNS)
            preferences.remove(DAYS)
        }
    }
}
