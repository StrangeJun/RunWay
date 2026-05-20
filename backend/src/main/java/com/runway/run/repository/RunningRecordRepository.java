package com.runway.run.repository;

import com.runway.run.domain.RunningRecord;
import com.runway.run.domain.enums.RunningRecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RunningRecordRepository extends JpaRepository<RunningRecord, UUID> {

    Optional<RunningRecord> findByIdAndUserId(UUID id, UUID userId);

    Page<RunningRecord> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    // ─── Personal Records 쿼리 ───

    Optional<RunningRecord> findTop1ByUserIdAndStatusOrderByDistanceMetersDesc(
            UUID userId, RunningRecordStatus status);

    @Query("SELECT r FROM RunningRecord r WHERE r.userId = ?1 AND r.status = ?2 " +
           "AND r.distanceMeters >= ?3 AND r.avgPaceSecondsPerKm IS NOT NULL " +
           "ORDER BY r.avgPaceSecondsPerKm ASC LIMIT 1")
    Optional<RunningRecord> findFastestPace(UUID userId, RunningRecordStatus status, double minDistance);

    Optional<RunningRecord> findTop1ByUserIdAndStatusAndCaloriesBurnedNotNullOrderByCaloriesBurnedDesc(
            UUID userId, RunningRecordStatus status);

    @Query("SELECT r FROM RunningRecord r WHERE r.userId = ?1 AND r.status = ?2 " +
           "AND r.distanceMeters >= ?3 ORDER BY r.durationSeconds ASC LIMIT 1")
    Optional<RunningRecord> findBestTimeForDistance(UUID userId, RunningRecordStatus status, double minDistance);

    long countByUserIdAndStatus(UUID userId, RunningRecordStatus status);

    @Query("SELECT COALESCE(SUM(r.distanceMeters), 0.0) FROM RunningRecord r " +
           "WHERE r.userId = ?1 AND r.status = ?2")
    double sumDistanceMetersByUserIdAndStatus(UUID userId, RunningRecordStatus status);
}
