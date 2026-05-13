package com.runway.run.domain;

import com.runway.run.domain.enums.RunningRecordStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "running_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    // RunningRecordStatus.JpaConverter 가 autoApply=true 이므로 @Convert 불필요
    @Column(nullable = false, length = 30)
    private RunningRecordStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "distance_meters", nullable = false)
    private Double distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "avg_pace_seconds_per_km")
    private Integer avgPaceSecondsPerKm;

    @Column(name = "avg_heart_rate_bpm")
    private Integer avgHeartRateBpm;

    @Column(name = "calories_burned")
    private Integer caloriesBurned;

    @Column(columnDefinition = "geography(LineString,4326)")
    private LineString path;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private RunningRecord(UUID userId, Instant startedAt) {
        this.userId = userId;
        this.startedAt = startedAt;
        this.status = RunningRecordStatus.IN_PROGRESS;
        this.distanceMeters = 0.0;
        this.durationSeconds = 0;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }

    public void pause() {
        this.status = RunningRecordStatus.PAUSED;
    }

    public void resume() {
        this.status = RunningRecordStatus.IN_PROGRESS;
    }

    public void finish(Instant endedAt, Double distanceMeters, Integer durationSeconds,
                       Integer avgPaceSecondsPerKm, Integer caloriesBurned, Integer avgHeartRateBpm) {
        this.status = RunningRecordStatus.COMPLETED;
        this.endedAt = endedAt;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.avgPaceSecondsPerKm = avgPaceSecondsPerKm;
        this.caloriesBurned = caloriesBurned;
        this.avgHeartRateBpm = avgHeartRateBpm;
    }

    public void abandon() {
        this.status = RunningRecordStatus.ABANDONED;
        this.endedAt = Instant.now();
    }

    public void updatePath(LineString path) {
        this.path = path;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.userId.equals(userId);
    }
}
