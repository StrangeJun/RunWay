package com.runway.android.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.runway.android.data.reminder.ReminderPrefs
import com.runway.android.data.reminder.ReminderPlanFormatter
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
            schedule(
                dayOfWeek = dayOfWeek,
                hour = prefs.hour,
                minute = prefs.minute,
                planSummary = ReminderPlanFormatter.summary(prefs.plans[dayOfWeek]),
            )
        }
    }

    fun cancelAll() {
        for (day in 1..7) {
            alarmManager.cancel(buildIntent(day, 0, 0))
        }
    }

    fun schedule(
        dayOfWeek: Int,
        hour: Int,
        minute: Int,
        planSummary: String = "",
    ) {
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

        val pendingIntent = buildIntent(dayOfWeek, hour, minute, planSummary)

        // Android 12+(API 31): 정확한 알람 권한이 없으면 비정확 알람으로 폴백
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent,
            )
            return
        }

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

    // ReminderReceiver에서 다음 주 재예약에 사용하기 위해 internal로 공개
    internal fun buildIntent(
        dayOfWeek: Int,
        hour: Int,
        minute: Int,
        planSummary: String = "",
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_DAY_LABEL, dayLabel(dayOfWeek))
            putExtra(ReminderReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
            putExtra(ReminderReceiver.EXTRA_HOUR, hour)
            putExtra(ReminderReceiver.EXTRA_MINUTE, minute)
            putExtra(ReminderReceiver.EXTRA_PLAN_SUMMARY, planSummary)
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
