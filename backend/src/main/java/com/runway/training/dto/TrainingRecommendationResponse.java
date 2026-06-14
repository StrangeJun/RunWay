package com.runway.training.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TrainingRecommendationResponse(
        String source,
        String status,
        Readiness readiness,
        Metrics metrics,
        Plan plan
) {
    public record Readiness(
            int validRunCount,
            int requiredRunCount,
            double totalDistanceMeters,
            double requiredDistanceMeters,
            int lookbackDays,
            @JsonProperty("isReady") boolean isReady
    ) {}

    public record Metrics(
            Integer averagePaceSecondsPerKm,
            double averageDistanceMeters,
            double weeklyDistanceMeters,
            double runsPerWeek
    ) {}

    public record Plan(
            String title,
            String description,
            String goalAssessment,
            int weeklyRuns,
            double totalWeeklyDistanceMeters,
            List<Session> sessions,
            String caution
    ) {}

    public record Session(
            int dayOfWeek,
            String type,
            String title,
            Integer targetDurationMinutes,
            Double targetDistanceMeters,
            Integer targetPaceSecondsPerKm,
            String guidanceText
    ) {}
}
