package com.runway.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean?> = authRepository.isLoggedInFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
