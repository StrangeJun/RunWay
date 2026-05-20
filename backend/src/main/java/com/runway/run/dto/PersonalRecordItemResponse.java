package com.runway.run.dto;

import com.runway.run.domain.RunningRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PersonalRecordItemResponse {

    private UUID runId;
    private Double distanceMeters;
    private Integer durationSeconds;
    private Integer avgPaceSecondsPerKm;
    private Integer caloriesBurned;
    private Instant recordedAt;

    public static PersonalRecordItemResponse from(RunningRecord r) {
        return PersonalRecordItemResponse.builder()
                .runId(r.getId())
                .distanceMeters(r.getDistanceMeters())
                .durationSeconds(r.getDurationSeconds())
                .avgPaceSecondsPerKm(r.getAvgPaceSecondsPerKm())
                .caloriesBurned(r.getCaloriesBurned())
                .recordedAt(r.getStartedAt())
                .build();
    }
}
