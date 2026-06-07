package com.runway.android.data.user

import android.content.Context
import android.net.Uri
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.result.safeApiCall
import com.runway.android.data.user.model.AchievementsResponse
import com.runway.android.data.user.model.UpdateProfileRequest
import com.runway.android.data.user.model.UserProfileResponse
import com.runway.android.data.user.remote.UserApi
import com.runway.android.domain.user.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userApi: UserApi,
) : UserRepository {

    override suspend fun getMe(): NetworkResult<UserProfileResponse> =
        safeApiCall { userApi.getMe() }

    override suspend fun updateMe(request: UpdateProfileRequest): NetworkResult<UserProfileResponse> =
        safeApiCall { userApi.updateMe(request) }

    override suspend fun uploadProfileImage(uri: Uri): NetworkResult<String> = safeApiCall {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open image")
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = stream.use { it.readBytes() }
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val ext = if (mimeType.contains("png")) ".png" else ".jpg"
        val part = MultipartBody.Part.createFormData("image", "profile$ext", requestBody)
        userApi.uploadProfileImage(part)
    }

    override suspend fun getAchievements(): NetworkResult<AchievementsResponse> =
        safeApiCall { userApi.getAchievements() }
}
