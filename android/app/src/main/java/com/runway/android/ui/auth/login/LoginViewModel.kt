package com.runway.android.ui.auth.login

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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val _navigateToHome = MutableSharedFlow<LoginCredentials?>()
    val navigateToHome = _navigateToHome.asSharedFlow()

    fun onEmailChange(value: String) {
        email = value
        error = null
    }

    fun onPasswordChange(value: String) {
        password = value
        error = null
    }

    fun fillCredentials(email: String, password: String) {
        this.email = email
        this.password = password
        error = null
    }

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            error = "이메일과 비밀번호를 입력해 주세요."
            return
        }
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = authRepository.login(email.trim(), password)) {
                is NetworkResult.Success -> _navigateToHome.emit(
                    LoginCredentials(email.trim(), password),
                )
                is NetworkResult.ApiError -> error = result.message
                is NetworkResult.NetworkError -> error = "네트워크 오류가 발생했습니다."
            }
            isLoading = false
        }
    }

    fun setGoogleError(message: String) { error = message }
    fun setCredentialError(message: String) { error = message }

    fun loginWithKakao(accessToken: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = authRepository.loginWithKakao(accessToken)) {
                is NetworkResult.Success -> _navigateToHome.emit(null)
                is NetworkResult.ApiError -> error = result.message ?: "카카오 로그인에 실패했습니다."
                is NetworkResult.NetworkError -> error = "네트워크 오류가 발생했습니다."
            }
            isLoading = false
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = authRepository.loginWithGoogle(idToken)) {
                is NetworkResult.Success -> _navigateToHome.emit(null)
                is NetworkResult.ApiError -> error = result.message ?: "Google 로그인에 실패했습니다."
                is NetworkResult.NetworkError -> error = "네트워크 오류가 발생했습니다."
            }
            isLoading = false
        }
    }
}

data class LoginCredentials(
    val email: String,
    val password: String,
)
