package com.runway.android.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.running.model.PersonalRecordsResponse
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
    var stats by mutableStateOf<ProfileStats?>(null)
        private set
    var personalRecords by mutableStateOf<PersonalRecordsResponse?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var profileError by mutableStateOf(false)
        private set

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profileDeferred = async { userRepository.getMe() }
            val recordsDeferred = async { runningRepository.getPersonalRecords() }
            val statsDeferred = async { runningRepository.getRunningStats("all") }

            val profileResult = profileDeferred.await()
            val recordsResult = recordsDeferred.await()
            val statsResult = statsDeferred.await()

            if (profileResult is NetworkResult.Success) {
                nickname = profileResult.data.nickname
                email = profileResult.data.email
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
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
