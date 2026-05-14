package com.runway.android.ui.auth.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var nickname by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val _navigateToLogin = MutableSharedFlow<Unit>()
    val navigateToLogin = _navigateToLogin.asSharedFlow()

    fun onEmailChange(value: String) {
        email = value
        error = null
    }

    fun onPasswordChange(value: String) {
        password = value
        error = null
    }

    fun onNicknameChange(value: String) {
        nickname = value
        error = null
    }

    fun signup() {
        if (email.isBlank() || password.isBlank() || nickname.isBlank()) {
            error = "모든 항목을 입력해 주세요."
            return
        }
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = authRepository.signup(email.trim(), password, nickname.trim())) {
                is NetworkResult.Success -> _navigateToLogin.emit(Unit)
                is NetworkResult.ApiError -> error = result.message
                is NetworkResult.NetworkError -> error = "네트워크 오류가 발생했습니다."
            }
            isLoading = false
        }
    }
}
