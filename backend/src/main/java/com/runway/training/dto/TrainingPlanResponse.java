package com.runway.training.dto;

import java.util.List;

public record TrainingPlanResponse(
        String title,
        String summary,
        List<TrainingDay> days,
        String source
) {
    public record TrainingDay(
            int dayOfWeek,
            String workoutType,
            String title,
            Double distanceKm,
            String targetPace,
            Integer durationMinutes,
            String description,
            boolean restDay
    ) {
    }
}
