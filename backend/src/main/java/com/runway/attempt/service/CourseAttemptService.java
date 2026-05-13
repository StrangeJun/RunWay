package com.runway.attempt.service;

import com.runway.attempt.domain.CourseAttempt;
import com.runway.attempt.dto.*;
import com.runway.attempt.repository.CourseAttemptRepository;
import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.common.response.PageResponse;
import com.runway.course.domain.Course;
import com.runway.course.domain.enums.CourseStatus;
import com.runway.course.repository.CourseRepository;
import com.runway.run.domain.RunningPoint;
import com.runway.run.domain.RunningRecord;
import com.runway.run.repository.RunningPointRepository;
import com.runway.run.repository.RunningRecordRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseAttemptService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final CourseAttemptRepository courseAttemptRepository;
    private final CourseRepository courseRepository;
    private final RunningRecordRepository runningRecordRepository;
    private final RunningPointRepository runningPointRepository;
    private final EntityManager entityManager;

    @Transactional
    public StartAttemptResponse startAttempt(UUID userId, UUID courseId,
                                             StartAttemptRequest request) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new RunwayException(ErrorCode.INVALID_COURSE_STATUS);
        }

        Instant startedAt = (request != null && request.getStartedAt() != null)
                ? request.getStartedAt() : Instant.now();

        // 1. running_records 먼저 생성
        RunningRecord runningRecord = RunningRecord.builder()
                .userId(userId)
                .startedAt(startedAt)
                .build();
        runningRecordRepository.save(runningRecord);

        // 2. course_attempts 생성 (running_record_id 연결)
        CourseAttempt attempt = CourseAttempt.builder()
                .courseId(courseId)
                .userId(userId)
                .runningRecordId(runningRecord.getId())
                .startedAt(startedAt)
                .build();
        courseAttemptRepository.save(attempt);

        // 3. courses.attempt_count 증가 (동일 트랜잭션)
        course.incrementAttemptCount();

        log.info("Attempt started: attemptId={} courseId={} runId={}",
                attempt.getId(), courseId, runningRecord.getId());
        return StartAttemptResponse.from(attempt);
    }

    @Transactional
    public FinishAttemptResponse finishAttempt(UUID userId, UUID attemptId,
                                               FinishAttemptRequest request) {
        CourseAttempt attempt = courseAttemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_ATTEMPT_NOT_FOUND));

        if (attempt.getStatus() != com.runway.attempt.domain.enums.CourseAttemptStatus.IN_PROGRESS) {
            throw new RunwayException(ErrorCode.INVALID_ATTEMPT_STATUS);
        }

        Instant endedAt = request.getEndedAt() != null ? request.getEndedAt() : Instant.now();

        // running_record 완료 처리
        if (attempt.getRunningRecordId() != null) {
            RunningRecord record = runningRecordRepository.findById(attempt.getRunningRecordId())
                    .orElseThrow(() -> new RunwayException(ErrorCode.RUN_NOT_FOUND));

            // running_points 로 LineString 경로 생성
            List<RunningPoint> points = runningPointRepository
                    .findByRunningRecordIdOrderBySequenceAsc(attempt.getRunningRecordId());
            if (points.size() >= 2) {
                Coordinate[] coords = points.stream()
                        .map(p -> p.getLocation().getCoordinate())
                        .toArray(Coordinate[]::new);
                LineString path = GEOMETRY_FACTORY.createLineString(coords);
                record.updatePath(path);
            }

            record.finish(endedAt, request.getDistanceMeters(), request.getDurationSeconds(),
                    request.getAvgPaceSecondsPerKm(), request.getCaloriesBurned(),
                    request.getAvgHeartRateBpm());
        }

        // course_attempt 완료 처리 (Phase 1: verification_status = verified 자동 설정)
        attempt.complete(endedAt, request.getDurationSeconds(), request.getDistanceMeters());

        // courses.completion_count 증가 (동일 트랜잭션)
        Course course = courseRepository.findByIdAndDeletedAtIsNull(attempt.getCourseId())
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_NOT_FOUND));
        course.incrementCompletionCount();

        log.info("Attempt finished: attemptId={} courseId={} durationSeconds={}",
                attemptId, attempt.getCourseId(), request.getDurationSeconds());
        return FinishAttemptResponse.from(attempt);
    }

    @Transactional
    public AbandonAttemptResponse abandonAttempt(UUID userId, UUID attemptId,
                                                  AbandonAttemptRequest request) {
        CourseAttempt attempt = courseAttemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_ATTEMPT_NOT_FOUND));

        if (attempt.getStatus() != com.runway.attempt.domain.enums.CourseAttemptStatus.IN_PROGRESS) {
            throw new RunwayException(ErrorCode.INVALID_ATTEMPT_STATUS);
        }

        Instant endedAt = (request != null && request.getEndedAt() != null)
                ? request.getEndedAt() : Instant.now();

        // running_record 중단 처리
        if (attempt.getRunningRecordId() != null) {
            RunningRecord record = runningRecordRepository.findById(attempt.getRunningRecordId())
                    .orElseThrow(() -> new RunwayException(ErrorCode.RUN_NOT_FOUND));
            record.abandon(endedAt);
        }

        // course_attempt 중단 처리 (verification_status 는 PENDING 유지)
        attempt.abandon();

        log.info("Attempt abandoned: attemptId={} courseId={}", attemptId, attempt.getCourseId());
        return AbandonAttemptResponse.from(attempt, endedAt);
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(UUID userId, UUID courseId, int page, int size) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.isVisibleTo(userId)) {
            throw new RunwayException(ErrorCode.FORBIDDEN);
        }

        // CTE + RANK() OVER 로 유저별 최단 완주 시간 기준 랭킹 계산
        String dataSql = """
                WITH ranked AS (
                    SELECT
                        u.id               AS user_id,
                        u.nickname         AS nickname,
                        MIN(ca.duration_seconds) AS best_time_seconds,
                        COUNT(*)           AS completion_count,
                        RANK() OVER (ORDER BY MIN(ca.duration_seconds) ASC) AS rank
                    FROM course_attempts ca
                    JOIN users u ON ca.user_id = u.id
                    WHERE ca.course_id = ?
                      AND ca.status = 'completed'
                      AND ca.verification_status = 'verified'
                    GROUP BY ca.user_id, u.id, u.nickname
                )
                SELECT * FROM ranked
                ORDER BY rank ASC
                LIMIT ? OFFSET ?
                """;

        String countSql = """
                SELECT COUNT(DISTINCT ca.user_id)
                FROM course_attempts ca
                WHERE ca.course_id = ?
                  AND ca.status = 'completed'
                  AND ca.verification_status = 'verified'
                """;

        List<Object> dataParams = new ArrayList<>();
        dataParams.add(courseId);
        dataParams.add(size);
        dataParams.add((long) page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = buildQuery(dataSql, dataParams).getResultList();
        List<LeaderboardItem> items = rows.stream().map(this::toLeaderboardItem).toList();

        long total = ((Number) buildQuery(countSql, List.of(courseId)).getSingleResult()).longValue();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);

        return LeaderboardResponse.builder()
                .courseId(courseId)
                .items(items)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseAttemptResponse> getMyAttempts(UUID userId, UUID courseId,
                                                              int page, int size) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.isVisibleTo(userId)) {
            throw new RunwayException(ErrorCode.FORBIDDEN);
        }

        return PageResponse.from(
                courseAttemptRepository
                        .findByCourseIdAndUserIdOrderByStartedAtDesc(
                                courseId, userId, PageRequest.of(page, size))
                        .map(CourseAttemptResponse::from));
    }

    // --- private helpers ---

    private jakarta.persistence.Query buildQuery(String sql, List<Object> params) {
        jakarta.persistence.Query query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }
        return query;
    }

    private LeaderboardItem toLeaderboardItem(Object[] row) {
        // row[0]=user_id, row[1]=nickname, row[2]=best_time_seconds,
        // row[3]=completion_count, row[4]=rank
        return LeaderboardItem.builder()
                .userId(UUID.fromString(row[0].toString()))
                .nickname((String) row[1])
                .bestTimeSeconds(((Number) row[2]).intValue())
                .completionCount(((Number) row[3]).longValue())
                .rank(((Number) row[4]).longValue())
                .build();
    }
}
