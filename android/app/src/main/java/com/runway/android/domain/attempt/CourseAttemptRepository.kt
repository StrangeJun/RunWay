package com.runway.android.domain.attempt

import com.runway.android.core.result.NetworkResult
import com.runway.android.data.attempt.model.AbandonAttemptRequest
import com.runway.android.data.attempt.model.AbandonAttemptResponse
import com.runway.android.data.attempt.model.FinishAttemptRequest
import com.runway.android.data.attempt.model.FinishAttemptResponse
import com.runway.android.data.attempt.model.LeaderboardResponse
import com.runway.android.data.attempt.model.MyBestAttemptResponse
import com.runway.android.data.attempt.model.StartAttemptRequest
import com.runway.android.data.attempt.model.StartAttemptResponse

interface CourseAttemptRepository {

    suspend fun startAttempt(
        courseId: String,
        request: StartAttemptRequest,
    ): NetworkResult<StartAttemptResponse>

    suspend fun finishAttempt(
        attemptId: String,
        request: FinishAttemptRequest,
    ): NetworkResult<FinishAttemptResponse>

    suspend fun abandonAttempt(
        attemptId: String,
        request: AbandonAttemptRequest,
    ): NetworkResult<AbandonAttemptResponse>

    suspend fun getLeaderboard(
        courseId: String,
        page: Int = 0,
        size: Int = 50,
        sortBy: String = "fastest_time",
    ): NetworkResult<LeaderboardResponse>

    suspend fun getMyBestAttempt(courseId: String): NetworkResult<MyBestAttemptResponse>
}
