package com.runway.android.data.attempt

import com.runway.android.core.result.NetworkResult
import com.runway.android.core.result.safeApiCall
import com.runway.android.data.attempt.model.AbandonAttemptRequest
import com.runway.android.data.attempt.model.AbandonAttemptResponse
import com.runway.android.data.attempt.model.FinishAttemptRequest
import com.runway.android.data.attempt.model.FinishAttemptResponse
import com.runway.android.data.attempt.model.LeaderboardResponse
import com.runway.android.data.attempt.model.MyBestAttemptResponse
import com.runway.android.data.attempt.model.StartAttemptRequest
import com.runway.android.data.attempt.model.StartAttemptResponse
import com.runway.android.data.attempt.remote.CourseAttemptApi
import com.runway.android.domain.attempt.CourseAttemptRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseAttemptRepositoryImpl @Inject constructor(
    private val courseAttemptApi: CourseAttemptApi,
) : CourseAttemptRepository {

    override suspend fun startAttempt(
        courseId: String,
        request: StartAttemptRequest,
    ): NetworkResult<StartAttemptResponse> =
        safeApiCall { courseAttemptApi.startAttempt(courseId, request) }

    override suspend fun finishAttempt(
        attemptId: String,
        request: FinishAttemptRequest,
    ): NetworkResult<FinishAttemptResponse> =
        safeApiCall { courseAttemptApi.finishAttempt(attemptId, request) }

    override suspend fun abandonAttempt(
        attemptId: String,
        request: AbandonAttemptRequest,
    ): NetworkResult<AbandonAttemptResponse> =
        safeApiCall { courseAttemptApi.abandonAttempt(attemptId, request) }

    override suspend fun getLeaderboard(
        courseId: String,
        page: Int,
        size: Int,
        sortBy: String,
    ): NetworkResult<LeaderboardResponse> =
        safeApiCall { courseAttemptApi.getLeaderboard(courseId, page, size, sortBy) }

    override suspend fun getMyBestAttempt(courseId: String): NetworkResult<MyBestAttemptResponse> =
        safeApiCall { courseAttemptApi.getMyBestAttempt(courseId) }
}
