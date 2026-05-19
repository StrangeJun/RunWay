package com.runway.android.core.tracking

import com.runway.android.core.location.DistanceCalculator
import com.runway.android.core.location.LocationTracker
import com.runway.android.data.running.model.RunPointRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
 */
@Singleton
class RunTrackingManager @Inject constructor(
    private val locationTracker: LocationTracker,
    @Named("appScope") private val appScope: CoroutineScope,
) {
    private val _state = MutableStateFlow(RunTrackingState())
    val state: StateFlow<RunTrackingState> = _state.asStateFlow()

    private val pointsMutex = Mutex()
    private val _pendingPoints = mutableListOf<RunPointRequest>()
    private val pointSequence = AtomicInteger(0)

    private var locationJob: Job? = null
    private var timerJob: Job? = null

    fun start(mode: RunTrackingMode) {
        pointSequence.set(0)
        _state.value = RunTrackingState(mode = mode, isTracking = true)
        startTimer()
        startLocationTracking()
    }

    fun pause() = _state.update { it.copy(isPaused = true) }

    fun resume() = _state.update { it.copy(isPaused = false) }

    fun stop() {
        timerJob?.cancel()
        locationJob?.cancel()
        timerJob = null
        locationJob = null
        _state.value = RunTrackingState()
        appScope.launch { pointsMutex.withLock { _pendingPoints.clear() } }
        pointSequence.set(0)
    }

    // Thread-safe consume: returns all pending points and clears the queue.
    suspend fun consumePoints(): List<RunPointRequest> = pointsMutex.withLock {
        _pendingPoints.toList().also { _pendingPoints.clear() }
    }

    // Re-enqueues points at the front (called on upload failure to preserve order).
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

                if (!currentState.isPaused) {
                    val prev = currentState.lastLocation
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

                    _state.update {
                        it.copy(
                            hasFirstFix = true,
                            distanceMeters = it.distanceMeters + deltaMeters,
                            currentSpeedMps = location.speedMps,
                            lastLocation = location,
                        )
                    }
                } else {
                    // During pause: update position and fix status but don't accumulate distance.
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
