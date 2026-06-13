package com.runway.android

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.runway.android.BuildConfig
import com.runway.android.core.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RunwayApplication : Application() {

    @Inject
    lateinit var notificationChannels: NotificationChannels

    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        notificationChannels.createAll()
    }
}
