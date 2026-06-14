package com.runway.training.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record TrainingRecommendationRequest(
        @NotNull GoalType goalType,
        @DecimalMin("1.0") @DecimalMax("100.0") Double goalDistanceKm,
        @FutureOrPresent LocalDate goalDate,
        @Min(600) @Max(86400) Integer goalTimeSeconds,
        @NotNull @Min(1) @Max(7) Integer preferredRunsPerWeek,
        List<Integer> availableDays
) {
    public enum GoalType {
        DISTANCE, RACE_TIME, FITNESS
    }
}
