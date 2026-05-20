package com.runway.android.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.datastore.OnboardingDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingDataStore: OnboardingDataStore,
) : ViewModel() {

    var currentPage by mutableIntStateOf(0)
        private set

    private val _onComplete = MutableSharedFlow<Unit>()
    val onComplete = _onComplete.asSharedFlow()

    fun nextPage(totalPages: Int) {
        if (currentPage < totalPages - 1) {
            currentPage++
        } else {
            complete()
        }
    }

    fun complete() {
        viewModelScope.launch {
            onboardingDataStore.markCompleted()
            _onComplete.emit(Unit)
        }
    }
}
