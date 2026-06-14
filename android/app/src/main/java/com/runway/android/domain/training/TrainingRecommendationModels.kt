package com.runway.android.domain.training

const val MIN_RUN_COUNT_FOR_PERSONALIZED_PLAN = 3
const val MIN_TOTAL_DISTANCE_METERS_FOR_PERSONALIZED_PLAN = 15_000
const val MIN_LOOKBACK_DAYS = 14
const val MIN_SINGLE_RUN_DISTANCE_METERS = 1_000
const val MIN_SINGLE_RUN_DURATION_SECONDS = 600

const val MIN_RUN_COUNT_FOR_ADVANCED_PLAN = 8
const val MIN_TOTAL_DISTANCE_METERS_FOR_ADVANCED_PLAN = 25_000
const val MIN_LOOKBACK_DAYS_FOR_ADVANCED_PLAN = 28

enum class TrainingRecommendationStatus {
    NO_DATA,
    INSUFFICIENT_DATA,
    PERSONALIZED_READY,
}

data class TrainingReadiness(
    val validRunCount: Int,
    val requiredRunCount: Int = MIN_RUN_COUNT_FOR_PERSONALIZED_PLAN,
    val totalDistanceMeters: Double,
    val requiredDistanceMeters: Double = MIN_TOTAL_DISTANCE_METERS_FOR_PERSONALIZED_PLAN.toDouble(),
    val lookbackDays: Int = MIN_LOOKBACK_DAYS,
    val isReady: Boolean,
)

data class TrainingMetrics(
    val averagePaceSecondsPerKm: Int?,
    val averageDistanceMeters: Double,
    val weeklyDistanceMeters: Double,
    val runsPerWeek: Double,
)

enum class TrainingSessionType {
    EASY,
    RECOVERY,
    LONG,
    TEMPO,
    INTERVAL,
}

data class TrainingSession(
    val type: TrainingSessionType,
    val title: String,
    val dayOfWeek: Int = 1,
    val targetDurationMinutes: Int? = null,
    val targetDistanceMeters: Double? = null,
    val targetPaceSecondsPerKm: Int? = null,
    val guidanceText: String,
)

data class TrainingPlan(
    val title: String,
    val description: String,
    val weeklyRuns: Int,
    val totalWeeklyDistanceMeters: Double,
    val sessions: List<TrainingSession>,
    val goalAssessment: String = "INSUFFICIENT_DATA",
    val caution: String = "",
)

data class TrainingRecommendation(
    val source: String = "GEMINI",
    val status: TrainingRecommendationStatus,
    val readiness: TrainingReadiness,
    val plan: TrainingPlan,
    val metrics: TrainingMetrics? = null,
)
