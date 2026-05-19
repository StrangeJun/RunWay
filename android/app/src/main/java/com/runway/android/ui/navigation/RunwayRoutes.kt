package com.runway.android.ui.navigation

import android.net.Uri

object RunwayRoutes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val MAIN = "main"
    const val RUNNING = "running"
    const val RUN_RESULT = "run_result"
    const val COURSE_DETAIL = "course_detail/{courseId}"

    fun courseDetail(courseId: String) = "course_detail/${Uri.encode(courseId)}"

    fun runResult(runId: String, elapsedSeconds: Int, distanceKm: Float) =
        "$RUN_RESULT/${Uri.encode(runId)}/$elapsedSeconds/$distanceKm"
}
