package com.runway.android.data.running.remote

import com.runway.android.core.model.ApiResponse
import com.runway.android.core.model.PageResponse
import com.runway.android.data.running.model.FinishRunRequest
import com.runway.android.data.running.model.FinishRunResponse
import com.runway.android.data.running.model.PersonalRecordsResponse
import com.runway.android.data.running.model.RunDetailResponse
import com.runway.android.data.running.model.RunningStatsResponse
import com.runway.android.data.running.model.RunStatusResponse
import com.runway.android.data.running.model.RunSummaryResponse
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.data.running.model.SavePointsResponse
import com.runway.android.data.running.model.StartRunRequest
import com.runway.android.data.running.model.StartRunResponse
import com.runway.android.data.running.model.TrimRunRequest
import com.runway.android.domain.training.TrainingRecommendation
import com.runway.android.domain.training.TrainingRecommendationRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RunningApi {

    @POST("api/training/recommendation")
    suspend fun getTrainingRecommendation(
        @Body request: TrainingRecommendationRequest,
    ): ApiResponse<TrainingRecommendation>

    @POST("api/runs/start")
    suspend fun startRun(@Body request: StartRunRequest): ApiResponse<StartRunResponse>

    @POST("api/runs/{runId}/points")
    suspend fun savePoints(
        @Path("runId") runId: String,
        @Body request: SavePointsRequest,
    ): ApiResponse<SavePointsResponse>

    @POST("api/runs/{runId}/pause")
    suspend fun pauseRun(@Path("runId") runId: String): ApiResponse<RunStatusResponse>

    @POST("api/runs/{runId}/resume")
    suspend fun resumeRun(@Path("runId") runId: String): ApiResponse<RunStatusResponse>

    @POST("api/runs/{runId}/finish")
    suspend fun finishRun(
        @Path("runId") runId: String,
        @Body request: FinishRunRequest,
    ): ApiResponse<FinishRunResponse>

    @POST("api/runs/{runId}/abandon")
    suspend fun abandonRun(@Path("runId") runId: String): ApiResponse<RunStatusResponse>

    @GET("api/runs/me")
    suspend fun getMyRuns(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiResponse<PageResponse<RunSummaryResponse>>

    @GET("api/runs/{runId}")
    suspend fun getRunDetail(@Path("runId") runId: String): ApiResponse<RunDetailResponse>

    @GET("api/runs/me/records")
    suspend fun getPersonalRecords(): ApiResponse<PersonalRecordsResponse>

    @GET("api/runs/me/stats")
    suspend fun getRunningStats(
        @Query("period") period: String = "monthly",
    ): ApiResponse<RunningStatsResponse>

    @DELETE("api/runs/{runId}")
    suspend fun deleteRun(@Path("runId") runId: String): ApiResponse<Unit>

    @PATCH("api/runs/{runId}/trim")
    suspend fun trimRun(
        @Path("runId") runId: String,
        @Body request: TrimRunRequest,
    ): ApiResponse<Unit>
}
