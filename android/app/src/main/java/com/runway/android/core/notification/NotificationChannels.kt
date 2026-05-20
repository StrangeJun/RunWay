package com.runway.android.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_REMINDER = "runway_reminder"
        const val CHANNEL_TRACKING = "runway_tracking"
    }

    fun createAll() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDER,
                "러닝 리마인더",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "예약된 러닝 리마인더 알림" },
        )
    }
}
