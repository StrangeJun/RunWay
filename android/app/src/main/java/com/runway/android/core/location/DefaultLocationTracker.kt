package com.runway.android.core.location

import android.annotation.SuppressLint
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// NOTE: Real GPS requires a physical device or emulator with mock location input.
// In Android Emulator: use Extended Controls → Location to send simulated GPS points,
// or use the "gpx" / "kml" route playback feature for continuous movement simulation.
@Singleton
class DefaultLocationTracker @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
) : LocationTracker {

    // Requires ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION — caller must verify before collecting.
    @SuppressLint("MissingPermission")
    override fun locationFlow(): Flow<RunwayLocation> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { loc ->
                    trySend(
                        RunwayLocation(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            altitudeMeters = if (loc.hasAltitude()) loc.altitude else null,
                            speedMps = if (loc.hasSpeed()) loc.speed else null,
                            recordedAt = Instant.ofEpochMilli(loc.time),
                            horizontalAccuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                            speedAccuracyMps = if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                loc.hasSpeedAccuracy()
                            ) {
                                loc.speedAccuracyMetersPerSecond
                            } else {
                                null
                            },
                            elapsedRealtimeNanos = loc.elapsedRealtimeNanos.takeIf { it > 0L },
                        )
                    )
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose { fusedLocationClient.removeLocationUpdates(callback) }
    }
}
