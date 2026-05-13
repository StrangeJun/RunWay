package com.runway.run.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class StartRunRequest {

    // null 허용 — null이면 서버가 Instant.now() 사용
    private Instant startedAt;
}
