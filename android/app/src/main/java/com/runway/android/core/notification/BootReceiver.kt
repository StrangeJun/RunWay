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
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = reminderPrefs.getPrefs().first()
            if (prefs.enabled) scheduler.scheduleAll(prefs)
        }
    }
}
