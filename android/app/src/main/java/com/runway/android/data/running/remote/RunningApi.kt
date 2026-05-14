package com.runway.android.data.running.remote

import com.runway.android.core.model.ApiResponse
import com.runway.android.data.running.model.FinishRunRequest
import com.runway.android.data.running.model.FinishRunResponse
import com.runway.android.data.running.model.RunStatusResponse
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.data.running.model.SavePointsResponse
import com.runway.android.data.running.model.StartRunRequest
import com.runway.android.data.running.model.StartRunResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface RunningApi {

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
}
