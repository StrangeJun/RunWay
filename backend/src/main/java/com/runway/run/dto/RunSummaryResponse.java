package com.runway.run.dto;

import com.runway.run.domain.RunningRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RunSummaryResponse {

    private UUID runId;
    private String status;
    private Instant startedAt;
    private Instant endedAt;
    private Double distanceMeters;
    private Integer durationSeconds;
    private Integer avgPaceSecondsPerKm;

    public static RunSummaryResponse from(RunningRecord record) {
        return RunSummaryResponse.builder()
                .runId(record.getId())
                .status(record.getStatus().getDbValue())
                .startedAt(record.getStartedAt())
                .endedAt(record.getEndedAt())
                .distanceMeters(record.getDistanceMeters())
                .durationSeconds(record.getDurationSeconds())
                .avgPaceSecondsPerKm(record.getAvgPaceSecondsPerKm())
                .build();
    }
}
