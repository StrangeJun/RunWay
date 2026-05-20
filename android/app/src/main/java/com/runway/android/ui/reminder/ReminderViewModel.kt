package com.runway.android.ui.reminder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.notification.ReminderScheduler
import com.runway.android.data.reminder.ReminderPrefs
import com.runway.android.data.reminder.ReminderPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderPrefs: ReminderPrefsRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    var enabled by mutableStateOf(false)
        private set
    var hour by mutableStateOf(7)
        private set
    var minute by mutableStateOf(0)
        private set
    var enabledDays by mutableStateOf(setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY))
        private set

    init {
        viewModelScope.launch {
            reminderPrefs.getPrefs().collect { prefs ->
                enabled = prefs.enabled
                hour = prefs.hour
                minute = prefs.minute
                enabledDays = prefs.enabledDays
            }
        }
    }

    fun toggleEnabled(value: Boolean) {
        enabled = value
        save()
    }

    fun setTime(h: Int, m: Int) {
        hour = h
        minute = m
        save()
    }

    fun toggleDay(dayOfWeek: Int) {
        enabledDays = if (dayOfWeek in enabledDays) {
            enabledDays - dayOfWeek
        } else {
            enabledDays + dayOfWeek
        }
        save()
    }

    private fun save() {
        val prefs = ReminderPrefs(
            enabled = enabled,
            hour = hour,
            minute = minute,
            enabledDays = enabledDays,
        )
        viewModelScope.launch {
            reminderPrefs.save(prefs)
            scheduler.scheduleAll(prefs)
        }
    }
}
