package com.runway.android.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.runway.android.data.reminder.ReminderPrefsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderPrefs: ReminderPrefsRepository

    @Inject
    lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // goAsync()로 브로드캐스트 윈도우를 연장해 코루틴 완료까지 보장
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = reminderPrefs.getPrefs().first()
                if (prefs.enabled) scheduler.scheduleAll(prefs)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
