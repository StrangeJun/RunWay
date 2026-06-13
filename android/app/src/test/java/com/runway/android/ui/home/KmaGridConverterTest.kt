package com.runway.android.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class KmaGridConverterTest {
    @Test
    fun convertsSeoulCityHallToKmaGrid() {
        assertEquals(KmaGrid(60, 127), KmaGridConverter.toGrid(37.5665, 126.9780))
    }

    @Test
    fun convertsChungjuLocationToKmaGrid() {
        assertEquals(KmaGrid(75, 114), KmaGridConverter.toGrid(36.969647, 127.868098))
    }
}
