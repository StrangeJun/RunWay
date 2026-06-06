package com.runway.android.core.wear

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.tracking.PendingPointQueue
import com.runway.android.core.tracking.RunTrackingAction
import com.runway.android.core.tracking.RunTrackingManager
import com.runway.android.core.tracking.RunTrackingMode
import com.runway.android.core.tracking.RunTrackingService
import com.runway.android.core.tracking.TrackingSessionSnapshot
import com.runway.android.core.tracking.TrackingSessionStore
import com.runway.android.core.tracking.toRunPointRequest
import com.runway.android.data.running.model.FinishRunRequest
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.data.running.model.StartRunRequest
import com.runway.android.domain.running.RunningRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WearRunSessionCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runningRepository: RunningRepository,
    private val manager: RunTrackingManager,
    private val pendingPointQueue: PendingPointQueue,
    private val sessionStore: TrackingSessionStore,
    private val stateSender: WatchStateSender,
    @Named("appScope") private val appScope: CoroutineScope,
) {
    private val commandMutex = Mutex()
    private var runId: String? = null
    private var goal: WatchRunGoal? = null
    private var batchJob: Job? = null
    private var stateJob: Job? = null

    fun handle(command: WatchCommand) {
        appScope.launch {
            commandMutex.withLock {
                when (command) {
                    WatchCommand.StartFreeRun -> start(WatchRunGoal.Free)
                    is WatchCommand.StartTimeGoalRun ->
                        start(WatchRunGoal.Time(command.targetMinutes))
                    is WatchCommand.StartDistanceGoalRun ->
                        start(WatchRunGoal.Distance(command.targetMeters))
                    is WatchCommand.StartIntervalRun -> start(
                        WatchRunGoal.Interval(
                            command.workSeconds,
                            command.restSeconds,
                            command.sets,
                        ),
                    )
                    WatchCommand.PauseRun -> pause()
                    WatchCommand.ResumeRun -> resume()
                    WatchCommand.FinishRun -> finish()
                    WatchCommand.AbandonRun -> abandon()
                }
            }
        }
    }

    private suspend fun start(newGoal: WatchRunGoal) {
        if (manager.state.value.isTracking || runId != null) {
            stateSender.sendState(
                manager.state.value,
                runId,
                status = "ERROR",
                goal = goal,
                error = "RUN_ALREADY_ACTIVE",
            )
            return
        }

        if (!hasTrackingPermissions()) {
            stateSender.sendState(
                manager.state.value,
                null,
                status = "ERROR",
                goal = newGoal,
                error = "PHONE_PERMISSION_REQUIRED",
            )
            return
        }

        goal = newGoal
        val serviceIntent = Intent(context, RunTrackingService::class.java).apply {
            action = RunTrackingAction.ACTION_START
            putExtra(RunTrackingAction.EXTRA_MODE, RunTrackingMode.FREE_RUN.name)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: RuntimeException) {
            stateSender.sendState(
                manager.state.value,
                null,
                status = "ERROR",
                goal = newGoal,
                error = "TRACKING_SERVICE_START_FAILED",
            )
            goal = null
            return
        }

        observeState()
        stateSender.sendState(manager.state.value, null, "STARTING", newGoal)

        when (val result = runningRepository.startRun(
            StartRunRequest(startedAt = Instant.now().toString()),
        )) {
            is NetworkResult.Success -> {
                runId = result.data.runId
                sessionStore.saveSnapshot(
                    TrackingSessionSnapshot(
                        runningRecordId = result.data.runId,
                        courseAttemptId = null,
                        elapsedSeconds = 0,
                        distanceMeters = 0.0,
                    ),
                )
                startBatchSaving()
                stateSender.sendState(manager.state.value, runId, "RUNNING", newGoal)
            }
            is NetworkResult.ApiError,
            is NetworkResult.NetworkError -> {
                stopTrackingService()
                stateSender.sendState(
                    manager.state.value,
                    null,
                    status = "ERROR",
                    goal = newGoal,
                    error = "RUN_CREATE_FAILED",
                )
                clearLocalSession()
            }
        }
    }

    private suspend fun pause() {
        val currentRunId = runId ?: return sendNoActiveRun()
        manager.pause()
        when (runningRepository.pauseRun(currentRunId)) {
            is NetworkResult.Success ->
                stateSender.sendState(manager.state.value, currentRunId, "PAUSED", goal)
            else -> {
                manager.resume()
                stateSender.sendState(
                    manager.state.value,
                    currentRunId,
                    "ERROR",
                    goal,
                    "PAUSE_FAILED",
                )
            }
        }
    }

    private suspend fun resume() {
        val currentRunId = runId ?: return sendNoActiveRun()
        manager.resume()
        when (runningRepository.resumeRun(currentRunId)) {
            is NetworkResult.Success ->
                stateSender.sendState(manager.state.value, currentRunId, "RUNNING", goal)
            else -> {
                manager.pause()
                stateSender.sendState(
                    manager.state.value,
                    currentRunId,
                    "ERROR",
                    goal,
                    "RESUME_FAILED",
                )
            }
        }
    }

    private suspend fun finish() {
        val currentRunId = runId ?: return sendNoActiveRun()
        val state = manager.state.value
        batchJob?.cancel()
        flushPendingPoints(currentRunId, drain = true)

        val distanceKm = state.distanceMeters / 1000.0
        val result = runningRepository.finishRun(
            currentRunId,
            FinishRunRequest(
                endedAt = Instant.now().toString(),
                distanceMeters = state.distanceMeters,
                durationSeconds = state.elapsedSeconds,
                avgPaceSecondsPerKm = if (distanceKm > 0.001) {
                    (state.elapsedSeconds / distanceKm).toInt()
                } else {
                    0
                },
                caloriesBurned = (distanceKm * 72).toInt(),
            ),
        )

        if (result is NetworkResult.Success) {
            stateSender.sendState(state, currentRunId, "FINISHED", goal)
            sessionStore.clearSnapshot()
            pendingPointQueue.deleteByRunningRecordId(currentRunId)
            stopTrackingService()
            clearLocalSession()
        } else {
            stateSender.sendState(state, currentRunId, "ERROR", goal, "FINISH_FAILED")
        }
    }

    private suspend fun abandon() {
        val currentRunId = runId
        if (currentRunId != null) {
            runningRepository.abandonRun(currentRunId)
            pendingPointQueue.deleteByRunningRecordId(currentRunId)
        }
        sessionStore.clearSnapshot()
        stateSender.sendState(manager.state.value, currentRunId, "ABANDONED", goal)
        stopTrackingService()
        clearLocalSession()
    }

    private fun observeState() {
        stateJob?.cancel()
        stateJob = appScope.launch {
            manager.state.collectLatest { state ->
                if (state.isTracking) {
                    stateSender.sendState(
                        state,
                        runId,
                        if (state.isPaused) "PAUSED" else "RUNNING",
                        goal,
                    )
                }
            }
        }
    }

    private fun startBatchSaving() {
        batchJob?.cancel()
        batchJob = appScope.launch {
            while (true) {
                delay(5_000)
                val currentRunId = runId ?: continue
                flushPendingPoints(currentRunId)
                val state = manager.state.value
                sessionStore.saveSnapshot(
                    TrackingSessionSnapshot(
                        runningRecordId = currentRunId,
                        courseAttemptId = null,
                        elapsedSeconds = state.elapsedSeconds,
                        distanceMeters = state.distanceMeters,
                    ),
                )
            }
        }
    }

    private suspend fun flushPendingPoints(currentRunId: String, drain: Boolean = false) {
        pendingPointQueue.add(currentRunId, manager.consumePoints())
        var attempts = 0
        do {
            val batch = pendingPointQueue.dequeue(currentRunId, 50)
            if (batch.isEmpty()) return
            when (runningRepository.savePoints(
                currentRunId,
                SavePointsRequest(batch.map { it.toRunPointRequest() }),
            )) {
                is NetworkResult.Success -> {
                    pendingPointQueue.deleteByIds(batch.map { it.id })
                    attempts = 0
                }
                else -> {
                    attempts++
                    if (!drain || attempts >= 3) return
                }
            }
        } while (drain)
    }

    private suspend fun sendNoActiveRun() {
        stateSender.sendState(
            manager.state.value,
            null,
            status = "ERROR",
            goal = goal,
            error = "NO_ACTIVE_RUN",
        )
    }

    private fun stopTrackingService() {
        context.startService(
            Intent(context, RunTrackingService::class.java).apply {
                action = RunTrackingAction.ACTION_STOP
            },
        )
    }

    private fun clearLocalSession() {
        batchJob?.cancel()
        stateJob?.cancel()
        batchJob = null
        stateJob = null
        runId = null
        goal = null
    }

    private fun hasTrackingPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val activityRecognitionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION,
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocationGranted && activityRecognitionGranted
    }
}
