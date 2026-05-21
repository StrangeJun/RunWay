package com.runway.android.core.cadence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneStepCadenceTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) : CadenceTracker {

    override fun cadenceSpmFlow(): Flow<Int?> = callbackFlow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepDetector == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val stepTimesMillis = ArrayDeque<Long>()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = System.currentTimeMillis()
                stepTimesMillis.addLast(now)

                val windowStart = now - CADENCE_WINDOW_MILLIS
                while ((stepTimesMillis.peekFirst() ?: Long.MAX_VALUE) < windowStart) {
                    stepTimesMillis.removeFirst()
                }

                val cadence = (stepTimesMillis.size * 60_000.0 / CADENCE_WINDOW_MILLIS).toInt()
                trySend(cadence.coerceAtLeast(0))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            stepDetector,
            SensorManager.SENSOR_DELAY_NORMAL,
        )

        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private companion object {
        const val CADENCE_WINDOW_MILLIS = 20_000L
    }
}
