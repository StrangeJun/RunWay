package com.runway.android.core.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.runway.android.MainActivity
import com.runway.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunTrackingNotification @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "runway_tracking"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "RunWay Tracking",
            NotificationManager.IMPORTANCE_LOW, // LOW = no sound, no vibration
        ).apply {
            description = "Shown while running or course attempt tracking is active"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun build(state: RunTrackingState): Notification {
        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val modeLabel = when (state.mode) {
            RunTrackingMode.FREE_RUN -> "Running"
            RunTrackingMode.COURSE_ATTEMPT -> "Course attempt"
        }
        val timeStr = formatElapsed(state.elapsedSeconds)
        val distStr = "%.2f km".format(state.distanceMeters / 1000.0)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("RunWay tracking")
            .setContentText("$modeLabel · $timeStr · $distStr")
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    fun update(state: RunTrackingState) {
        notificationManager.notify(NOTIFICATION_ID, build(state))
    }

    private fun formatElapsed(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
