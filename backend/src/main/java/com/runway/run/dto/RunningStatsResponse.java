package com.runway.run.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class RunningStatsResponse {

    private String period;
    private long totalRuns;
    private double totalDistanceMeters;
    private long totalDurationSeconds;
    private long totalCaloriesBurned;
    private int averagePaceSecondsPerKm;
    private double longestRunMeters;
    private int currentStreakDays;
    private int longestStreakDays;
    private long activeDays;
    private Instant periodStart;
    private Instant periodEnd;
}
