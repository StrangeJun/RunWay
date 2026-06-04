package com.runway.android.ui.navigation

import android.net.Uri

object RunwayRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val ONBOARDING = "onboarding"
    const val PERMISSION = "permission"
    const val MAIN = "main"
    const val RUNNING = "running"
    const val RUN_RESULT = "run_result"
    const val MY_RUNS = "my_runs"
    const val MY_COURSES = "my_courses"
    const val RUN_DETAIL = "run_detail/{runId}"
    const val COURSE_DETAIL = "course_detail/{courseId}"
    const val COURSE_ATTEMPT = "course_attempt/{courseId}/{courseAttemptId}/{runningRecordId}"
    const val COURSE_LEADERBOARD = "course_leaderboard/{courseId}?isPR={isPR}&previousBestSeconds={previousBestSeconds}&improvementSeconds={improvementSeconds}&justCompleted={justCompleted}"
    const val COURSE_MAP_DETAIL = "course_map_detail/{courseId}"
    const val RUN_SHARE = "run_share/{runId}"
    const val STATS = "stats"
    const val ACHIEVEMENTS = "achievements"
    const val REMINDER = "reminder"
    const val SETTINGS = "settings"
    const val POSTURE_CAPTURE = "posture_capture"
    const val POSTURE_RESULT = "posture_result/{analysisId}"

    fun postureResult(analysisId: String) = "posture_result/${Uri.encode(analysisId)}"

    fun courseDetail(courseId: String) = "course_detail/${Uri.encode(courseId)}"
    fun runShare(runId: String) = "run_share/${Uri.encode(runId)}"

    fun runResult(runId: String, elapsedSeconds: Int, distanceKm: Float) =
        "$RUN_RESULT/${Uri.encode(runId)}/$elapsedSeconds/$distanceKm"

    fun runDetail(runId: String) = "run_detail/${Uri.encode(runId)}"

    fun courseAttempt(
        courseId: String,
        courseAttemptId: String,
        runningRecordId: String,
    ) = "course_attempt/${Uri.encode(courseId)}/${Uri.encode(courseAttemptId)}/${Uri.encode(runningRecordId)}"

    fun courseLeaderboard(
        courseId: String,
        isPR: Boolean = false,
        previousBestSeconds: Int? = null,
        improvementSeconds: Int? = null,
        justCompleted: Boolean = false,
    ) = buildString {
        append("course_leaderboard/${Uri.encode(courseId)}")
        append("?isPR=$isPR")
        append("&justCompleted=$justCompleted")
        if (previousBestSeconds != null) append("&previousBestSeconds=$previousBestSeconds")
        if (improvementSeconds != null) append("&improvementSeconds=$improvementSeconds")
    }
    fun courseMapDetail(courseId: String) = "course_map_detail/${Uri.encode(courseId)}"
}
