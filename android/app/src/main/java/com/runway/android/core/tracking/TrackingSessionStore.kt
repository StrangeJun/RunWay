package com.runway.android.core.tracking

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TrackingSessionStore @Inject constructor(
    @Named("trackingDataStore") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_RUNNING_RECORD_ID = stringPreferencesKey("running_record_id")
        private val KEY_COURSE_ATTEMPT_ID = stringPreferencesKey("course_attempt_id")
        private val KEY_ELAPSED_SECONDS = intPreferencesKey("elapsed_seconds")
        private val KEY_DISTANCE_METERS = floatPreferencesKey("distance_meters")
    }

    val snapshot: Flow<TrackingSessionSnapshot?> = dataStore.data.map { prefs ->
        val runningRecordId = prefs[KEY_RUNNING_RECORD_ID] ?: return@map null
        TrackingSessionSnapshot(
            runningRecordId = runningRecordId,
            courseAttemptId = prefs[KEY_COURSE_ATTEMPT_ID],
            elapsedSeconds = prefs[KEY_ELAPSED_SECONDS] ?: 0,
            distanceMeters = (prefs[KEY_DISTANCE_METERS] ?: 0f).toDouble(),
        )
    }

    suspend fun saveSnapshot(snapshot: TrackingSessionSnapshot) {
        dataStore.edit { prefs ->
            prefs[KEY_RUNNING_RECORD_ID] = snapshot.runningRecordId
            if (snapshot.courseAttemptId != null) {
                prefs[KEY_COURSE_ATTEMPT_ID] = snapshot.courseAttemptId
            } else {
                prefs.remove(KEY_COURSE_ATTEMPT_ID)
            }
            prefs[KEY_ELAPSED_SECONDS] = snapshot.elapsedSeconds
            prefs[KEY_DISTANCE_METERS] = snapshot.distanceMeters.toFloat()
        }
    }

    suspend fun clearSnapshot() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_RUNNING_RECORD_ID)
            prefs.remove(KEY_COURSE_ATTEMPT_ID)
            prefs.remove(KEY_ELAPSED_SECONDS)
            prefs.remove(KEY_DISTANCE_METERS)
        }
    }
}
