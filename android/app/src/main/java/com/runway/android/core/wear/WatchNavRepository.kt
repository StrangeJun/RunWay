package com.runway.android.core.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchNavRepository @Inject constructor() {
    private val _pendingCourseId = MutableStateFlow<String?>(null)
    val pendingCourseId = _pendingCourseId.asStateFlow()

    private val _pendingRunId = MutableStateFlow<String?>(null)
    val pendingRunId = _pendingRunId.asStateFlow()

    fun requestOpenCourse(courseId: String) {
        _pendingCourseId.value = courseId
    }

    fun consumeCourse() {
        _pendingCourseId.value = null
    }

    fun requestOpenRun(runId: String) {
        _pendingRunId.value = runId
    }

    fun consumeRun() {
        _pendingRunId.value = null
    }

    @Deprecated("use consumeCourse or consumeRun")
    fun consume() = consumeCourse()
}
