package com.runway.android.data.running

import com.runway.android.core.model.PageResponse
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.result.safeApiCall
import com.runway.android.core.result.safeApiCallUnit
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
import com.runway.android.data.running.remote.RunningApi
import com.runway.android.domain.running.RunningRepository
import com.runway.android.domain.training.TrainingRecommendation
import com.runway.android.domain.training.TrainingRecommendationRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunningRepositoryImpl @Inject constructor(
    private val runningApi: RunningApi,
) : RunningRepository {

    override suspend fun getTrainingRecommendation(
        request: TrainingRecommendationRequest,
    ): NetworkResult<TrainingRecommendation> =
        safeApiCall { runningApi.getTrainingRecommendation(request) }

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

    override suspend fun getMyRuns(page: Int, size: Int): NetworkResult<PageResponse<RunSummaryResponse>> =
        safeApiCall { runningApi.getMyRuns(page, size) }

    override suspend fun getRunDetail(runId: String): NetworkResult<RunDetailResponse> =
        safeApiCall { runningApi.getRunDetail(runId) }

    override suspend fun getPersonalRecords(): NetworkResult<PersonalRecordsResponse> =
        safeApiCall { runningApi.getPersonalRecords() }

    override suspend fun getRunningStats(period: String): NetworkResult<RunningStatsResponse> =
        safeApiCall { runningApi.getRunningStats(period) }

    override suspend fun deleteRun(runId: String): NetworkResult<Unit> =
        safeApiCallUnit { runningApi.deleteRun(runId) }

    override suspend fun trimRun(runId: String, targetDistanceMeters: Double): NetworkResult<Unit> =
        safeApiCallUnit { runningApi.trimRun(runId, TrimRunRequest(targetDistanceMeters)) }
}
