package com.runway.android.ui.profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.running.model.PersonalRecordsResponse
import com.runway.android.data.user.model.UpdateProfileRequest
import com.runway.android.domain.auth.AuthRepository
import com.runway.android.domain.running.RunningRepository
import com.runway.android.domain.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileStats(
    val totalRuns: Long,
    val totalDistanceKm: String,
    val currentStreakDays: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val runningRepository: RunningRepository,
) : ViewModel() {

    var nickname by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var bio by mutableStateOf("")
        private set
    var profileImageUrl by mutableStateOf<String?>(null)
        private set
    var stats by mutableStateOf<ProfileStats?>(null)
        private set
    var personalRecords by mutableStateOf<PersonalRecordsResponse?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var profileError by mutableStateOf(false)
        private set

    // 편집 상태
    var isEditing by mutableStateOf(false)
        private set
    var editNickname by mutableStateOf("")
        private set
    var editBio by mutableStateOf("")
        private set
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    init {
        loadProfile()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            doLoad()
            isRefreshing = false
        }
    }

    private fun loadProfile() {
        viewModelScope.launch { doLoad() }
    }

    private suspend fun doLoad() = kotlinx.coroutines.coroutineScope {
            val profileDeferred = async { userRepository.getMe() }
            val recordsDeferred = async { runningRepository.getPersonalRecords() }
            val statsDeferred = async { runningRepository.getRunningStats("all") }

            val profileResult = profileDeferred.await()
            val recordsResult = recordsDeferred.await()
            val statsResult = statsDeferred.await()

            if (profileResult is NetworkResult.Success) {
                nickname = profileResult.data.nickname
                email = profileResult.data.email
                bio = profileResult.data.bio ?: ""
                profileImageUrl = profileResult.data.profileImageUrl
            } else {
                profileError = true
            }

            val currentStreak = if (statsResult is NetworkResult.Success) statsResult.data.currentStreakDays else 0

            if (recordsResult is NetworkResult.Success) {
                val r = recordsResult.data
                stats = ProfileStats(
                    totalRuns = r.totalCompletedRuns,
                    totalDistanceKm = "%.1f".format(r.totalDistanceMeters / 1000.0),
                    currentStreakDays = currentStreak,
                )
                personalRecords = r
            } else {
                stats = ProfileStats(totalRuns = 0L, totalDistanceKm = "0.0", currentStreakDays = currentStreak)
            }

            isLoading = false
    }

    fun startEditing() {
        editNickname = nickname
        editBio = bio
        selectedImageUri = null
        saveError = null
        isEditing = true
    }

    fun cancelEditing() {
        isEditing = false
        selectedImageUri = null
        saveError = null
    }

    fun updateEditNickname(value: String) { editNickname = value }
    fun updateEditBio(value: String) { editBio = value }
    fun onImageSelected(uri: Uri?) { selectedImageUri = uri }

    fun saveProfile() {
        val trimmedNickname = editNickname.trim()
        if (trimmedNickname.isBlank()) {
            saveError = "닉네임을 입력해 주세요."
            return
        }
        viewModelScope.launch {
            isSaving = true
            saveError = null
            when (val result = userRepository.updateMe(
                UpdateProfileRequest(
                    nickname = trimmedNickname,
                    profileImageUrl = null,
                    bio = editBio.trim().ifEmpty { null },
                )
            )) {
                is NetworkResult.Success -> {
                    nickname = result.data.nickname
                    bio = result.data.bio ?: ""
                    profileImageUrl = result.data.profileImageUrl
                    selectedImageUri = null
                    isEditing = false
                }
                is NetworkResult.ApiError -> saveError = when (result.errorCode) {
                    "DUPLICATED_NICKNAME" -> "이미 사용 중인 닉네임입니다."
                    else -> "저장에 실패했습니다."
                }
                is NetworkResult.NetworkError -> saveError = "네트워크 연결을 확인해 주세요."
            }
            isSaving = false
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
