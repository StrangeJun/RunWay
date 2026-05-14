package com.runway.android.ui.home

import androidx.lifecycle.ViewModel
import com.runway.android.ui.components.NearbyCourse
import com.runway.android.ui.components.RecentRun
import com.runway.android.ui.components.WeeklyStats
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    val greeting: String = buildGreeting()

    val weeklyStats = WeeklyStats(
        distanceKm = "24.6",
        runs = "4",
        avgPace = "5'12\"",
        calories = "1,820",
    )

    val nearbyCourses = listOf(
        NearbyCourse(
            name = "Riverside Loop",
            distanceAway = "0.4 km",
            lengthKm = "5.2 km",
            routeVariant = 0,
        ),
        NearbyCourse(
            name = "Hillcrest Sprint",
            distanceAway = "1.1 km",
            lengthKm = "3.0 km",
            routeVariant = 1,
        ),
        NearbyCourse(
            name = "Old Bridge Circuit",
            distanceAway = "2.3 km",
            lengthKm = "7.8 km",
            routeVariant = 2,
        ),
    )

    val recentRuns = listOf(
        RecentRun(day = "Today", distanceKm = "5.20", pace = "5'08\"", duration = "26:43"),
        RecentRun(day = "Tue", distanceKm = "8.10", pace = "5'22\"", duration = "43:30"),
        RecentRun(day = "Mon", distanceKm = "6.50", pace = "5'14\"", duration = "34:01"),
    )

    private fun buildGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning,"
            in 12..17 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }
}
