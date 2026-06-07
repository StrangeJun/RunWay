package com.runway.wear

import android.app.Application
import android.annotation.SuppressLint
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runway.wear.data.WatchDataLayerClient
import com.runway.wear.data.PendingWatchRun
import com.runway.wear.data.PendingWatchRunStore
import com.runway.wear.data.WatchRunPoint
import com.runway.wear.data.WatchRunSyncRepository
import com.runway.wear.data.OfflineCourse
import com.runway.wear.data.OfflineCourseRepository
import com.runway.wear.data.OfflineCourseStore
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.runway.wear.data.PhoneRunStateRepository
import com.runway.wear.data.WatchSettings
import com.runway.wear.data.WatchSettingsStore
import com.runway.wear.health.HealthServicesManager
import com.runway.wear.model.GoalCompletionAction
import com.runway.wear.model.IntervalTarget
import com.runway.wear.model.RunGoal
import com.runway.wear.model.WatchRunState
import com.runway.wear.model.WatchScreen
import com.runway.wear.voice.RunningVoiceGuide
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await as awaitTask
import java.time.Instant

class WatchViewModel(application: Application) : AndroidViewModel(application) {
    private val dataLayer = WatchDataLayerClient(application)
    private val healthServices = HealthServicesManager(application)
    private val voiceGuide = RunningVoiceGuide(application)
    private val settingsStore = WatchSettingsStore(application)
    private val pendingRunStore = PendingWatchRunStore(application)
    private val offlineCourseStore = OfflineCourseStore(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val initialSettings = settingsStore.load()
    private val _state = MutableStateFlow(
        WatchRunState(
            voiceGuidanceEnabled = initialSettings.voiceGuidanceEnabled,
            autoPauseEnabled = initialSettings.autoPauseEnabled,
            goalCompletionAction = initialSettings.goalCompletionAction,
        ),
    )
    val state: StateFlow<WatchRunState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var metricsJob: Job? = null
    private var gpsAutoPauseJob: Job? = null
    private var intervalSegmentStartSeconds = 0L
    private var intervalSegmentStartMeters = 0.0
    private var lastAnnouncedKilometer = 0
    private var settingsReturnScreen = WatchScreen.HOME
    private var lastAutoPaused = false
    private var lastGpsStatus = "SEARCHING"
    private var runStartedAt: Instant? = null
    private val runPoints = mutableListOf<WatchRunPoint>()
    private var activeCourse: OfflineCourse? = null
    private var lastCourseWarning = false
    private var pausedByCourseDeviation = false
    private var pausedByGpsInactivity = false
    private var lowSpeedTicks = 0
    private var highSpeedTicks = 0
    private var gpsLocationCallback: LocationCallback? = null

    init {
        OfflineCourseRepository.update(offlineCourseStore.load())
        refreshConnection()
        observePhoneState()
        observeRunSync()
        observeOfflineCourses()
    }

    fun refreshConnection() {
        viewModelScope.launch {
            val connected = dataLayer.isPhoneConnected()
            _state.update { it.copy(isPhoneConnected = connected) }
            if (connected) dataLayer.syncPendingRuns(pendingRunStore)
        }
    }

    fun navigate(screen: WatchScreen) {
        _state.update { it.copy(screen = screen) }
    }

    fun navigateBack() {
        when (_state.value.screen) {
            WatchScreen.HOME -> Unit
            WatchScreen.COURSE_LIST -> navigate(WatchScreen.HOME)
            WatchScreen.COURSE_DETAIL -> navigate(WatchScreen.COURSE_LIST)
            WatchScreen.GOAL_TYPE -> navigate(WatchScreen.HOME)
            WatchScreen.TIME_GOAL,
            WatchScreen.DISTANCE_GOAL,
            WatchScreen.INTERVAL_GOAL,
            -> navigate(WatchScreen.GOAL_TYPE)
            WatchScreen.SETTINGS -> navigate(settingsReturnScreen)
            WatchScreen.TRACKING -> pause()
            WatchScreen.PAUSED -> navigate(WatchScreen.TRACKING)
            WatchScreen.SUMMARY -> returnHome()
        }
    }

    fun openSettings() {
        settingsReturnScreen = _state.value.screen
        navigate(WatchScreen.SETTINGS)
    }

    fun setVoiceGuidanceEnabled(enabled: Boolean) {
        _state.update { it.copy(voiceGuidanceEnabled = enabled) }
        if (!enabled) voiceGuide.stopGuidance()
        persistSettings()
    }

    fun setAutoPauseEnabled(enabled: Boolean) {
        _state.update { it.copy(autoPauseEnabled = enabled) }
        persistSettings()
    }

    fun setGoalCompletionAction(action: GoalCompletionAction) {
        _state.update { it.copy(goalCompletionAction = action) }
        persistSettings()
    }

    fun start(goal: RunGoal) {
        activeCourse = null
        startTracking(goal)
    }

    fun selectCourse(courseId: String) {
        val course = OfflineCourseRepository.courses.value.firstOrNull { it.courseId == courseId }
            ?: return
        _state.update {
            it.copy(
                screen = WatchScreen.COURSE_DETAIL,
                selectedCourseId = course.courseId,
                courseName = course.name,
                courseDistanceMeters = course.distanceMeters,
            )
        }
    }

    fun startSelectedCourse() {
        val courseId = _state.value.selectedCourseId ?: return
        activeCourse = OfflineCourseRepository.courses.value.firstOrNull { it.courseId == courseId }
            ?: return
        lastCourseWarning = false
        pausedByCourseDeviation = false
        startTracking(RunGoal.Free)
    }

    @SuppressLint("MissingPermission")
    fun syncNearbyCourses() {
        if (!_state.value.isPhoneConnected) {
            _state.update { it.copy(phoneStatusMessage = "주변 코스 저장은 폰 연결이 필요합니다") }
            return
        }
        _state.update { it.copy(isSyncingCourses = true, phoneStatusMessage = "현재 위치 확인 중") }
        viewModelScope.launch {
            val location = runCatching {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).awaitTask()
            }.getOrNull()
            if (location == null) {
                _state.update {
                    it.copy(isSyncingCourses = false, phoneStatusMessage = "워치 위치를 확인하지 못했습니다")
                }
                return@launch
            }
            val sent = dataLayer.requestNearbyCourses(location.latitude, location.longitude)
            _state.update {
                it.copy(
                    isSyncingCourses = sent,
                    phoneStatusMessage = if (sent) "주변 코스를 불러오는 중" else "폰 연결을 확인해 주세요",
                )
            }
        }
    }

    private fun startTracking(goal: RunGoal) {
        runStartedAt = Instant.now()
        runPoints.clear()
        intervalSegmentStartSeconds = 0L
        intervalSegmentStartMeters = 0.0
        lastAnnouncedKilometer = 0
        lastAutoPaused = false
        lastGpsStatus = "SEARCHING"
        resetGpsAutoPause()
        _state.update {
            WatchRunState(
                screen = WatchScreen.TRACKING,
                goal = goal,
                isPhoneConnected = it.isPhoneConnected,
                voiceGuidanceEnabled = it.voiceGuidanceEnabled,
                autoPauseEnabled = it.autoPauseEnabled,
                goalCompletionAction = goal.completionAction,
                isRunning = true,
                selectedCourseId = activeCourse?.courseId,
                courseName = activeCourse?.name,
                courseDistanceMeters = activeCourse?.distanceMeters ?: 0.0,
            )
        }
        viewModelScope.launch {
            runCatching { healthServices.start(_state.value.autoPauseEnabled) }
        }
        speakIfEnabled { start() }
        startTimer()
        observeMetrics()
        startGpsAutoPauseMonitor()
    }

    fun pause() {
        pausedByGpsInactivity = false
        _state.update { it.copy(screen = WatchScreen.PAUSED, isPaused = true) }
        viewModelScope.launch {
            runCatching { healthServices.pause() }
        }
    }

    fun resume() {
        pausedByGpsInactivity = false
        resetGpsAutoPause()
        _state.update { it.copy(screen = WatchScreen.TRACKING, isPaused = false) }
        viewModelScope.launch {
            runCatching { healthServices.resume() }
        }
    }

    fun finish() {
        timerJob?.cancel()
        stopGpsAutoPauseMonitor()
        speakIfEnabled { finish() }
        val completed = _state.value
        _state.update {
            it.copy(
                screen = WatchScreen.SUMMARY,
                isRunning = false,
                isPaused = false,
                phoneStatusMessage = "폰 연결 시 자동으로 동기화됩니다",
            )
        }
        viewModelScope.launch {
            runCatching { healthServices.finish() }
            val durationSeconds = completed.elapsedSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val distanceKm = completed.distanceMeters / 1000.0
            pendingRunStore.add(
                PendingWatchRun.create(
                    startedAt = (runStartedAt ?: Instant.now()).toString(),
                    courseId = activeCourse?.courseId,
                    endedAt = Instant.now().toString(),
                    distanceMeters = completed.distanceMeters,
                    durationSeconds = durationSeconds,
                    avgPaceSecondsPerKm = if (distanceKm > 0.001) {
                        (durationSeconds / distanceKm).toInt()
                    } else {
                        0
                    },
                    caloriesBurned = (distanceKm * 72).toInt(),
                    avgHeartRateBpm = completed.heartRateBpm,
                    points = runPoints.toList(),
                ),
            )
            val sent = dataLayer.syncPendingRuns(pendingRunStore)
            if (sent > 0) {
                _state.update { it.copy(phoneStatusMessage = "폰으로 기록을 전송했습니다") }
            }
        }
    }

    fun abandon() {
        timerJob?.cancel()
        metricsJob?.cancel()
        stopGpsAutoPauseMonitor()
        speakIfEnabled { cancel() }
        viewModelScope.launch {
            runCatching { healthServices.finish() }
        }
        _state.value = freshAuthenticatedState()
    }

    fun returnHome() {
        metricsJob?.cancel()
        stopGpsAutoPauseMonitor()
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

    @SuppressLint("MissingPermission")
    private fun startGpsAutoPauseMonitor() {
        stopGpsAutoPauseMonitor()
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            GPS_SAMPLE_INTERVAL_MILLIS,
        )
            .setMinUpdateIntervalMillis(GPS_SAMPLE_INTERVAL_MILLIS)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    handleSpeedTick(location.speed.toDouble())
                }
            }
        }
        gpsLocationCallback = callback
        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun handleSpeedTick(speedMps: Double) {
        val current = _state.value
        if (!current.autoPauseEnabled || !current.isRunning) return

        if (!current.isPaused) {
            if (speedMps < AUTO_PAUSE_SPEED_MPS) {
                lowSpeedTicks++
                highSpeedTicks = 0
            } else {
                lowSpeedTicks = 0
                highSpeedTicks = 0
            }
            if (lowSpeedTicks >= AUTO_PAUSE_TICKS) {
                lowSpeedTicks = 0
                pausedByGpsInactivity = true
                _state.update { it.copy(screen = WatchScreen.TRACKING, isPaused = true) }
                viewModelScope.launch { runCatching { healthServices.pause() } }
                speakIfEnabled { autoPaused() }
            }
        } else if (pausedByGpsInactivity && !pausedByCourseDeviation) {
            if (speedMps > AUTO_RESUME_SPEED_MPS) {
                highSpeedTicks++
                if (highSpeedTicks >= AUTO_RESUME_TICKS) {
                    highSpeedTicks = 0
                    lowSpeedTicks = 0
                    pausedByGpsInactivity = false
                    _state.update { it.copy(screen = WatchScreen.TRACKING, isPaused = false) }
                    viewModelScope.launch { runCatching { healthServices.resume() } }
                    speakIfEnabled { autoResumed() }
                }
            } else {
                highSpeedTicks = 0
            }
        }
    }

    private fun resetGpsAutoPause() {
        pausedByGpsInactivity = false
        lowSpeedTicks = 0
        highSpeedTicks = 0
    }

    private fun stopGpsAutoPauseMonitor() {
        gpsAutoPauseJob?.cancel()
        gpsAutoPauseJob = null
        gpsLocationCallback?.let(fusedLocationClient::removeLocationUpdates)
        gpsLocationCallback = null
        resetGpsAutoPause()
    }

    private fun observeMetrics() {
        metricsJob?.cancel()
        metricsJob = viewModelScope.launch {
            healthServices.updates.collect { update ->
                update.locations.forEach { location ->
                    runPoints += WatchRunPoint(
                        sequence = runPoints.size,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitudeMeters = location.altitudeMeters,
                        speedMps = location.speedMps,
                        recordedAt = location.recordedAt,
                    )
                    updateCourseProgress(location.latitude, location.longitude)
                }
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
                updateAutoPause(update.isAutoPaused)
                updateGpsVoice(update.gpsStatus)
                announceCompletedKilometer()
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
        speakIfEnabled { intervalChanged(isWork, step) }
    }

    private fun completeGoal(@Suppress("UNUSED_PARAMETER") action: GoalCompletionAction) {
        _state.update { it.copy(goalCompleted = true) }
        val selectedAction = _state.value.goalCompletionAction
        speakIfEnabled {
            goalCompleted(selectedAction == GoalCompletionAction.CONTINUE)
        }
        if (selectedAction == GoalCompletionAction.PAUSE) pause()
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

    private fun observeRunSync() {
        viewModelScope.launch {
            WatchRunSyncRepository.lastSyncedId.collect { localId ->
                if (localId != null) {
                    _state.update { it.copy(phoneStatusMessage = "폰에 기록이 저장되었습니다") }
                }
            }
        }
    }

    private fun observeOfflineCourses() {
        viewModelScope.launch {
            OfflineCourseRepository.courses.collect { courses ->
                _state.update {
                    it.copy(
                        isSyncingCourses = false,
                        phoneStatusMessage = if (courses.isNotEmpty()) {
                            "주변 코스 ${courses.size}개를 저장했습니다"
                        } else {
                            it.phoneStatusMessage
                        },
                    )
                }
            }
        }
    }

    private fun updateCourseProgress(latitude: Double, longitude: Double) {
        val course = activeCourse ?: return
        val nearest = nearestDistanceToCourse(latitude, longitude, course)
        val offCourse = nearest != null && nearest > 80.0
        val progress = if (course.distanceMeters > 1.0) {
            (_state.value.distanceMeters * 100.0 / course.distanceMeters).toInt().coerceIn(0, 100)
        } else {
            0
        }
        _state.update {
            it.copy(
                courseProgressPercent = progress,
                distanceToCourseMeters = nearest,
                isOffCourse = offCourse,
            )
        }
        if (offCourse != lastCourseWarning) {
            lastCourseWarning = offCourse
            speakIfEnabled {
                if (offCourse) gpsWeak() else gpsRecovered()
            }
            if (offCourse && !_state.value.isPaused) {
                pausedByCourseDeviation = true
                pause()
            } else if (!offCourse && pausedByCourseDeviation) {
                pausedByCourseDeviation = false
                resume()
            }
        } else if (offCourse && !_state.value.isPaused) {
            pausedByCourseDeviation = true
            pause()
        }
    }

    override fun onCleared() {
        stopGpsAutoPauseMonitor()
        voiceGuide.shutdown()
        super.onCleared()
    }

    private fun freshAuthenticatedState(): WatchRunState = WatchRunState(
        isPhoneConnected = _state.value.isPhoneConnected,
        voiceGuidanceEnabled = _state.value.voiceGuidanceEnabled,
        autoPauseEnabled = _state.value.autoPauseEnabled,
        goalCompletionAction = _state.value.goalCompletionAction,
    )

    private fun distanceMeters(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double,
    ): Double {
        val earthRadius = 6_371_000.0
        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)
        val deltaLat = Math.toRadians(latitude2 - latitude1)
        val deltaLon = Math.toRadians(longitude2 - longitude1)
        val a = kotlin.math.sin(deltaLat / 2).let { it * it } +
            kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
            kotlin.math.sin(deltaLon / 2).let { it * it }
        return earthRadius * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }

    private fun nearestDistanceToCourse(
        latitude: Double,
        longitude: Double,
        course: OfflineCourse,
    ): Double? {
        if (course.points.isEmpty()) return null
        if (course.points.size == 1) {
            val point = course.points.first()
            return distanceMeters(latitude, longitude, point.latitude, point.longitude)
        }
        return course.points.zipWithNext().minOf { (start, end) ->
            val denominator =
                (end.latitude - start.latitude) * (end.latitude - start.latitude) +
                    (end.longitude - start.longitude) * (end.longitude - start.longitude)
            val fraction = if (denominator < 1e-12) {
                0.0
            } else {
                (((latitude - start.latitude) * (end.latitude - start.latitude) +
                    (longitude - start.longitude) * (end.longitude - start.longitude)) / denominator)
                    .coerceIn(0.0, 1.0)
            }
            val projectedLatitude = start.latitude + fraction * (end.latitude - start.latitude)
            val projectedLongitude = start.longitude + fraction * (end.longitude - start.longitude)
            distanceMeters(latitude, longitude, projectedLatitude, projectedLongitude)
        }
    }

    private fun announceCompletedKilometer() {
        val current = _state.value
        val completedKilometer = (current.distanceMeters / 1000.0).toInt()
        if (completedKilometer <= lastAnnouncedKilometer) return
        lastAnnouncedKilometer = completedKilometer
        speakIfEnabled {
            kilometer(
                kilometers = completedKilometer,
                elapsedSeconds = current.elapsedSeconds,
                heartRateBpm = current.heartRateBpm,
            )
        }
    }

    private fun updateAutoPause(isAutoPaused: Boolean?) {
        if (isAutoPaused == null || isAutoPaused == lastAutoPaused) return
        lastAutoPaused = isAutoPaused
        _state.update {
            it.copy(
                isPaused = isAutoPaused,
                screen = WatchScreen.TRACKING,
            )
        }
        speakIfEnabled {
            if (isAutoPaused) autoPaused() else autoResumed()
        }
    }

    private fun updateGpsVoice(gpsStatus: String?) {
        gpsStatus ?: return
        if (gpsStatus == lastGpsStatus) return
        if (gpsStatus == "POOR") {
            speakIfEnabled { gpsWeak() }
        } else if (lastGpsStatus == "POOR" && gpsStatus == "GOOD") {
            speakIfEnabled { gpsRecovered() }
        }
        lastGpsStatus = gpsStatus
    }

    private fun persistSettings() {
        val current = _state.value
        settingsStore.save(
            WatchSettings(
                voiceGuidanceEnabled = current.voiceGuidanceEnabled,
                autoPauseEnabled = current.autoPauseEnabled,
                goalCompletionAction = current.goalCompletionAction,
            ),
        )
    }

    private inline fun speakIfEnabled(block: RunningVoiceGuide.() -> Unit) {
        if (_state.value.voiceGuidanceEnabled) voiceGuide.block()
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

    private companion object {
        const val GPS_SAMPLE_INTERVAL_MILLIS = 1_000L
        const val AUTO_PAUSE_SPEED_MPS = 0.5
        const val AUTO_RESUME_SPEED_MPS = 1.0
        const val AUTO_PAUSE_TICKS = 10
        const val AUTO_RESUME_TICKS = 3
    }
}
