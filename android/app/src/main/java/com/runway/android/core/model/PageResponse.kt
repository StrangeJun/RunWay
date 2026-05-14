package com.runway.android.core.model

/**
 * 백엔드 페이지네이션 응답 wrapper.
 * ApiResponse<PageResponse<T>> 형태로 사용된다.
 *
 * GET /api/runs/me?page=0&size=20 응답의 data 필드 형식.
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)
