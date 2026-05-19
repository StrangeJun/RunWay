package com.runway.android.ui.navigation

import android.net.Uri

object RunwayRoutes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val MAIN = "main"
    const val RUNNING = "running"
    const val RUN_RESULT = "run_result"
    const val COURSE_DETAIL = "course_detail/{courseId}"
    const val COURSE_ATTEMPT = "course_attempt/{courseId}/{courseAttemptId}/{runningRecordId}"
    const val COURSE_LEADERBOARD = "course_leaderboard/{courseId}"

    fun courseDetail(courseId: String) = "course_detail/${Uri.encode(courseId)}"

    fun runResult(runId: String, elapsedSeconds: Int, distanceKm: Float) =
        "$RUN_RESULT/${Uri.encode(runId)}/$elapsedSeconds/$distanceKm"

    fun courseAttempt(
        courseId: String,
        courseAttemptId: String,
        runningRecordId: String,
    ) = "course_attempt/${Uri.encode(courseId)}/${Uri.encode(courseAttemptId)}/${Uri.encode(runningRecordId)}"

    fun courseLeaderboard(courseId: String) = "course_leaderboard/${Uri.encode(courseId)}"
}
