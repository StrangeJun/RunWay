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
import java.util.Optional;
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

        // 12 m/s (43.2 km/h) 초과는 인간 러닝으로 물리적으로 불가능한 속도
        if (request.getDurationSeconds() > 0
                && (double) request.getDistanceMeters() / request.getDurationSeconds() > 12.0) {
            throw new RunwayException(ErrorCode.IMPOSSIBLE_SPEED);
        }

        Instant endedAt = request.getEndedAt() != null ? request.getEndedAt() : Instant.now();

        if (!endedAt.isAfter(attempt.getStartedAt())) {
            throw new RunwayException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각 이후여야 합니다.");
        }

        // 코스 조회 (검증에도 필요하므로 먼저 fetch)
        Course course = courseRepository.findByIdAndDeletedAtIsNull(attempt.getCourseId())
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_NOT_FOUND));

        // running_record 완료 처리 + 경로 수집
        List<RunningPoint> points = List.of();
        if (attempt.getRunningRecordId() != null) {
            RunningRecord record = runningRecordRepository.findById(attempt.getRunningRecordId())
                    .orElseThrow(() -> new RunwayException(ErrorCode.RUN_NOT_FOUND));

            points = runningPointRepository
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

        // ─── 기록 무결성 검증 ───
        com.runway.attempt.domain.enums.AttemptVerificationStatus verificationStatus =
                com.runway.attempt.domain.enums.AttemptVerificationStatus.VERIFIED;

        // GPS 포인트 < 2개 → 경로 재구성 불가, 검증 보류
        if (points.size() < 2) {
            verificationStatus = com.runway.attempt.domain.enums.AttemptVerificationStatus.PENDING;
            log.info("Attempt marked unverified: insufficient GPS points. count={} attemptId={}",
                    points.size(), attemptId);
        }

        // 코스 거리 대비 70% 미만 → PENDING
        if (verificationStatus == com.runway.attempt.domain.enums.AttemptVerificationStatus.VERIFIED
                && course.getDistanceMeters() > 0
                && request.getDistanceMeters() < course.getDistanceMeters() * 0.7) {
            verificationStatus = com.runway.attempt.domain.enums.AttemptVerificationStatus.PENDING;
            log.info("Attempt marked unverified (distance coverage): attempted={}m course={}m attemptId={}",
                    request.getDistanceMeters(), course.getDistanceMeters(), attemptId);
        }

        // 시작/종료 지점 500m 이상 이탈 → PENDING
        if (verificationStatus == com.runway.attempt.domain.enums.AttemptVerificationStatus.VERIFIED
                && !points.isEmpty()) {
            RunningPoint firstPt = points.get(0);
            RunningPoint lastPt = points.get(points.size() - 1);
            double startDist = haversineMeters(
                    course.getStartLocation().getY(), course.getStartLocation().getX(),
                    firstPt.getLocation().getY(), firstPt.getLocation().getX());
            double endDist = haversineMeters(
                    course.getEndLocation().getY(), course.getEndLocation().getX(),
                    lastPt.getLocation().getY(), lastPt.getLocation().getX());
            if (startDist > 500 || endDist > 500) {
                verificationStatus = com.runway.attempt.domain.enums.AttemptVerificationStatus.PENDING;
                log.info("Attempt marked unverified (proximity): startDist={}m endDist={}m attemptId={}",
                        startDist, endDist, attemptId);
            }
        }

        // ─── PR 계산 (complete() 호출 전 이전 기록 조회) ───
        Optional<Integer> previousBestOpt = courseAttemptRepository
                .findMinDurationSecondsByCourseIdAndUserId(attempt.getCourseId(), userId);
        boolean isPR;
        Integer previousBestSeconds;
        Integer improvementSeconds;
        if (previousBestOpt.isPresent()) {
            previousBestSeconds = previousBestOpt.get();
            isPR = request.getDurationSeconds() < previousBestSeconds;
            improvementSeconds = previousBestSeconds - request.getDurationSeconds();
        } else {
            // 첫 완주
            isPR = true;
            previousBestSeconds = null;
            improvementSeconds = null;
        }

        // course_attempt 완료 처리
        attempt.complete(endedAt, request.getDurationSeconds(), request.getDistanceMeters(), verificationStatus);

        // courses.completion_count 증가 (동일 트랜잭션)
        course.incrementCompletionCount();

        log.info("Attempt finished: attemptId={} courseId={} durationSeconds={} isPR={}",
                attemptId, attempt.getCourseId(), request.getDurationSeconds(), isPR);
        return FinishAttemptResponse.from(attempt, isPR, previousBestSeconds, improvementSeconds);
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
    public LeaderboardResponse getLeaderboard(UUID userId, UUID courseId, int page, int size, String sortBy) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.isVisibleTo(userId)) {
            throw new RunwayException(ErrorCode.FORBIDDEN);
        }

        boolean byCompletions = "most_completions".equalsIgnoreCase(sortBy);
        String rankOrder = byCompletions
                ? "ORDER BY COUNT(*) DESC, MIN(ca.duration_seconds) ASC"
                : "ORDER BY MIN(ca.duration_seconds) ASC";

        String dataSql = String.format("""
                WITH ranked AS (
                    SELECT
                        u.id               AS user_id,
                        u.nickname         AS nickname,
                        MIN(ca.duration_seconds) AS best_time_seconds,
                        COUNT(*)           AS completion_count,
                        RANK() OVER (%s) AS rank
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
                """, rankOrder);

        String countSql = """
                SELECT COUNT(DISTINCT ca.user_id)
                FROM course_attempts ca
                WHERE ca.course_id = ?
                  AND ca.status = 'completed'
                  AND ca.verification_status = 'verified'
                """;

        String myRankSql = String.format("""
                SELECT ranked.rank FROM (
                    SELECT ca.user_id,
                           RANK() OVER (%s) AS rank
                    FROM course_attempts ca
                    WHERE ca.course_id = ?
                      AND ca.status = 'completed'
                      AND ca.verification_status = 'verified'
                    GROUP BY ca.user_id
                ) ranked
                WHERE ranked.user_id = ?
                """, rankOrder);

        List<Object> dataParams = new ArrayList<>();
        dataParams.add(courseId);
        dataParams.add(size);
        dataParams.add((long) page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = buildQuery(dataSql, dataParams).getResultList();
        List<LeaderboardItem> items = rows.stream().map(this::toLeaderboardItem).toList();

        long total = ((Number) buildQuery(countSql, List.of(courseId)).getSingleResult()).longValue();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);

        @SuppressWarnings("unchecked")
        List<Object> myRankRows = buildQuery(myRankSql, List.of(courseId, userId)).getResultList();
        Long myRank = myRankRows.isEmpty() ? null : ((Number) myRankRows.get(0)).longValue();

        return LeaderboardResponse.builder()
                .courseId(courseId)
                .items(items)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .sortBy(byCompletions ? "most_completions" : "fastest_time")
                .myRank(myRank)
                .build();
    }

    @Transactional(readOnly = true)
    public MyBestAttemptResponse getMyBestAttempt(UUID userId, UUID courseId) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.isVisibleTo(userId)) {
            throw new RunwayException(ErrorCode.FORBIDDEN);
        }

        long count = courseAttemptRepository.countByCourseIdAndUserIdAndStatus(
                courseId, userId, com.runway.attempt.domain.enums.CourseAttemptStatus.COMPLETED);
        Optional<Integer> bestTime = courseAttemptRepository
                .findMinDurationSecondsByCourseIdAndUserId(courseId, userId);
        // 가장 최근 완주 시각
        com.runway.attempt.domain.enums.CourseAttemptStatus completed =
                com.runway.attempt.domain.enums.CourseAttemptStatus.COMPLETED;
        java.time.Instant lastAttemptAt = courseAttemptRepository
                .findByCourseIdAndUserIdOrderByStartedAtDesc(courseId, userId,
                        org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent().stream()
                .filter(a -> a.getStatus() == completed)
                .map(com.runway.attempt.domain.CourseAttempt::getCompletedAt)
                .findFirst()
                .orElse(null);

        return MyBestAttemptResponse.builder()
                .completionCount(count)
                .bestTimeSeconds(bestTime.orElse(null))
                .lastAttemptAt(lastAttemptAt)
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

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

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
