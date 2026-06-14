package com.runway.android.domain.running

import com.runway.android.core.model.PageResponse
import com.runway.android.core.result.NetworkResult
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
import com.runway.android.domain.training.TrainingRecommendation
import com.runway.android.domain.training.TrainingRecommendationRequest

interface RunningRepository {
    suspend fun getTrainingRecommendation(
        request: TrainingRecommendationRequest,
    ): NetworkResult<TrainingRecommendation>

    suspend fun startRun(request: StartRunRequest): NetworkResult<StartRunResponse>
    suspend fun savePoints(runId: String, request: SavePointsRequest): NetworkResult<SavePointsResponse>
    suspend fun pauseRun(runId: String): NetworkResult<RunStatusResponse>
    suspend fun resumeRun(runId: String): NetworkResult<RunStatusResponse>
    suspend fun finishRun(runId: String, request: FinishRunRequest): NetworkResult<FinishRunResponse>
    suspend fun abandonRun(runId: String): NetworkResult<RunStatusResponse>
    suspend fun getMyRuns(page: Int = 0, size: Int = 20): NetworkResult<PageResponse<RunSummaryResponse>>
    suspend fun getRunDetail(runId: String): NetworkResult<RunDetailResponse>
    suspend fun getPersonalRecords(): NetworkResult<PersonalRecordsResponse>
    suspend fun getRunningStats(period: String = "monthly"): NetworkResult<RunningStatsResponse>
    suspend fun deleteRun(runId: String): NetworkResult<Unit>
    suspend fun trimRun(runId: String, targetDistanceMeters: Double): NetworkResult<Unit>
}
