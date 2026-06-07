package com.runway.android.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.runway.android.ui.theme.AccentColor
import com.runway.android.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ThemeDataStore @Inject constructor(
    @Named("settingsDataStore") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val KEY_VOICE_NAME = stringPreferencesKey("voice_name")
    }

    val themeModeFlow: Flow<ThemeMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "SYSTEM" -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK
        }
    }

    val accentColorFlow: Flow<AccentColor> = dataStore.data.map { prefs ->
        runCatching {
            AccentColor.valueOf(prefs[KEY_ACCENT_COLOR] ?: AccentColor.GREEN.name)
        }.getOrDefault(AccentColor.GREEN)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(color: AccentColor) {
        dataStore.edit { it[KEY_ACCENT_COLOR] = color.name }
    }

    val voiceNameFlow: Flow<String?> = dataStore.data.map { it[KEY_VOICE_NAME] }

    suspend fun setVoiceName(name: String) {
        dataStore.edit { it[KEY_VOICE_NAME] = name }
    }
}
