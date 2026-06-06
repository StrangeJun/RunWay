package com.runway.wear

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runway.wear.data.WatchDataLayerClient
import com.runway.wear.data.PhoneRunStateRepository
import com.runway.wear.health.HealthServicesManager
import com.runway.wear.model.RunGoal
import com.runway.wear.model.WatchRunState
import com.runway.wear.model.WatchScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WatchViewModel(application: Application) : AndroidViewModel(application) {
    private val dataLayer = WatchDataLayerClient(application)
    private val healthServices = HealthServicesManager(application)

    private val _state = MutableStateFlow(WatchRunState())
    val state: StateFlow<WatchRunState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var metricsJob: Job? = null

    init {
        refreshConnection()
        observePhoneState()
    }

    fun refreshConnection() {
        viewModelScope.launch {
            _state.update { it.copy(isPhoneConnected = dataLayer.isPhoneConnected()) }
        }
    }

    fun navigate(screen: WatchScreen) {
        _state.update { it.copy(screen = screen) }
    }

    fun start(goal: RunGoal) {
        _state.update {
            WatchRunState(
                screen = WatchScreen.TRACKING,
                goal = goal,
                isPhoneConnected = it.isPhoneConnected,
                isRunning = true,
            )
        }
        viewModelScope.launch {
            dataLayer.sendStart(goal)
            runCatching { healthServices.start() }
        }
        startTimer()
        observeMetrics()
    }

    fun pause() {
        _state.update { it.copy(screen = WatchScreen.PAUSED, isPaused = true) }
        viewModelScope.launch {
            dataLayer.pause()
            runCatching { healthServices.pause() }
        }
    }

    fun resume() {
        _state.update { it.copy(screen = WatchScreen.TRACKING, isPaused = false) }
        viewModelScope.launch {
            dataLayer.resume()
            runCatching { healthServices.resume() }
        }
    }

    fun finish() {
        timerJob?.cancel()
        _state.update { it.copy(screen = WatchScreen.SUMMARY, isRunning = false, isPaused = false) }
        viewModelScope.launch {
            dataLayer.finish()
            runCatching { healthServices.finish() }
        }
    }

    fun abandon() {
        timerJob?.cancel()
        metricsJob?.cancel()
        viewModelScope.launch {
            dataLayer.abandon()
            runCatching { healthServices.finish() }
        }
        _state.value = WatchRunState(isPhoneConnected = _state.value.isPhoneConnected)
    }

    fun returnHome() {
        metricsJob?.cancel()
        _state.value = WatchRunState(isPhoneConnected = _state.value.isPhoneConnected)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _state.update { current ->
                    if (current.isRunning && !current.isPaused) {
                        current.copy(elapsedSeconds = current.elapsedSeconds + 1)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun observeMetrics() {
        metricsJob?.cancel()
        metricsJob = viewModelScope.launch {
            healthServices.updates.collect { update ->
                _state.update { current ->
                    val distance = update.distanceMeters ?: current.distanceMeters
                    val elapsedMinutes = current.elapsedSeconds / 60f
                    val pace = if (distance >= 20 && elapsedMinutes > 0) {
                        (elapsedMinutes / (distance / 1000f)).toFloat()
                    } else {
                        current.paceMinPerKm
                    }
                    current.copy(
                        distanceMeters = distance,
                        paceMinPerKm = pace,
                        heartRateBpm = update.heartRateBpm ?: current.heartRateBpm,
                        cadenceSpm = update.cadenceSpm ?: current.cadenceSpm,
                        gpsStatus = update.gpsStatus ?: current.gpsStatus,
                    )
                }
            }
        }
    }

    private fun observePhoneState() {
        viewModelScope.launch {
            PhoneRunStateRepository.state.collect { phoneState ->
                phoneState ?: return@collect
                _state.update { current ->
                    current.copy(
                        isPhoneConnected = true,
                        phoneStatusMessage = when (phoneState.status) {
                            "STARTING" -> "폰에서 기록을 준비하고 있습니다"
                            "RUNNING" -> null
                            "PAUSED" -> "폰 기록 일시정지"
                            "FINISHED" -> "폰에 기록이 저장되었습니다"
                            "ABANDONED" -> "폰 기록이 취소되었습니다"
                            "ERROR" -> phoneErrorMessage(phoneState.error)
                            else -> current.phoneStatusMessage
                        },
                    )
                }
            }
        }
    }

    private fun phoneErrorMessage(error: String?): String = when (error) {
        "RUN_ALREADY_ACTIVE" -> "폰에서 이미 러닝 중입니다"
        "PHONE_PERMISSION_REQUIRED" -> "폰 앱에서 위치 권한을 허용하세요"
        "TRACKING_SERVICE_START_FAILED" -> "폰의 위치 권한을 확인하세요"
        "RUN_CREATE_FAILED" -> "폰에서 기록을 만들지 못했습니다"
        "PAUSE_FAILED" -> "폰 기록을 일시정지하지 못했습니다"
        "RESUME_FAILED" -> "폰 기록을 재개하지 못했습니다"
        "FINISH_FAILED" -> "폰 기록 저장을 다시 시도해 주세요"
        "NO_ACTIVE_RUN" -> "폰에 진행 중인 기록이 없습니다"
        else -> "폰 동기화에 실패했습니다"
    }
}
