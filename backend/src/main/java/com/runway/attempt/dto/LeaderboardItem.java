package com.runway.attempt.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class LeaderboardItem {

    private long rank;
    private UUID userId;
    private String nickname;
    private Integer bestTimeSeconds;
    private long completionCount;
}
