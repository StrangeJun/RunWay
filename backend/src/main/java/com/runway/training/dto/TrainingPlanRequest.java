package com.runway.training.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class TrainingPlanRequest {

    @NotNull
    @DecimalMin("1.0")
    @DecimalMax("100.0")
    private Double goalDistanceKm;

    @Min(10)
    @Max(1440)
    private Integer goalTimeMinutes;

    @Pattern(regexp = "^$|^[0-9]{1,2}:[0-5][0-9]$")
    private String targetPace;

    @DecimalMin("0.0")
    @DecimalMax("300.0")
    private Double currentWeeklyKm;

    @NotNull
    @Min(1)
    @Max(7)
    private Integer trainingDays;

    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED")
    private String experienceLevel = "BEGINNER";
}
