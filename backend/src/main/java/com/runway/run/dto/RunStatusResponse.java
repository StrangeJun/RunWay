package com.runway.run.dto;

import com.runway.run.domain.RunningRecord;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RunStatusResponse {

    private UUID runId;
    private String status;

    public static RunStatusResponse from(RunningRecord record) {
        return RunStatusResponse.builder()
                .runId(record.getId())
                .status(record.getStatus().getDbValue())
                .build();
    }
}
