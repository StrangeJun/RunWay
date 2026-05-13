package com.runway.attempt.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.time.Instant;

@Getter
public class FinishAttemptRequest {

    @NotNull(message = "종료 시각은 필수입니다.")
    private Instant endedAt;

    @NotNull(message = "이동 거리는 필수입니다.")
    @Positive(message = "이동 거리는 양수여야 합니다.")
    private Double distanceMeters;

    @NotNull(message = "소요 시간은 필수입니다.")
    @Positive(message = "소요 시간은 양수여야 합니다.")
    private Integer durationSeconds;

    private Integer avgPaceSecondsPerKm;
    private Integer caloriesBurned;
    private Integer avgHeartRateBpm;
}
