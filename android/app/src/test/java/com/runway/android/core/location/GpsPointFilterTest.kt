package com.runway.android.core.location

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GpsPointFilterTest {
    private lateinit var filter: GpsPointFilter

    @Before
    fun setUp() {
        filter = GpsPointFilter()
    }

    @Test
    fun rejectsPoorAccuracy() {
        assertNull(filter.filter(point(latitude = 37.0, accuracy = 80f)))
    }

    @Test
    fun removesStationaryDrift() {
        assertNotNull(filter.filter(point(latitude = 37.0, elapsedSeconds = 0)))

        val result = filter.filter(
            point(latitude = 37.00001, elapsedSeconds = 3, accuracy = 12f),
        )

        assertNotNull(result)
        assertFalse(result!!.shouldRecord)
        assertEquals(0.0, result.distanceMeters, 0.0)
        assertEquals(37.0, result.location.latitude, 0.0)
    }

    @Test
    fun rejectsTeleportWithoutPoisoningNextPoint() {
        assertNotNull(filter.filter(point(latitude = 37.0, elapsedSeconds = 0)))
        assertNull(filter.filter(point(latitude = 37.01, elapsedSeconds = 3)))

        val recovered = filter.filter(point(latitude = 37.00005, elapsedSeconds = 6))

        assertNotNull(recovered)
        assertTrue(recovered!!.shouldRecord)
        assertTrue(recovered.distanceMeters in 5.0..6.5)
    }

    @Test
    fun rejectsOutOfOrderSample() {
        assertNotNull(filter.filter(point(latitude = 37.0, elapsedSeconds = 5)))
        assertNull(filter.filter(point(latitude = 37.00001, elapsedSeconds = 4)))
    }

    private fun point(
        latitude: Double,
        elapsedSeconds: Long = 0,
        accuracy: Float = 8f,
    ) = RunwayLocation(
        latitude = latitude,
        longitude = 127.0,
        altitudeMeters = 100.0,
        speedMps = null,
        recordedAt = Instant.EPOCH.plusSeconds(elapsedSeconds),
        horizontalAccuracyMeters = accuracy,
        elapsedRealtimeNanos = elapsedSeconds * 1_000_000_000L,
    )
}
