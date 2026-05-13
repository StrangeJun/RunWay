package com.runway.run.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class SavePointsResponse {

    private final UUID runId;
    private final int savedCount;
}
