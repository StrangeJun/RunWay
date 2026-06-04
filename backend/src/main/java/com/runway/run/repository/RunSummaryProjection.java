package com.runway.run.repository;

import java.time.Instant;

public interface RunSummaryProjection {
    Double getDistanceMeters();
    Instant getStartedAt();
}
