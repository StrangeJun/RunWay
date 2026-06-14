package com.runway.android.domain.training

import java.time.LocalDate

enum class TrainingGoalType {
    DISTANCE,
    RACE_TIME,
    FITNESS,
}

data class TrainingGoal(
    val goalType: TrainingGoalType = TrainingGoalType.DISTANCE,
    val goalDistanceKm: Double? = 10.0,
    val goalDate: LocalDate? = LocalDate.now().plusWeeks(8),
    val goalTimeSeconds: Int? = null,
    val preferredRunsPerWeek: Int = 3,
    val availableDays: Set<Int> = setOf(2, 4, 7),
)

data class TrainingRecommendationRequest(
    val goalType: String,
    val goalDistanceKm: Double?,
    val goalDate: String?,
    val goalTimeSeconds: Int?,
    val preferredRunsPerWeek: Int,
    val availableDays: List<Int>,
) {
    companion object {
        fun from(goal: TrainingGoal) = TrainingRecommendationRequest(
            goalType = goal.goalType.name,
            goalDistanceKm = goal.goalDistanceKm,
            goalDate = goal.goalDate?.toString(),
            goalTimeSeconds = goal.goalTimeSeconds,
            preferredRunsPerWeek = goal.preferredRunsPerWeek,
            availableDays = goal.availableDays.sorted(),
        )
    }
}
