package com.runway.android.data.running

import com.runway.android.core.result.NetworkResult
import com.runway.android.core.result.safeApiCall
import com.runway.android.data.running.model.FinishRunRequest
import com.runway.android.data.running.model.FinishRunResponse
import com.runway.android.data.running.model.RunStatusResponse
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.data.running.model.SavePointsResponse
import com.runway.android.data.running.model.StartRunRequest
import com.runway.android.data.running.model.StartRunResponse
import com.runway.android.data.running.remote.RunningApi
import com.runway.android.domain.running.RunningRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunningRepositoryImpl @Inject constructor(
    private val runningApi: RunningApi,
) : RunningRepository {

    override suspend fun startRun(request: StartRunRequest): NetworkResult<StartRunResponse> =
        safeApiCall { runningApi.startRun(request) }

    override suspend fun savePoints(runId: String, request: SavePointsRequest): NetworkResult<SavePointsResponse> =
        safeApiCall { runningApi.savePoints(runId, request) }

    override suspend fun pauseRun(runId: String): NetworkResult<RunStatusResponse> =
        safeApiCall { runningApi.pauseRun(runId) }

    override suspend fun resumeRun(runId: String): NetworkResult<RunStatusResponse> =
        safeApiCall { runningApi.resumeRun(runId) }

    override suspend fun finishRun(runId: String, request: FinishRunRequest): NetworkResult<FinishRunResponse> =
        safeApiCall { runningApi.finishRun(runId, request) }

    override suspend fun abandonRun(runId: String): NetworkResult<RunStatusResponse> =
        safeApiCall { runningApi.abandonRun(runId) }
}
