package com.runway.android.domain.user

import com.runway.android.core.result.NetworkResult
import com.runway.android.data.user.model.UserProfileResponse

interface UserRepository {
    suspend fun getMe(): NetworkResult<UserProfileResponse>
}
