package com.runway.attempt.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class StartAttemptRequest {
    private Instant startedAt;
}
