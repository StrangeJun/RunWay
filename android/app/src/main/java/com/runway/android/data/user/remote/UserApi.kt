package com.runway.android.data.user.remote

import com.runway.android.core.model.ApiResponse
import com.runway.android.data.user.model.AchievementsResponse
import com.runway.android.data.user.model.UpdateProfileRequest
import com.runway.android.data.user.model.UserProfileResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface UserApi {

    @GET("api/users/me")
    suspend fun getMe(): ApiResponse<UserProfileResponse>

    @PUT("api/users/me")
    suspend fun updateMe(@Body request: UpdateProfileRequest): ApiResponse<UserProfileResponse>

    @Multipart
    @POST("api/users/me/profile-image")
    suspend fun uploadProfileImage(@Part image: MultipartBody.Part): ApiResponse<String>

    @GET("api/users/me/achievements")
    suspend fun getAchievements(): ApiResponse<AchievementsResponse>
}
