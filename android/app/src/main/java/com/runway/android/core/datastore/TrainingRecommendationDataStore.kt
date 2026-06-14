package com.runway.android.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.runway.android.domain.training.TrainingGoal
import com.runway.android.domain.training.TrainingRecommendation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TrainingRecommendationDataStore @Inject constructor(
    @Named("trainingRecommendationDataStore") private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val RECOMMENDATION = stringPreferencesKey("recommendation_json")
        val GOAL_SIGNATURE = stringPreferencesKey("goal_signature")
        val WEEK_START = stringPreferencesKey("week_start")
        val OWNER_ID = stringPreferencesKey("owner_id")
    }

    private val gson = Gson()

    suspend fun getValid(
        ownerId: String,
        goal: TrainingGoal,
        today: LocalDate = LocalDate.now(),
    ): TrainingRecommendation? {
        val preferences = dataStore.data.first()
        if (preferences[Keys.OWNER_ID] != ownerId) return null
        if (preferences[Keys.GOAL_SIGNATURE] != goal.signature()) return null
        if (preferences[Keys.WEEK_START] != today.weekStart().toString()) return null
        return preferences[Keys.RECOMMENDATION]?.let { json ->
            runCatching { gson.fromJson(json, TrainingRecommendation::class.java) }.getOrNull()
        }
    }

    suspend fun save(
        ownerId: String,
        goal: TrainingGoal,
        recommendation: TrainingRecommendation,
        today: LocalDate = LocalDate.now(),
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.OWNER_ID] = ownerId
            preferences[Keys.GOAL_SIGNATURE] = goal.signature()
            preferences[Keys.WEEK_START] = today.weekStart().toString()
            preferences[Keys.RECOMMENDATION] = gson.toJson(recommendation)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun TrainingGoal.signature(): String = listOf(
        goalType.name,
        goalDistanceKm?.toString().orEmpty(),
        goalDate?.toString().orEmpty(),
        goalTimeSeconds?.toString().orEmpty(),
        preferredRunsPerWeek.toString(),
        availableDays.sorted().joinToString(","),
    ).joinToString("|")

    private fun LocalDate.weekStart(): LocalDate =
        with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
