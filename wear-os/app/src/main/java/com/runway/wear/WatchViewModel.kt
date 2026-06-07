package com.runway.wear

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.concurrent.futures.await
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.runway.wear.data.WatchDataLayerClient
import com.runway.wear.data.PhoneAuthStateRepository
import com.runway.wear.data.PhoneRunStateRepository
import com.runway.wear.health.HealthServicesManager
import com.runway.wear.model.GoalCompletionAction
import com.runway.wear.model.IntervalTarget
import com.runway.wear.model.PhoneAuthState
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
import java.util.concurrent.Executors

class WatchViewModel(application: Application) : AndroidViewModel(application) {
    private val dataLayer = WatchDataLayerClient(application)
    private val healthServices = HealthServicesManager(application)
    private val remoteActivityExecutor = Executors.newSingleThreadExecutor()
    private val remoteActivityHelper = RemoteActivityHelper(
        application,
        remoteActivityExecutor,
    )

    private val _state = MutableStateFlow(WatchRunState())
    val state: StateFlow<WatchRunState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var metricsJob: Job? = null
    private var authTimeoutJob: Job? = null
    private var intervalSegmentStartSeconds = 0L
    private var intervalSegmentStartMeters = 0.0

    init {
        refreshConnection()
        refreshAuthState()
        observePhoneState()
        observePhoneAuthState()
    }

    fun refreshConnection() {
        viewModelScope.launch {
            _state.update { it.copy(isPhoneConnected = dataLayer.isPhoneConnected()) }
        }
    }

    fun refreshAuthState() {
        authTimeoutJob?.cancel()
        _state.update { it.copy(phoneAuthState = PhoneAuthState.CHECKING, authMessage = null) }
        viewModelScope.launch {
            val requested = dataLayer.requestAuthState()
            if (!requested) {
                _state.update {
                    it.copy(
                        isPhoneConnected = false,
                        phoneAuthState = PhoneAuthState.LOGGED_OUT,
                        authMessage = "휴대폰 연결을 확인해 주세요",
                    )
                }
            } else {
                authTimeoutJob = viewModelScope.launch {
                    delay(4_000)
                    _state.update {
                        if (it.phoneAuthState == PhoneAuthState.CHECKING) {
                            it.copy(
                                phoneAuthState = PhoneAuthState.LOGGED_OUT,
                                authMessage = "휴대폰 앱을 열고 다시 확인해 주세요",
                            )
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    fun openPhoneLogin() {
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse(PHONE_LOGIN_URI))
                .addCategory(Intent.CATEGORY_BROWSABLE)
            runCatching {
                remoteActivityHelper.startRemoteActivity(intent, null).await()
            }.onSuccess {
                _state.update { it.copy(authMessage = "휴대폰에서 로그인해 주세요") }
            }.onFailure {
                _state.update { it.copy(authMessage = "휴대폰 앱을 열지 못했습니다") }
            }
        }
    }

    fun navigate(screen: WatchScreen) {
        _state.update { it.copy(screen = screen) }
    }

    fun start(goal: RunGoal) {
        intervalSegmentStartSeconds = 0L
        intervalSegmentStartMeters = 0.0
        _state.update {
            WatchRunState(
                screen = WatchScreen.TRACKING,
                goal = goal,
                isPhoneConnected = it.isPhoneConnected,
                phoneAuthState = it.phoneAuthState,
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
        _state.value = freshAuthenticatedState()
    }

    fun returnHome() {
        metricsJob?.cancel()
        _state.value = freshAuthenticatedState()
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
                updateGoalProgress()
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
                updateGoalProgress()
            }
        }
    }

    private fun updateGoalProgress() {
        val current = _state.value
        if (!current.isRunning || current.isPaused || current.goalCompleted) return

        when (val goal = current.goal) {
            RunGoal.Free -> Unit
            is RunGoal.Time -> {
                if (current.elapsedSeconds >= goal.minutes * 60L) completeGoal(goal.completionAction)
            }
            is RunGoal.Distance -> {
                if (current.distanceMeters >= goal.meters) completeGoal(goal.completionAction)
            }
            is RunGoal.Interval -> updateIntervalProgress(current, goal)
        }
    }

    private fun updateIntervalProgress(current: WatchRunState, goal: RunGoal.Interval) {
        val target = if (current.intervalIsWork) goal.work else goal.recovery
        val progress = when (target) {
            is IntervalTarget.Time -> {
                val elapsed = current.elapsedSeconds - intervalSegmentStartSeconds
                ((elapsed * 100) / target.seconds.coerceAtLeast(1)).toInt()
            }
            is IntervalTarget.Distance -> {
                val distance = current.distanceMeters - intervalSegmentStartMeters
                ((distance * 100) / target.meters.coerceAtLeast(1)).toInt()
            }
        }.coerceIn(0, 100)

        val remaining = when (target) {
            is IntervalTarget.Time -> {
                val elapsed = current.elapsedSeconds - intervalSegmentStartSeconds
                "${(target.seconds - elapsed).coerceAtLeast(0)}초 남음"
            }
            is IntervalTarget.Distance -> {
                val distance = current.distanceMeters - intervalSegmentStartMeters
                "${(target.meters - distance).coerceAtLeast(0.0).toInt()}m 남음"
            }
        }

        _state.update {
            it.copy(
                intervalSegmentProgress = progress,
                intervalRemainingLabel = remaining,
            )
        }

        if (progress < 100) return
        if (current.intervalIsWork) {
            startNextIntervalSegment(isWork = false, step = current.intervalStep)
        } else if (current.intervalStep < goal.sets) {
            startNextIntervalSegment(isWork = true, step = current.intervalStep + 1)
        } else {
            completeGoal(goal.completionAction)
        }
    }

    private fun startNextIntervalSegment(isWork: Boolean, step: Int) {
        val current = _state.value
        intervalSegmentStartSeconds = current.elapsedSeconds
        intervalSegmentStartMeters = current.distanceMeters
        _state.update {
            it.copy(
                intervalIsWork = isWork,
                intervalStep = step,
                intervalSegmentProgress = 0,
            )
        }
    }

    private fun completeGoal(action: GoalCompletionAction) {
        _state.update { it.copy(goalCompleted = true) }
        if (action == GoalCompletionAction.PAUSE) pause()
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

    private fun observePhoneAuthState() {
        viewModelScope.launch {
            PhoneAuthStateRepository.state.collect { snapshot ->
                snapshot ?: return@collect
                authTimeoutJob?.cancel()
                _state.update {
                    it.copy(
                        isPhoneConnected = true,
                        phoneAuthState = if (snapshot.isLoggedIn) {
                            PhoneAuthState.LOGGED_IN
                        } else {
                            PhoneAuthState.LOGGED_OUT
                        },
                        authMessage = null,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        remoteActivityExecutor.shutdown()
        super.onCleared()
    }

    private fun freshAuthenticatedState(): WatchRunState = WatchRunState(
        isPhoneConnected = _state.value.isPhoneConnected,
        phoneAuthState = _state.value.phoneAuthState,
    )

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

    private companion object {
        const val PHONE_LOGIN_URI = "pathfinder://auth/login"
    }
}
