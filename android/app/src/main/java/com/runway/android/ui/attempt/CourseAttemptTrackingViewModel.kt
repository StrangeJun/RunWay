package com.runway.android.ui.attempt

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.runway.android.core.location.GpsStatus
import com.runway.android.core.map.MapPoint
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.tracking.PendingPointQueue
import com.runway.android.core.tracking.RunTrackingAction
import com.runway.android.core.tracking.RunTrackingManager
import com.runway.android.core.tracking.RunTrackingMode
import com.runway.android.core.tracking.RunTrackingService
import com.runway.android.core.tracking.TrackingSessionSnapshot
import com.runway.android.core.tracking.TrackingSessionStore
import com.runway.android.core.tracking.toRunPointRequest
import com.runway.android.core.util.formatDuration
import com.runway.android.core.voice.RunningVoiceGuide
import com.runway.android.data.attempt.model.AbandonAttemptRequest
import com.runway.android.data.attempt.model.FinishAttemptRequest
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.domain.attempt.CourseAttemptRepository
import com.runway.android.domain.course.CourseRepository
import com.runway.android.domain.running.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named

sealed class AttemptNavEvent {
    data class NavigateToLeaderboard(
        val courseId: String,
        val isPR: Boolean = false,
        val previousBestSeconds: Int? = null,
        val improvementSeconds: Int? = null,
    ) : AttemptNavEvent()
    object NavigateBack : AttemptNavEvent()
}

enum class CourseTrackStatus {
    UNKNOWN,
    ON_COURSE,
    NEAR_COURSE,
    OFF_COURSE,
}

@HiltViewModel
class CourseAttemptTrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val runningRepository: RunningRepository,
    private val courseAttemptRepository: CourseAttemptRepository,
    private val courseRepository: CourseRepository,
    private val manager: RunTrackingManager,
    private val pendingPointQueue: PendingPointQueue,
    private val sessionStore: TrackingSessionStore,
    private val voiceGuide: RunningVoiceGuide,
    @Named("appScope") private val appScope: CoroutineScope,
) : ViewModel() {

    val courseId: String = checkNotNull(savedStateHandle["courseId"])
    private val courseAttemptId: String = checkNotNull(savedStateHandle["courseAttemptId"])
    private val runningRecordId: String = checkNotNull(savedStateHandle["runningRecordId"])

    var gpsStatus by mutableStateOf(GpsStatus.PERMISSION_REQUIRED)
        private set
    var elapsedSeconds by mutableStateOf(0)
        private set
    var distanceKm by mutableStateOf(0.0)
        private set
    var isFinishing by mutableStateOf(false)
        private set
    var isAbandoning by mutableStateOf(false)
        private set
    var finishError by mutableStateOf<String?>(null)
        private set
    var coursePoints by mutableStateOf<List<MapPoint>>(emptyList())
        private set
    var currentLocationPoint by mutableStateOf<MapPoint?>(null)
        private set
    var isAutoPaused by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var milestoneMessage by mutableStateOf<String?>(null)
        private set
    var courseDistanceMeters by mutableStateOf(0.0)
        private set
    var nearestCourseDistanceMeters by mutableStateOf<Double?>(null)
        private set
    var showDeviationWarning by mutableStateOf(false)
        private set

    private var lastSpeedMps by mutableStateOf<Float?>(null)
    private var cadenceSpm by mutableStateOf<Int?>(null)

    val timerText: String
        get() = formatDuration(elapsedSeconds)

    val distanceText: String
        get() = "%.2f".format(distanceKm)

    val paceText: String
        get() {
            if (distanceKm < 0.001) return "--'--\""
            val secsPerKm = (elapsedSeconds / distanceKm).toInt()
            return "%d'%02d\"".format(secsPerKm / 60, secsPerKm % 60)
        }

    val speedText: String
        get() {
            val gpsSpeed = lastSpeedMps
            return if (gpsSpeed != null && gpsSpeed >= 0.1f) {
                "%.1f".format(gpsSpeed * 3.6f)
            } else if (elapsedSeconds < 1) {
                "0.0"
            } else {
                "%.1f".format(distanceKm / (elapsedSeconds / 3600.0))
            }
        }

    val cadenceText: String
        get() = cadenceSpm?.takeIf { it > 0 }?.toString() ?: "--"

    val courseProgressPercent: Int
        get() = if (courseDistanceMeters > 1.0) {
            ((distanceKm * 1000.0 / courseDistanceMeters) * 100.0).toInt().coerceIn(0, 100)
        } else {
            0
        }

    val remainingDistanceText: String
        get() {
            val remainingMeters = (courseDistanceMeters - distanceKm * 1000.0).coerceAtLeast(0.0)
            return "%.2f".format(remainingMeters / 1000.0)
        }

    val trackStatus: CourseTrackStatus
        get() = when (val meters = nearestCourseDistanceMeters) {
            null -> CourseTrackStatus.UNKNOWN
            in 0.0..30.0 -> CourseTrackStatus.ON_COURSE
            in 30.0..80.0 -> CourseTrackStatus.NEAR_COURSE
            else -> CourseTrackStatus.OFF_COURSE
        }

    private val _navEvent = MutableSharedFlow<AttemptNavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    private var isDone = false
    private var serviceStarted = false
    private var lastTrackStatus: CourseTrackStatus = CourseTrackStatus.UNKNOWN
    private var nearFinishMessageShown = false
    private var pausedByDeviation = false
    // true 동안에는 연속 재일시정지 중복 실행 방지 (isPaused=true 확인 후 리셋)
    private var repausingForDeviation = false

    private var stateObserveJob: Job? = null
    private var batchJob: Job? = null
    private var milestoneCollectJob: Job? = null
    private var milestoneDisplayJob: Job? = null

    init {
        viewModelScope.launch {
            sessionStore.saveSnapshot(
                TrackingSessionSnapshot(
                    runningRecordId = runningRecordId,
                    courseAttemptId = courseAttemptId,
                    elapsedSeconds = 0,
                    distanceMeters = 0.0,
                )
            )
        }
        viewModelScope.launch {
            when (val r = courseRepository.getCoursePoints(courseId)) {
                is NetworkResult.Success -> {
                    coursePoints = r.data.points.map { MapPoint(it.latitude, it.longitude) }
                    courseDistanceMeters = calculateCourseDistance(coursePoints)
                }
                else -> {}
            }
        }
        startBatchSaving()
    }

    fun startTracking() {
        if (serviceStarted) return
        serviceStarted = true
        gpsStatus = GpsStatus.WAITING_FOR_FIX
        voiceGuide.start()

        ContextCompat.startForegroundService(
            context,
            Intent(context, RunTrackingService::class.java).apply {
                action = RunTrackingAction.ACTION_START
                putExtra(RunTrackingAction.EXTRA_MODE, RunTrackingMode.COURSE_ATTEMPT.name)
            }
        )

        stateObserveJob = viewModelScope.launch {
            manager.state.collect { state ->
                if (state.hasFirstFix && gpsStatus != GpsStatus.ACTIVE) {
                    gpsStatus = GpsStatus.ACTIVE
                }
                elapsedSeconds = state.elapsedSeconds
                distanceKm = state.distanceMeters / 1000.0
                lastSpeedMps = state.currentSpeedMps
                cadenceSpm = state.cadenceSpm
                isAutoPaused = state.isAutoPaused
                isPaused = state.isPaused
                if (isPaused) repausingForDeviation = false
                currentLocationPoint = state.lastLocation?.let { location ->
                    MapPoint(location.latitude, location.longitude).also { point ->
                        nearestCourseDistanceMeters = nearestDistanceToCourse(point, coursePoints)
                        val currentStatus = trackStatus
                        if (currentStatus != lastTrackStatus) {
                            if (currentStatus == CourseTrackStatus.OFF_COURSE) {
                                showDeviationWarning = true
                                vibrateDeviation()
                                voiceGuide.offCourse()
                                // 최초 이탈 시 즉시 일시정지
                                if (!isPaused && !isAutoPaused) {
                                    pausedByDeviation = true
                                    repausingForDeviation = true
                                    pause()
                                }
                            } else if (lastTrackStatus == CourseTrackStatus.OFF_COURSE) {
                                showDeviationWarning = false
                                voiceGuide.backOnCourse()
                                if (pausedByDeviation) {
                                    pausedByDeviation = false
                                    repausingForDeviation = false
                                    resume()
                                }
                            }
                            lastTrackStatus = currentStatus
                        }
                        // 이탈 중 재생 버튼으로 재개한 경우 → 진동+재일시정지 (매 틱 확인)
                        if (currentStatus == CourseTrackStatus.OFF_COURSE &&
                            !isPaused && !isAutoPaused && !repausingForDeviation
                        ) {
                            repausingForDeviation = true
                            pausedByDeviation = true
                            vibrate()
                            pause()
                        }
                        // 90% 도달 시 1회만 동기부여 메시지 표시
                        if (!nearFinishMessageShown && courseProgressPercent >= 90 && courseProgressPercent < 100) {
                            nearFinishMessageShown = true
                            vibrate()
                            voiceGuide.nearCourseFinish()
                            milestoneDisplayJob?.cancel()
                            milestoneMessage = "거의 다 왔어요! 조금만 더 달려요!"
                            milestoneDisplayJob = viewModelScope.launch {
                                delay(4_000)
                                milestoneMessage = null
                            }
                        }
                    }
                }
            }
        }

        milestoneCollectJob = viewModelScope.launch {
            manager.milestoneFlow.collect { km -> triggerMilestone(km) }
        }
    }

    private fun triggerMilestone(km: Int) {
        vibrate()
        voiceGuide.kilometer(km, elapsedSeconds)
        milestoneDisplayJob?.cancel()
        milestoneMessage = "${km}km 완료 · 현재 페이스 $paceText /km"
        milestoneDisplayJob = viewModelScope.launch {
            delay(4_000)
            milestoneMessage = null
        }
    }

    fun pause() {
        manager.pause()
        viewModelScope.launch { runningRepository.pauseRun(runningRecordId) }
    }

    fun resume() {
        val wasAutoPaused = manager.state.value.isAutoPaused
        manager.resume()
        if (!wasAutoPaused) {
            viewModelScope.launch { runningRepository.resumeRun(runningRecordId) }
        }
    }

    fun dismissDeviationWarning() {
        showDeviationWarning = false
    }

    @Suppress("DEPRECATION")
    private fun vibrateDeviation() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300, 100, 300), -1)
        )
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
        )
    }

    private fun startBatchSaving() {
        batchJob = viewModelScope.launch {
            while (true) {
                delay(5_000L)
                flushPendingPoints()
                updateSnapshot()
            }
        }
    }

    private suspend fun flushPendingPoints() {
        val inMemory = manager.consumePoints()
        pendingPointQueue.add(runningRecordId, inMemory)

        val batch = pendingPointQueue.dequeue(runningRecordId, 50)
        if (batch.isEmpty()) return

        val result = runningRepository.savePoints(runningRecordId, SavePointsRequest(batch.map { it.toRunPointRequest() }))
        if (result is NetworkResult.Success) {
            pendingPointQueue.deleteByIds(batch.map { it.id })
        }
    }

    private suspend fun updateSnapshot() {
        sessionStore.saveSnapshot(
            TrackingSessionSnapshot(
                runningRecordId = runningRecordId,
                courseAttemptId = courseAttemptId,
                elapsedSeconds = elapsedSeconds,
                distanceMeters = distanceKm * 1000.0,
            )
        )
    }

    fun finish() {
        if (isFinishing || isAbandoning) return
        isFinishing = true
        finishError = null

        val seconds = elapsedSeconds
        val distance = distanceKm

        viewModelScope.launch {
            pendingPointQueue.add(runningRecordId, manager.consumePoints())
            // 큐가 빌 때까지 업로드. 연속 실패 10회 시 중단 (서버/네트워크 문제).
            var failures = 0
            while (failures < 10) {
                val batch = pendingPointQueue.dequeue(runningRecordId, 50)
                if (batch.isEmpty()) break
                val r = runningRepository.savePoints(
                    runningRecordId,
                    SavePointsRequest(batch.map { it.toRunPointRequest() }),
                )
                if (r is NetworkResult.Success) {
                    pendingPointQueue.deleteByIds(batch.map { it.id })
                    failures = 0
                } else {
                    failures++
                }
            }

            val finishResult = courseAttemptRepository.finishAttempt(
                attemptId = courseAttemptId,
                request = FinishAttemptRequest(
                    endedAt = Instant.now().toString(),
                    distanceMeters = distance * 1000.0,
                    durationSeconds = seconds,
                    avgPaceSecondsPerKm = if (distance > 0.001) (seconds / distance).toInt() else 0,
                    caloriesBurned = (distance * 72).toInt(),
                ),
            )

            when (finishResult) {
                is NetworkResult.ApiError,
                is NetworkResult.NetworkError -> {
                    isFinishing = false
                    finishError = "완주 처리에 실패했습니다. 다시 시도해 주세요."
                    return@launch
                }
                is NetworkResult.Success -> {
                    voiceGuide.finish()
                    val response = finishResult.data
                    sessionStore.clearSnapshot()
                    pendingPointQueue.deleteByRunningRecordId(runningRecordId)
                    stopServiceAndJobs()
                    isDone = true
                    _navEvent.emit(
                        AttemptNavEvent.NavigateToLeaderboard(
                            courseId = courseId,
                            isPR = response.isPR,
                            previousBestSeconds = response.previousBestSeconds,
                            improvementSeconds = response.improvementSeconds,
                        )
                    )
                }
            }
        }
    }

    fun abandon() {
        if (isFinishing || isAbandoning) return
        isAbandoning = true

        viewModelScope.launch {
            courseAttemptRepository.abandonAttempt(
                attemptId = courseAttemptId,
                request = AbandonAttemptRequest(endedAt = Instant.now().toString()),
            )
            sessionStore.clearSnapshot()
            pendingPointQueue.deleteByRunningRecordId(runningRecordId)
            stopServiceAndJobs()
            isDone = true
            _navEvent.emit(AttemptNavEvent.NavigateBack)
        }
    }

    private fun stopServiceAndJobs() {
        stateObserveJob?.cancel()
        batchJob?.cancel()
        milestoneCollectJob?.cancel()
        milestoneDisplayJob?.cancel()
        manager.stop()
        context.startService(
            Intent(context, RunTrackingService::class.java).apply {
                action = RunTrackingAction.ACTION_STOP
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        if (!isDone) {
            stopServiceAndJobs()
            appScope.launch {
                courseAttemptRepository.abandonAttempt(
                    attemptId = courseAttemptId,
                    request = AbandonAttemptRequest(endedAt = Instant.now().toString()),
                )
                sessionStore.clearSnapshot()
                pendingPointQueue.deleteByRunningRecordId(runningRecordId)
            }
        }
    }
}

private fun calculateCourseDistance(points: List<MapPoint>): Double {
    if (points.size < 2) return 0.0
    return points.zipWithNext().sumOf { (from, to) ->
        distanceMeters(from, to)
    }
}

private fun nearestDistanceToCourse(current: MapPoint, course: List<MapPoint>): Double? {
    if (course.isEmpty()) return null
    if (course.size == 1) return distanceMeters(current, course[0])
    // Use segment-based distance to avoid false off-course on long straight segments
    return course.zipWithNext().minOf { (a, b) -> distanceToSegmentMeters(current, a, b) }
}

private fun distanceToSegmentMeters(p: MapPoint, a: MapPoint, b: MapPoint): Double {
    val segLenSq = distanceMeters(a, b).let { it * it }
    if (segLenSq < 0.01) return distanceMeters(p, a)  // degenerate segment
    // Project p onto segment ab (in lat/lon space — approximate but fine for small distances)
    val t = ((p.latitude - a.latitude) * (b.latitude - a.latitude) +
            (p.longitude - a.longitude) * (b.longitude - a.longitude)) /
            ((b.latitude - a.latitude) * (b.latitude - a.latitude) +
             (b.longitude - a.longitude) * (b.longitude - a.longitude))
    val tClamped = t.coerceIn(0.0, 1.0)
    val proj = MapPoint(
        latitude = a.latitude + tClamped * (b.latitude - a.latitude),
        longitude = a.longitude + tClamped * (b.longitude - a.longitude),
    )
    return distanceMeters(p, proj)
}

private fun distanceMeters(from: MapPoint, to: MapPoint): Double {
    val result = FloatArray(1)
    Location.distanceBetween(
        from.latitude,
        from.longitude,
        to.latitude,
        to.longitude,
        result,
    )
    return result[0].toDouble()
}
