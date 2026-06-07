package com.runway.wear.health

import android.annotation.SuppressLint
import android.content.Context
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.LocationAvailability
import android.os.SystemClock
import java.time.Instant
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.pauseExercise
import androidx.health.services.client.resumeExercise
import androidx.health.services.client.startExercise
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

data class HealthMetricUpdate(
    val distanceMeters: Double? = null,
    val heartRateBpm: Int? = null,
    val cadenceSpm: Int? = null,
    val gpsStatus: String? = null,
    val isAutoPaused: Boolean? = null,
    val locations: List<HealthLocationSample> = emptyList(),
)

data class HealthLocationSample(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val speedMps: Double,
    val recordedAt: String,
)

@SuppressLint("RestrictedApi")
class HealthServicesManager(context: Context) {
    private val exerciseClient = HealthServices.getClient(context.applicationContext).exerciseClient

    suspend fun start(autoPauseEnabled: Boolean) {
        val capabilities = exerciseClient.getCapabilities()
            .getExerciseTypeCapabilities(ExerciseType.RUNNING)
        val requested = setOf(
            DataType.DISTANCE_TOTAL,
            DataType.HEART_RATE_BPM,
            DataType.STEPS_PER_MINUTE,
            DataType.LOCATION,
            DataType.SPEED,
        ).intersect(capabilities.supportedDataTypes)

        exerciseClient.startExercise(
            ExerciseConfig(
                exerciseType = ExerciseType.RUNNING,
                dataTypes = requested,
                isAutoPauseAndResumeEnabled = autoPauseEnabled,
                isGpsEnabled = true,
            ),
        )
    }

    suspend fun pause() = exerciseClient.pauseExercise()
    suspend fun resume() = exerciseClient.resumeExercise()
    suspend fun finish() = exerciseClient.endExercise()

    val updates = callbackFlow {
        val callback = object : ExerciseUpdateCallback {
            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                val metrics = update.latestMetrics
                val bootInstant = Instant.now().minusMillis(SystemClock.elapsedRealtime())
                val speed = metrics.getData(DataType.SPEED).lastOrNull()?.value ?: 0.0
                trySendBlocking(
                    HealthMetricUpdate(
                        distanceMeters = metrics.getData(DataType.DISTANCE_TOTAL)?.total,
                        heartRateBpm = metrics.getData(DataType.HEART_RATE_BPM)
                            .lastOrNull()?.value?.toInt(),
                        cadenceSpm = metrics.getData(DataType.STEPS_PER_MINUTE)
                            .lastOrNull()?.value?.toInt(),
                        isAutoPaused = when (update.exerciseStateInfo.state) {
                            ExerciseState.AUTO_PAUSING,
                            ExerciseState.AUTO_PAUSED,
                            -> true
                            ExerciseState.AUTO_RESUMING,
                            ExerciseState.ACTIVE,
                            -> false
                            else -> null
                        },
                        locations = metrics.getData(DataType.LOCATION).map { point ->
                            HealthLocationSample(
                                latitude = point.value.latitude,
                                longitude = point.value.longitude,
                                altitudeMeters = point.value.altitude
                                    .takeUnless { it == Double.MIN_VALUE || !it.isFinite() }
                                    ?: 0.0,
                                speedMps = speed,
                                recordedAt = point.getTimeInstant(bootInstant).toString(),
                            )
                        },
                    ),
                )
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit
            override fun onRegistered() = Unit
            override fun onRegistrationFailed(throwable: Throwable) {
                close(throwable)
            }

            override fun onAvailabilityChanged(
                dataType: DataType<*, *>,
                availability: Availability,
            ) {
                if (availability is LocationAvailability) {
                    trySendBlocking(
                        HealthMetricUpdate(
                            gpsStatus = when (availability) {
                                LocationAvailability.ACQUIRED_TETHERED,
                                LocationAvailability.ACQUIRED_UNTETHERED -> "GOOD"
                                LocationAvailability.NO_GNSS -> "POOR"
                                else -> "SEARCHING"
                            },
                        ),
                    )
                }
            }
        }
        exerciseClient.setUpdateCallback(callback)
        awaitClose { exerciseClient.clearUpdateCallbackAsync(callback) }
    }
}
