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
    var passwordConfirm by mutableStateOf("")
        private set
    var nickname by mutableStateOf("")
        private set
    var isNicknameChecked by mutableStateOf(false)
        private set
    var isNicknameCheckLoading by mutableStateOf(false)
        private set
    var nicknameCheckMessage by mutableStateOf<String?>(null)
        private set
    var emailCode by mutableStateOf("")
        private set
    var isEmailCodeSent by mutableStateOf(false)
        private set
    var isEmailVerified by mutableStateOf(false)
        private set
    var isEmailVerificationLoading by mutableStateOf(false)
        private set
    var emailVerificationMessage by mutableStateOf<String?>(null)
        private set
    private var emailVerificationToken: String? = null
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // 약관 동의
    var agreeTerms by mutableStateOf(false)
        private set
    var agreePrivacy by mutableStateOf(false)
        private set
    var agreeMarketing by mutableStateOf(false)
        private set

    val agreeAll get() = agreeTerms && agreePrivacy && agreeMarketing
    val canSignup get() = agreeTerms && agreePrivacy

    fun toggleAll() {
        val next = !agreeAll
        agreeTerms = next; agreePrivacy = next; agreeMarketing = next
    }
    fun toggleTerms()     { agreeTerms = !agreeTerms }
    fun togglePrivacy()   { agreePrivacy = !agreePrivacy }
    fun toggleMarketing() { agreeMarketing = !agreeMarketing }

    private val _navigateToLogin = MutableSharedFlow<SignupCredentials>()
    val navigateToLogin = _navigateToLogin.asSharedFlow()

    fun onEmailChange(value: String) {
        email = value
        error = null
        isEmailVerified = false
        isEmailCodeSent = false
        emailCode = ""
        emailVerificationToken = null
        emailVerificationMessage = null
    }

    fun onEmailCodeChange(value: String) {
        emailCode = value.filter(Char::isDigit).take(6)
        error = null
    }

    fun onPasswordChange(value: String) {
        password = value.take(72)
        error = null
    }

    fun onPasswordConfirmChange(value: String) {
        passwordConfirm = value.take(72)
        error = null
    }

    fun onNicknameChange(value: String) {
        nickname = value.take(50)
        isNicknameChecked = false
        nicknameCheckMessage = null
        error = null
    }

    fun checkNickname() {
        val normalizedNickname = nickname.trim()
        if (normalizedNickname.length !in 2..50) {
            error = "닉네임은 2자 이상 50자 이하로 입력해 주세요."
            return
        }
        viewModelScope.launch {
            isNicknameCheckLoading = true
            error = null
            when (val result = authRepository.checkNickname(normalizedNickname)) {
                is NetworkResult.Success -> {
                    isNicknameChecked = result.data
                    nicknameCheckMessage = if (result.data) {
                        "사용 가능한 닉네임입니다."
                    } else {
                        "이미 사용 중인 닉네임입니다."
                    }
                }
                is NetworkResult.ApiError -> error = result.message
                is NetworkResult.NetworkError -> error = "네트워크 오류가 발생했습니다."
            }
            isNicknameCheckLoading = false
        }
    }

    fun requestEmailCode() {
        if (!email.contains("@")) {
            error = "올바른 이메일을 입력해 주세요."
            return
        }
        viewModelScope.launch {
            isEmailVerificationLoading = true
            error = null
            when (val result = authRepository.requestSignupEmailCode(email.trim())) {
                is NetworkResult.Success -> {
                    isEmailCodeSent = true
                    emailCode = ""
                    emailVerificationMessage = "인증번호를 이메일로 보냈습니다."
                }
                is NetworkResult.ApiError -> error = result.message
                is NetworkResult.NetworkError -> error = "네트워크 오류가 발생했습니다."
            }
            isEmailVerificationLoading = false
        }
    }

    fun verifyEmailCode() {
        if (emailCode.length != 6) {
            error = "6자리 인증번호를 입력해 주세요."
            return
        }
        viewModelScope.launch {
            isEmailVerificationLoading = true
            error = null
            when (
                val result = authRepository.verifySignupEmailCode(
                    email.trim(),
                    emailCode,
                )
            ) {
                is NetworkResult.Success -> {
                    emailVerificationToken = result.data
                    isEmailVerified = true
                    emailVerificationMessage = "이메일 인증이 완료되었습니다."
                }
                is NetworkResult.ApiError -> error = result.message
                is NetworkResult.NetworkError -> error = "네트워크 오류가 발생했습니다."
            }
            isEmailVerificationLoading = false
        }
    }

    fun signup() {
        if (email.isBlank() || password.isBlank() || nickname.isBlank()) {
            error = "모든 항목을 입력해 주세요."
            return
        }
        if (password.length < 10) {
            error = "비밀번호는 10자 이상이어야 합니다."
            return
        }
        if (password != passwordConfirm) {
            error = "비밀번호가 일치하지 않습니다."
            return
        }
        if (!isNicknameChecked) {
            error = "닉네임 중복 확인을 완료해 주세요."
            return
        }
        val verificationToken = emailVerificationToken
        if (!isEmailVerified || verificationToken == null) {
            error = "이메일 인증을 완료해 주세요."
            return
        }
        if (!agreeTerms || !agreePrivacy) {
            error = "필수 약관에 동의해 주세요."
            return
        }
        viewModelScope.launch {
            isLoading = true
            error = null
            when (
                val result = authRepository.signup(
                    email.trim(),
                    password,
                    nickname.trim(),
                    verificationToken,
                )
            ) {
                is NetworkResult.Success -> _navigateToLogin.emit(
                    SignupCredentials(email.trim(), password),
                )
                is NetworkResult.ApiError -> error = result.message
                is NetworkResult.NetworkError -> error = "네트워크 오류가 발생했습니다."
            }
            isLoading = false
        }
    }
}

data class SignupCredentials(
    val email: String,
    val password: String,
)
