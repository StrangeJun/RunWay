package com.runway.android.core.model

/**
 * 백엔드 공통 응답 wrapper.
 *
 * 성공: { "success": true,  "message": "...", "data": {...} }
 * 실패: { "success": false, "message": "...", "errorCode": "SOME_CODE", "data": null }
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val errorCode: String? = null,
)
