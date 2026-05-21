package com.runway.android.data.user

import com.runway.android.core.result.NetworkResult
import com.runway.android.core.result.safeApiCall
import com.runway.android.data.user.model.AchievementsResponse
import com.runway.android.data.user.model.UpdateProfileRequest
import com.runway.android.data.user.model.UserProfileResponse
import com.runway.android.data.user.remote.UserApi
import com.runway.android.domain.user.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
) : UserRepository {

    override suspend fun getMe(): NetworkResult<UserProfileResponse> =
        safeApiCall { userApi.getMe() }

    override suspend fun updateMe(request: UpdateProfileRequest): NetworkResult<UserProfileResponse> =
        safeApiCall { userApi.updateMe(request) }

    override suspend fun getAchievements(): NetworkResult<AchievementsResponse> =
        safeApiCall { userApi.getAchievements() }
}
