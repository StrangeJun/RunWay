package com.runway.android.data.training.remote

import com.runway.android.core.model.ApiResponse
import com.runway.android.data.training.model.TrainingPlanRequest
import com.runway.android.data.training.model.TrainingPlanResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TrainingPlanApi {
    @POST("api/training-plans/recommend")
    suspend fun recommend(
        @Body request: TrainingPlanRequest,
    ): ApiResponse<TrainingPlanResponse>
}
