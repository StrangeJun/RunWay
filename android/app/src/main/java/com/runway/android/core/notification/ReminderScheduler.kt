package com.runway.android.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.runway.android.data.reminder.ReminderPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAll(prefs: ReminderPrefs) {
        cancelAll()
        if (!prefs.enabled) return
        prefs.enabledDays.forEach { dayOfWeek ->
            schedule(dayOfWeek, prefs.hour, prefs.minute)
        }
    }

    fun cancelAll() {
        for (day in 1..7) {
            val intent = buildIntent(day)
            alarmManager.cancel(intent)
        }
    }

    private fun schedule(dayOfWeek: Int, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val pendingIntent = buildIntent(dayOfWeek)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent,
            )
        }
    }

    private fun buildIntent(dayOfWeek: Int): PendingIntent {
        val dayLabel = dayLabel(dayOfWeek)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("day_label", dayLabel)
        }
        return PendingIntent.getBroadcast(
            context,
            dayOfWeek,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun dayLabel(dayOfWeek: Int) = when (dayOfWeek) {
        Calendar.SUNDAY -> "일요일"
        Calendar.MONDAY -> "월요일"
        Calendar.TUESDAY -> "화요일"
        Calendar.WEDNESDAY -> "수요일"
        Calendar.THURSDAY -> "목요일"
        Calendar.FRIDAY -> "금요일"
        Calendar.SATURDAY -> "토요일"
        else -> "오늘"
    }
}
