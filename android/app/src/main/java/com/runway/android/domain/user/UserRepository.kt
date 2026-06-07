package com.runway.android.domain.user

import android.net.Uri
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.user.model.AchievementsResponse
import com.runway.android.data.user.model.UpdateProfileRequest
import com.runway.android.data.user.model.UserProfileResponse

interface UserRepository {
    suspend fun getMe(): NetworkResult<UserProfileResponse>
    suspend fun updateMe(request: UpdateProfileRequest): NetworkResult<UserProfileResponse>
    suspend fun uploadProfileImage(uri: Uri): NetworkResult<String>
    suspend fun getAchievements(): NetworkResult<AchievementsResponse>
}
