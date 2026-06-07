package com.runway.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.datastore.ThemeDataStore
import com.runway.android.core.voice.RunningVoiceGuide
import com.runway.android.core.voice.VoiceOption
import com.runway.android.ui.theme.AccentColor
import com.runway.android.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeDataStore: ThemeDataStore,
    private val voiceGuide: RunningVoiceGuide,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themeDataStore.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)

    val accentColor: StateFlow<AccentColor> = themeDataStore.accentColorFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentColor.GREEN)

    val selectedVoiceName: StateFlow<String?> = themeDataStore.voiceNameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val availableVoices: List<VoiceOption>
        get() = voiceGuide.availableVoices

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeDataStore.setThemeMode(mode) }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch { themeDataStore.setAccentColor(color) }
    }

    fun selectVoice(voiceName: String) {
        voiceGuide.applyVoice(voiceName)
        viewModelScope.launch { themeDataStore.setVoiceName(voiceName) }
    }

    fun previewVoice(voiceName: String) {
        voiceGuide.applyVoice(voiceName)
        voiceGuide.preview()
    }
}
