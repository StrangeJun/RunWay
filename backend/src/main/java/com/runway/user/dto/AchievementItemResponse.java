package com.runway.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AchievementItemResponse {

    private String code;
    private String title;
    private String description;
    private boolean unlocked;
    private Instant unlockedAt;
    private long progress;
    private long target;
}
