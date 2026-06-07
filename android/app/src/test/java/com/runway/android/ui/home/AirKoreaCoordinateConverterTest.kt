package com.runway.android.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test

class AirKoreaCoordinateConverterTest {
    @Test
    fun `converts Seoul City Hall coordinates to AirKorea TM`() {
        val result = AirKoreaCoordinateConverter.toTm(
            latitude = 37.5665,
            longitude = 126.9780,
        )

        assertTrue("Unexpected TM x: ${result.x}", result.x in 197_000.0..199_000.0)
        assertTrue("Unexpected TM y: ${result.y}", result.y in 449_000.0..453_000.0)
    }
}
