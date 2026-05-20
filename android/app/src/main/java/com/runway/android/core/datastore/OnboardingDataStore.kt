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
class OnboardingDataStore @Inject constructor(
    @Named("onboardingDataStore") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val isCompletedFlow: Flow<Boolean> = dataStore.data.map { it[KEY_COMPLETED] ?: false }

    suspend fun markCompleted() {
        dataStore.edit { it[KEY_COMPLETED] = true }
    }
}
