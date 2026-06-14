package com.runway.android.data.training.model

data class TrainingPlanRequest(
    val goalDistanceKm: Double,
    val goalTimeMinutes: Int?,
    val targetPace: String,
    val currentWeeklyKm: Double?,
    val trainingDays: Int,
    val experienceLevel: String,
)

data class TrainingPlanResponse(
    val title: String,
    val summary: String,
    val days: List<TrainingDay>,
    val source: String,
)

data class TrainingDay(
    val dayOfWeek: Int,
    val workoutType: String,
    val title: String,
    val distanceKm: Double?,
    val targetPace: String,
    val durationMinutes: Int?,
    val description: String,
    val restDay: Boolean,
)
