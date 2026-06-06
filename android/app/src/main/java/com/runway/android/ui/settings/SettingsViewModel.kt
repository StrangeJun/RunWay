package com.runway.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.datastore.ThemeDataStore
import com.runway.android.ui.theme.AccentColor
import com.runway.android.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeDataStore: ThemeDataStore,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themeDataStore.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)

    val accentColor: StateFlow<AccentColor> = themeDataStore.accentColorFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentColor.GREEN)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeDataStore.setThemeMode(mode) }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch { themeDataStore.setAccentColor(color) }
    }
}
