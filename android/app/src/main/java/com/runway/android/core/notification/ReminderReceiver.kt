package com.runway.android.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.runway.android.MainActivity
import com.runway.android.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val dayLabel = intent.getStringExtra(EXTRA_DAY_LABEL) ?: "오늘"
        val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1)
        val hour = intent.getIntExtra(EXTRA_HOUR, 7)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)

        showReminderNotification(context, dayLabel)

        // Exact alarm은 one-shot — 다음 주 같은 요일로 재예약
        if (dayOfWeek != -1) {
            scheduler.schedule(dayOfWeek, hour, minute)
        }
    }

    private fun showReminderNotification(context: Context, dayLabel: String) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("오늘 런 하셨나요? 🏃")
            .setContentText("$dayLabel 러닝 목표를 달성해보세요!")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val EXTRA_DAY_LABEL = "day_label"
        const val EXTRA_DAY_OF_WEEK = "day_of_week"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
    }
}
