package com.runway.run.repository;

import com.runway.run.domain.RunningRecord;
import com.runway.run.domain.enums.RunningRecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RunningRecordRepository extends JpaRepository<RunningRecord, UUID> {

    Optional<RunningRecord> findByIdAndUserId(UUID id, UUID userId);

    Page<RunningRecord> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    List<RunningRecord> findTop20ByUserIdAndStatusOrderByStartedAtDesc(
            UUID userId, RunningRecordStatus status);

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

    // ─── Stats 쿼리 (period 기반) ───

    @Query("SELECT r FROM RunningRecord r WHERE r.userId = :userId AND r.status = :status " +
           "AND r.startedAt >= :from AND r.startedAt < :to")
    List<RunningRecord> findByUserIdAndStatusAndPeriod(
            @Param("userId") UUID userId,
            @Param("status") RunningRecordStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);

    // streak 계산용: 완료된 런 전체를 날짜 오름차순으로 조회
    @Query("SELECT r FROM RunningRecord r WHERE r.userId = :userId AND r.status = :status " +
           "ORDER BY r.startedAt ASC")
    List<RunningRecord> findAllByUserIdAndStatusOrderByStartedAt(
            @Param("userId") UUID userId,
            @Param("status") RunningRecordStatus status);

    // ─── Aggregate projection 쿼리 ───

    @Query(value = """
        SELECT
            COUNT(*)                                               AS totalRuns,
            COALESCE(SUM(distance_meters), 0)                    AS totalDistanceMeters,
            COALESCE(SUM(duration_seconds), 0)                   AS totalDurationSeconds,
            COALESCE(SUM(calories_burned), 0)                    AS totalCaloriesBurned,
            COALESCE(MAX(distance_meters), 0)                    AS longestRunMeters,
            COUNT(DISTINCT DATE(started_at AT TIME ZONE 'UTC'))  AS activeDays
        FROM running_records
        WHERE user_id = :userId
          AND status = :status
          AND started_at >= :from
          AND started_at < :to
        """, nativeQuery = true)
    RunningStatsProjection aggregateStatsByPeriod(
            @Param("userId") UUID userId,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(value = """
        SELECT DISTINCT DATE(started_at AT TIME ZONE 'UTC') AS runDate
        FROM running_records
        WHERE user_id = :userId AND status = :status
        ORDER BY runDate ASC
        """, nativeQuery = true)
    List<RunDateProjection> findDistinctRunDatesByUserId(
            @Param("userId") UUID userId,
            @Param("status") String status);

    @Query("SELECT r.distanceMeters AS distanceMeters, r.startedAt AS startedAt " +
           "FROM RunningRecord r WHERE r.userId = :userId AND r.status = :status " +
           "ORDER BY r.startedAt ASC")
    List<RunSummaryProjection> findRunSummariesByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") RunningRecordStatus status);
}
