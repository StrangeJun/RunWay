package com.runway.wear.health

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WatchGpsPointFilterTest {
    private lateinit var filter: WatchGpsPointFilter

    @Before
    fun setUp() {
        filter = WatchGpsPointFilter()
    }

    @Test
    fun removesStationaryDrift() {
        assertNotNull(filter.filter(point(latitude = 37.0, elapsedSeconds = 0)))

        val result = filter.filter(point(latitude = 37.00001, elapsedSeconds = 3))

        assertNotNull(result)
        assertFalse(result!!.shouldRecord)
    }

    @Test
    fun rejectsTeleportWithoutPoisoningNextPoint() {
        assertNotNull(filter.filter(point(latitude = 37.0, elapsedSeconds = 0)))
        assertNull(filter.filter(point(latitude = 37.01, elapsedSeconds = 3)))

        val recovered = filter.filter(point(latitude = 37.00005, elapsedSeconds = 6))

        assertNotNull(recovered)
        assertTrue(recovered!!.shouldRecord)
    }

    @Test
    fun rejectsOutOfOrderSample() {
        assertNotNull(filter.filter(point(latitude = 37.0, elapsedSeconds = 5)))
        assertNull(filter.filter(point(latitude = 37.00001, elapsedSeconds = 4)))
    }

    private fun point(
        latitude: Double,
        elapsedSeconds: Long,
    ) = HealthLocationSample(
        latitude = latitude,
        longitude = 127.0,
        altitudeMeters = 100.0,
        speedMps = 2.5,
        recordedAt = Instant.EPOCH.plusSeconds(elapsedSeconds).toString(),
    )
}
