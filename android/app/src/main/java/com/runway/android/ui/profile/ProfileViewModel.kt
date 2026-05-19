package com.runway.android.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
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
            val runsDeferred = async { runningRepository.getMyRuns(page = 0, size = 100) }

            val profileResult = profileDeferred.await()
            val runsResult = runsDeferred.await()

            if (profileResult is NetworkResult.Success) {
                nickname = profileResult.data.nickname
                email = profileResult.data.email
            } else {
                profileError = true
            }

            val totalRuns: Long
            val totalDistanceKm: Double
            if (runsResult is NetworkResult.Success) {
                totalRuns = runsResult.data.totalElements
                totalDistanceKm = runsResult.data.content.sumOf { it.distanceMeters ?: 0.0 } / 1000.0
            } else {
                totalRuns = 0L
                totalDistanceKm = 0.0
            }

            stats = ProfileStats(
                totalRuns = totalRuns,
                totalDistanceKm = "%.1f".format(totalDistanceKm),
            )
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
