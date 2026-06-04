package com.runway.run.repository;

public interface RunningStatsProjection {
    long getTotalRuns();
    double getTotalDistanceMeters();
    long getTotalDurationSeconds();
    long getTotalCaloriesBurned();
    double getLongestRunMeters();
    long getActiveDays();
}
