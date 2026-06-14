package com.runway.android.ui.reminder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.notification.ReminderScheduler
import com.runway.android.data.reminder.ReminderPrefs
import com.runway.android.data.reminder.ReminderPrefsRepository
import com.runway.android.data.reminder.DayRunningPlan
import com.runway.android.data.reminder.ReminderWorkoutType
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
    var plans by mutableStateOf<Map<Int, DayRunningPlan>>(emptyMap())
        private set

    init {
        viewModelScope.launch {
            reminderPrefs.getPrefs().collect { prefs ->
                enabled = prefs.enabled
                hour = prefs.hour
                minute = prefs.minute
                enabledDays = prefs.enabledDays
                plans = prefs.plans
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

    fun planFor(dayOfWeek: Int): DayRunningPlan =
        plans[dayOfWeek] ?: DayRunningPlan(dayOfWeek = dayOfWeek)

    fun setWorkoutType(dayOfWeek: Int, type: ReminderWorkoutType) {
        updatePlan(dayOfWeek) { it.copy(workoutType = type) }
    }

    fun setDistance(dayOfWeek: Int, value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' }
            .let { text ->
                val firstDot = text.indexOf('.')
                if (firstDot < 0) text else {
                    text.substring(0, firstDot + 1) +
                        text.substring(firstDot + 1).replace(".", "")
                }
            }
            .take(6)
        updatePlan(dayOfWeek) { it.copy(distanceKm = sanitized) }
    }

    fun setPace(dayOfWeek: Int, value: String) {
        val sanitized = value.filter { it.isDigit() || it == ':' }.take(5)
        updatePlan(dayOfWeek) { it.copy(targetPace = sanitized) }
    }

    fun setDuration(dayOfWeek: Int, value: String) {
        updatePlan(dayOfWeek) { it.copy(durationMinutes = value.filter(Char::isDigit).take(4)) }
    }

    fun setNote(dayOfWeek: Int, value: String) {
        updatePlan(dayOfWeek) { it.copy(note = value.take(80)) }
    }

    private fun updatePlan(dayOfWeek: Int, transform: (DayRunningPlan) -> DayRunningPlan) {
        plans = plans + (dayOfWeek to transform(planFor(dayOfWeek)))
        save()
    }

    private fun save() {
        val prefs = ReminderPrefs(
            enabled = enabled,
            hour = hour,
            minute = minute,
            enabledDays = enabledDays,
            plans = plans,
        )
        viewModelScope.launch {
            reminderPrefs.save(prefs)
            scheduler.scheduleAll(prefs)
        }
    }
}
