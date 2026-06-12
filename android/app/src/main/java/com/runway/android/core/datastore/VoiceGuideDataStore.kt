package com.runway.android.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class VoiceGuideDataStore @Inject constructor(
    @Named("settingsDataStore") private val dataStore: DataStore<Preferences>,
) {
    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("voice_guide_enabled")
    }

    val enabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ENABLED] ?: true
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }
}
