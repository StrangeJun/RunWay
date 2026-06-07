package com.runway.android.core.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class RunningVoiceTextTest {
    @Test
    fun `formats kilometer time and average pace naturally`() {
        assertEquals(
            "2킬로미터 완료. 시간 12분 20초. 평균 페이스 6분 10초.",
            RunningVoiceText.kilometer(kilometers = 2, elapsedSeconds = 740),
        )
    }
}
