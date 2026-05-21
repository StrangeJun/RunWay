package com.runway.android.core.tracking

import com.runway.android.core.cadence.CadenceTracker
import com.runway.android.core.location.DistanceCalculator
import com.runway.android.core.location.GpsPointValidator
import com.runway.android.core.location.LocationTracker
import com.runway.android.data.running.model.RunPointRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 앱 전체 생명주기를 따르는 추적 상태 관리자.
 * GPS 수집과 타이머는 appScope(Dispatchers.IO)에서 실행되므로
 * ForegroundService가 살아있는 동안 앱이 백그라운드에 있어도 계속 동작한다.
 *
 * Auto-pause: speed < AUTO_PAUSE_SPEED_MPS 가 AUTO_PAUSE_TICKS 번 연속으로 감지되면
 * 자동으로 일시정지. speed > AUTO_RESUME_SPEED_MPS 가 AUTO_RESUME_TICKS 번 연속이면
 * 자동 재개.
 */
@Singleton
class RunTrackingManager @Inject constructor(
    private val locationTracker: LocationTracker,
    private val cadenceTracker: CadenceTracker,
    @Named("appScope") private val appScope: CoroutineScope,
) {
    companion object {
        private const val AUTO_PAUSE_SPEED_MPS = 0.5f   // 이하 속도 = 정지 판정
        private const val AUTO_RESUME_SPEED_MPS = 1.0f  // 초과 속도 = 움직임 재개
        private const val AUTO_PAUSE_TICKS = 10          // 10회 연속 → auto-pause
        private const val AUTO_RESUME_TICKS = 3          // 3회 연속 → auto-resume
    }

    private val _state = MutableStateFlow(RunTrackingState())
    val state: StateFlow<RunTrackingState> = _state.asStateFlow()

    /** Emits the completed kilometer count (1, 2, 3 …) each time a milestone is crossed. */
    private val _milestoneFlow = MutableSharedFlow<Int>(extraBufferCapacity = 10)
    val milestoneFlow: SharedFlow<Int> = _milestoneFlow.asSharedFlow()

    private val pointsMutex = Mutex()
    private val _pendingPoints = mutableListOf<RunPointRequest>()
    private val pointSequence = AtomicInteger(0)

    private var locationJob: Job? = null
    private var timerJob: Job? = null
    private var cadenceJob: Job? = null

    @Volatile private var lowSpeedTicks = 0
    @Volatile private var highSpeedTicks = 0
    @Volatile private var lastMilestoneKm = 0

    fun start(mode: RunTrackingMode) {
        pointSequence.set(0)
        lowSpeedTicks = 0
        highSpeedTicks = 0
        lastMilestoneKm = 0
        _state.value = RunTrackingState(mode = mode, isTracking = true)
        startTimer()
        startCadenceTracking()
        startLocationTracking()
    }

    fun pause() {
        lowSpeedTicks = 0
        highSpeedTicks = 0
        _state.update { it.copy(isPaused = true, pauseReason = PauseReason.MANUAL) }
    }

    fun resume() {
        lowSpeedTicks = 0
        highSpeedTicks = 0
        _state.update { it.copy(isPaused = false, pauseReason = PauseReason.NONE) }
    }

    fun stop() {
        timerJob?.cancel()
        locationJob?.cancel()
        cadenceJob?.cancel()
        timerJob = null
        locationJob = null
        cadenceJob = null
        lowSpeedTicks = 0
        highSpeedTicks = 0
        lastMilestoneKm = 0
        _state.value = RunTrackingState()
        appScope.launch { pointsMutex.withLock { _pendingPoints.clear() } }
        pointSequence.set(0)
    }

    private fun startCadenceTracking() {
        cadenceJob?.cancel()
        cadenceJob = appScope.launch {
            cadenceTracker.cadenceSpmFlow().collect { cadence ->
                _state.update { it.copy(cadenceSpm = cadence) }
            }
        }
    }

    /** Thread-safe consume: returns all pending points and clears the queue. */
    suspend fun consumePoints(): List<RunPointRequest> = pointsMutex.withLock {
        _pendingPoints.toList().also { _pendingPoints.clear() }
    }

    /** Re-enqueues points at the front (called on upload failure to preserve order). */
    suspend fun requeuePoints(points: List<RunPointRequest>) = pointsMutex.withLock {
        _pendingPoints.addAll(0, points)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = appScope.launch {
            while (true) {
                delay(1_000L)
                if (!_state.value.isPaused) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    private fun startLocationTracking() {
        locationJob?.cancel()
        locationJob = appScope.launch {
            locationTracker.locationFlow().collect { location ->
                val currentState = _state.value
                val speed = location.speedMps ?: 0f

                when {
                    !currentState.isPaused -> {
                        // Track low-speed ticks for auto-pause detection
                        if (speed < AUTO_PAUSE_SPEED_MPS) {
                            lowSpeedTicks++
                            highSpeedTicks = 0
                        } else {
                            lowSpeedTicks = 0
                            highSpeedTicks = 0
                        }

                        if (lowSpeedTicks >= AUTO_PAUSE_TICKS) {
                            // Trigger auto-pause — don't add the current point
                            lowSpeedTicks = 0
                            _state.update {
                                it.copy(
                                    isPaused = true,
                                    pauseReason = PauseReason.AUTO,
                                    hasFirstFix = true,
                                    currentSpeedMps = location.speedMps,
                                    lastLocation = location,
                                )
                            }
                            return@collect
                        }

                        // Normal GPS point processing
                        val prev = currentState.lastLocation
                        if (!GpsPointValidator.isValid(location, prev)) {
                            _state.update {
                                it.copy(
                                    hasFirstFix = true,
                                    currentSpeedMps = location.speedMps,
                                    lastLocation = location,
                                )
                            }
                            return@collect
                        }

                        val deltaMeters = if (prev != null) {
                            DistanceCalculator.calculate(prev, location)
                        } else {
                            0.0
                        }

                        val point = RunPointRequest(
                            sequence = pointSequence.getAndIncrement(),
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitudeMeters = location.altitudeMeters ?: 0.0,
                            speedMps = location.speedMps?.toDouble() ?: 0.0,
                            recordedAt = location.recordedAt.toString(),
                        )
                        pointsMutex.withLock { _pendingPoints.add(point) }

                        val newDistance = currentState.distanceMeters + deltaMeters
                        _state.update {
                            it.copy(
                                hasFirstFix = true,
                                distanceMeters = newDistance,
                                currentSpeedMps = location.speedMps,
                                lastLocation = location,
                            )
                        }

                        // Emit km milestone event
                        val newKm = (newDistance / 1000.0).toInt()
                        if (newKm > lastMilestoneKm) {
                            lastMilestoneKm = newKm
                            _milestoneFlow.tryEmit(newKm)
                        }
                    }

                    currentState.pauseReason == PauseReason.AUTO -> {
                        // Auto-paused: monitor for auto-resume
                        if (speed > AUTO_RESUME_SPEED_MPS) {
                            highSpeedTicks++
                            if (highSpeedTicks >= AUTO_RESUME_TICKS) {
                                highSpeedTicks = 0
                                lowSpeedTicks = 0
                                _state.update {
                                    it.copy(
                                        isPaused = false,
                                        pauseReason = PauseReason.NONE,
                                        hasFirstFix = true,
                                        currentSpeedMps = location.speedMps,
                                        lastLocation = location,
                                    )
                                }
                                return@collect
                            }
                        } else {
                            highSpeedTicks = 0
                        }
                        // Update position/speed without accumulating distance
                        _state.update {
                            it.copy(
                                hasFirstFix = true,
                                currentSpeedMps = location.speedMps,
                                lastLocation = location,
                            )
                        }
                    }

                    else -> {
                        // Manual pause: update position without accumulating distance
                        _state.update {
                            it.copy(
                                hasFirstFix = true,
                                currentSpeedMps = location.speedMps,
                                lastLocation = location,
                            )
                        }
                    }
                }
            }
        }
    }
}
