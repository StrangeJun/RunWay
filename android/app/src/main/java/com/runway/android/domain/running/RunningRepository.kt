package com.runway.android.domain.running

import com.runway.android.core.result.NetworkResult
import com.runway.android.data.running.model.FinishRunRequest
import com.runway.android.data.running.model.FinishRunResponse
import com.runway.android.data.running.model.RunStatusResponse
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.data.running.model.SavePointsResponse
import com.runway.android.data.running.model.StartRunRequest
import com.runway.android.data.running.model.StartRunResponse

interface RunningRepository {
    suspend fun startRun(request: StartRunRequest): NetworkResult<StartRunResponse>
    suspend fun savePoints(runId: String, request: SavePointsRequest): NetworkResult<SavePointsResponse>
    suspend fun pauseRun(runId: String): NetworkResult<RunStatusResponse>
    suspend fun resumeRun(runId: String): NetworkResult<RunStatusResponse>
    suspend fun finishRun(runId: String, request: FinishRunRequest): NetworkResult<FinishRunResponse>
    suspend fun abandonRun(runId: String): NetworkResult<RunStatusResponse>
}
