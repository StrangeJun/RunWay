package com.runway.android.core.network

object NetworkConstants {
    /**
     * Android 에뮬레이터: 10.0.2.2 → 호스트 머신의 localhost (백엔드 포트 8080)
     * 실기기 테스트: 호스트 머신의 실제 IP로 교체 (예: "http://192.168.x.x:8080/")
     */
    const val BASE_URL = "http://10.0.2.2:8080/"

    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L
}
