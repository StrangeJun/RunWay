package com.runway.run.dto;

import com.runway.run.domain.RunningPoint;
import com.runway.run.domain.RunningRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class RunDetailResponse {

    private UUID runId;
    private String status;
    private Instant startedAt;
    private Instant endedAt;
    private Double distanceMeters;
    private Integer durationSeconds;
    private Integer avgPaceSecondsPerKm;
    private Integer caloriesBurned;
    private List<RunPointResponse> points;

    public static RunDetailResponse from(RunningRecord record, List<RunningPoint> points) {
        return RunDetailResponse.builder()
                .runId(record.getId())
                .status(record.getStatus().getDbValue())
                .startedAt(record.getStartedAt())
                .endedAt(record.getEndedAt())
                .distanceMeters(record.getDistanceMeters())
                .durationSeconds(record.getDurationSeconds())
                .avgPaceSecondsPerKm(record.getAvgPaceSecondsPerKm())
                .caloriesBurned(record.getCaloriesBurned())
                .points(points.stream().map(RunPointResponse::from).toList())
                .build();
    }
}
