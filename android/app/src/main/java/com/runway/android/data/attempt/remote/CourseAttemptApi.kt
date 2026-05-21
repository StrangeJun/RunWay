package com.runway.android.data.attempt.remote

import com.runway.android.core.model.ApiResponse
import com.runway.android.data.attempt.model.AbandonAttemptRequest
import com.runway.android.data.attempt.model.AbandonAttemptResponse
import com.runway.android.data.attempt.model.FinishAttemptRequest
import com.runway.android.data.attempt.model.FinishAttemptResponse
import com.runway.android.data.attempt.model.LeaderboardResponse
import com.runway.android.data.attempt.model.StartAttemptRequest
import com.runway.android.data.attempt.model.StartAttemptResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CourseAttemptApi {

    @POST("api/courses/{courseId}/attempts/start")
    suspend fun startAttempt(
        @Path("courseId") courseId: String,
        @Body request: StartAttemptRequest,
    ): ApiResponse<StartAttemptResponse>

    @POST("api/course-attempts/{attemptId}/finish")
    suspend fun finishAttempt(
        @Path("attemptId") attemptId: String,
        @Body request: FinishAttemptRequest,
    ): ApiResponse<FinishAttemptResponse>

    @POST("api/course-attempts/{attemptId}/abandon")
    suspend fun abandonAttempt(
        @Path("attemptId") attemptId: String,
        @Body request: AbandonAttemptRequest,
    ): ApiResponse<AbandonAttemptResponse>

    @GET("api/courses/{courseId}/leaderboard")
    suspend fun getLeaderboard(
        @Path("courseId") courseId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): ApiResponse<LeaderboardResponse>
}
