package com.runway.wear.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class RunningVoiceTextTest {
    @Test
    fun `formats kilometer guide with heart rate`() {
        assertEquals(
            "2킬로미터 완료. 시간 12분 20초. 평균 페이스 6분 10초. 현재 심박수 152.",
            RunningVoiceText.kilometer(2, 740, 152),
        )
    }
}
