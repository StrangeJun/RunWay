package com.runway.run.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

import java.time.Instant;

@Getter
public class FinishRunRequest {

    @NotNull(message = "endedAt은 필수입니다.")
    private Instant endedAt;

    @NotNull(message = "distanceMeters는 필수입니다.")
    @PositiveOrZero(message = "distanceMeters는 0 이상이어야 합니다.")
    private Double distanceMeters;

    @NotNull(message = "durationSeconds는 필수입니다.")
    @PositiveOrZero(message = "durationSeconds는 0 이상이어야 합니다.")
    private Integer durationSeconds;

    private Integer avgPaceSecondsPerKm;

    private Integer caloriesBurned;

    private Integer avgHeartRateBpm;
}
