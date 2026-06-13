package com.runway.android.ui.auth.passwordreset

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

enum class PasswordResetStep { EMAIL, CODE, PASSWORD }

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    var step by mutableStateOf(PasswordResetStep.EMAIL)
        private set
    var email by mutableStateOf("")
        private set
    var code by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var passwordConfirm by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    private var verificationToken: String? = null

    private val _completed = MutableSharedFlow<Unit>()
    val completed = _completed.asSharedFlow()

    fun onEmailChange(value: String) {
        email = value
        clearFeedback()
    }

    fun onCodeChange(value: String) {
        code = value.filter(Char::isDigit).take(6)
        clearFeedback()
    }

    fun onPasswordChange(value: String) {
        password = value
        clearFeedback()
    }

    fun onPasswordConfirmChange(value: String) {
        passwordConfirm = value
        clearFeedback()
    }

    fun requestCode() {
        if (!email.contains("@")) {
            error = "올바른 이메일을 입력해 주세요."
            return
        }
        launchRequest {
            when (val result = authRepository.requestPasswordResetCode(email.trim())) {
                is NetworkResult.Success -> {
                    step = PasswordResetStep.CODE
                    message = "가입된 이메일인 경우 인증번호를 발송했습니다."
                }
                is NetworkResult.ApiError -> error = result.message
                is NetworkResult.NetworkError -> error = "네트워크 오류가 발생했습니다."
            }
        }
    }

    fun verifyCode() {
        if (code.length != 6) {
            error = "6자리 인증번호를 입력해 주세요."
            return
        }
        launchRequest {
            when (val result = authRepository.verifyPasswordResetCode(email.trim(), code)) {
                is NetworkResult.Success -> {
                    verificationToken = result.data
                    step = PasswordResetStep.PASSWORD
                    message = "새 비밀번호를 설정해 주세요."
                }
                is NetworkResult.ApiError -> error = result.message
                is NetworkResult.NetworkError -> error = "네트워크 오류가 발생했습니다."
            }
        }
    }

    fun resetPassword() {
        val token = verificationToken ?: return
        if (password.length < 10) {
            error = "비밀번호는 10자 이상이어야 합니다."
            return
        }
        if (password != passwordConfirm) {
            error = "비밀번호가 일치하지 않습니다."
            return
        }
        launchRequest {
            when (val result = authRepository.resetPassword(token, password)) {
                is NetworkResult.Success -> _completed.emit(Unit)
                is NetworkResult.ApiError -> error = result.message
                is NetworkResult.NetworkError -> error = "네트워크 오류가 발생했습니다."
            }
        }
    }

    private fun launchRequest(block: suspend () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            clearFeedback()
            block()
            isLoading = false
        }
    }

    private fun clearFeedback() {
        error = null
        message = null
    }
}
