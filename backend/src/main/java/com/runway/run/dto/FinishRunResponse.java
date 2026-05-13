package com.runway.run.dto;

import com.runway.run.domain.RunningRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class FinishRunResponse {

    private UUID runId;
    private String status;
    private Instant startedAt;
    private Instant endedAt;
    private Double distanceMeters;
    private Integer durationSeconds;
    private Integer avgPaceSecondsPerKm;
    private Integer caloriesBurned;
    private boolean pathCreated;

    public static FinishRunResponse from(RunningRecord record, boolean pathCreated) {
        return FinishRunResponse.builder()
                .runId(record.getId())
                .status(record.getStatus().getDbValue())
                .startedAt(record.getStartedAt())
                .endedAt(record.getEndedAt())
                .distanceMeters(record.getDistanceMeters())
                .durationSeconds(record.getDurationSeconds())
                .avgPaceSecondsPerKm(record.getAvgPaceSecondsPerKm())
                .caloriesBurned(record.getCaloriesBurned())
                .pathCreated(pathCreated)
                .build();
    }
}
