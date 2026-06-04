package com.runway.attempt.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MyBestAttemptResponse {

    /** 완주 횟수 (completed 기준) */
    private long completionCount;
    /** 최고 기록(초). 완주 기록 없으면 null */
    private Integer bestTimeSeconds;
    /** 가장 최근 완주 시각. 완주 기록 없으면 null */
    private Instant lastAttemptAt;
}
