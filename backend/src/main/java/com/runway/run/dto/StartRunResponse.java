package com.runway.run.dto;

import com.runway.run.domain.RunningRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class StartRunResponse {

    private UUID runId;
    private String status;
    private Instant startedAt;

    public static StartRunResponse from(RunningRecord record) {
        return StartRunResponse.builder()
                .runId(record.getId())
                .status(record.getStatus().getDbValue())
                .startedAt(record.getStartedAt())
                .build();
    }
}
