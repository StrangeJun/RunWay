package com.runway.run.service;

import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.common.response.PageResponse;
import com.runway.run.domain.RunningPoint;
import com.runway.run.domain.RunningRecord;
import com.runway.run.domain.enums.RunningRecordStatus;
import com.runway.run.dto.*;
import com.runway.run.dto.PersonalRecordItemResponse;
import com.runway.run.dto.PersonalRecordsResponse;
import com.runway.run.repository.RunDateProjection;
import com.runway.run.repository.RunningPointBatchInserter;
import com.runway.run.repository.RunningPointRepository;
import com.runway.run.repository.RunningRecordRepository;
import com.runway.run.repository.RunningStatsProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunningService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // Stale-record policy (Phase 2): running_records with status IN_PROGRESS or PAUSED
    // older than 24 hours should be auto-abandoned by a scheduled task. This prevents
    // orphaned records from client crashes or network loss. Implement in Phase 2 with
    // @Scheduled(cron = "0 0 3 * * *") and batch UPDATE ... WHERE started_at < now() - interval '24 hours'.

    private final RunningRecordRepository runningRecordRepository;
    private final RunningPointRepository runningPointRepository;
    private final RunningPointBatchInserter runningPointBatchInserter;

    @Transactional
    public StartRunResponse startRun(UUID userId, StartRunRequest request) {
        Instant startedAt = (request.getStartedAt() != null) ? request.getStartedAt() : Instant.now();
        RunningRecord record = RunningRecord.builder()
                .userId(userId)
                .startedAt(startedAt)
                .build();
        RunningRecord saved = runningRecordRepository.save(record);
        log.info("Run started: runId={} userId={}", saved.getId(), userId);
        return StartRunResponse.from(saved);
    }

    @Transactional
    public SavePointsResponse savePoints(UUID userId, UUID runId, SavePointsRequest request) {
        RunningRecord record = findOwnedRecord(runId, userId);

        if (record.getStatus() != RunningRecordStatus.IN_PROGRESS
                && record.getStatus() != RunningRecordStatus.PAUSED) {
            throw new RunwayException(ErrorCode.INVALID_RUN_STATUS);
        }

        int count = request.getPoints().size();
        runningPointBatchInserter.batchInsert(runId, request.getPoints());
        log.info("Points saved (upsert): runId={} count={}", runId, count);
        return new SavePointsResponse(runId, count);
    }

    @Transactional
    public RunStatusResponse pauseRun(UUID userId, UUID runId) {
        RunningRecord record = findOwnedRecord(runId, userId);
        if (record.getStatus() != RunningRecordStatus.IN_PROGRESS) {
            throw new RunwayException(ErrorCode.INVALID_RUN_STATUS);
        }
        record.pause();
        return RunStatusResponse.from(record);
    }

    @Transactional
    public RunStatusResponse resumeRun(UUID userId, UUID runId) {
        RunningRecord record = findOwnedRecord(runId, userId);
        if (record.getStatus() != RunningRecordStatus.PAUSED) {
            throw new RunwayException(ErrorCode.INVALID_RUN_STATUS);
        }
        record.resume();
        return RunStatusResponse.from(record);
    }

    @Transactional
    public FinishRunResponse finishRun(UUID userId, UUID runId, FinishRunRequest request) {
        RunningRecord record = findOwnedRecord(runId, userId);
        if (record.getStatus() != RunningRecordStatus.IN_PROGRESS
                && record.getStatus() != RunningRecordStatus.PAUSED) {
            throw new RunwayException(ErrorCode.INVALID_RUN_STATUS);
        }

        if (!request.getEndedAt().isAfter(record.getStartedAt())) {
            throw new RunwayException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각 이후여야 합니다.");
        }

        // 12 m/s (43.2 km/h) 초과는 인간 러닝으로 물리적으로 불가능한 속도
        if (request.getDistanceMeters() != null && request.getDurationSeconds() != null) {
            boolean zeroTime = request.getDurationSeconds() == 0;
            boolean hasDistance = request.getDistanceMeters() > 0;
            boolean tooFast = !zeroTime && (request.getDistanceMeters() / request.getDurationSeconds() > 12.0);
            if ((zeroTime && hasDistance) || tooFast) {
                throw new RunwayException(ErrorCode.IMPOSSIBLE_SPEED);
            }
        }

        // running_points로 LineString 생성 — 2개 이상 포인트가 있어야 함
        List<RunningPoint> points = runningPointRepository.findByRunningRecordIdOrderBySequenceAsc(runId);
        boolean pathCreated = false;
        if (points.size() >= 2) {
            Coordinate[] coords = points.stream()
                    .map(p -> p.getLocation().getCoordinate())
                    .toArray(Coordinate[]::new);
            LineString path = GEOMETRY_FACTORY.createLineString(coords);
            record.updatePath(path);
            pathCreated = true;
        }

        record.finish(
                request.getEndedAt(),
                request.getDistanceMeters(),
                request.getDurationSeconds(),
                request.getAvgPaceSecondsPerKm(),
                request.getCaloriesBurned(),
                request.getAvgHeartRateBpm()
        );

        log.info("Run finished: runId={} pathCreated={}", runId, pathCreated);
        return FinishRunResponse.from(record, pathCreated);
    }

    @Transactional
    public RunStatusResponse abandonRun(UUID userId, UUID runId) {
        RunningRecord record = findOwnedRecord(runId, userId);
        if (record.getStatus() != RunningRecordStatus.IN_PROGRESS
                && record.getStatus() != RunningRecordStatus.PAUSED) {
            throw new RunwayException(ErrorCode.INVALID_RUN_STATUS);
        }
        record.abandon();
        log.info("Run abandoned: runId={}", runId);
        return RunStatusResponse.from(record);
    }

    @Transactional(readOnly = true)
    public PageResponse<RunSummaryResponse> getMyRuns(UUID userId, int page, int size) {
        return PageResponse.from(
                runningRecordRepository
                        .findByUserIdOrderByStartedAtDesc(userId, PageRequest.of(page, size))
                        .map(RunSummaryResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public RunDetailResponse getRunDetail(UUID userId, UUID runId) {
        RunningRecord record = findOwnedRecord(runId, userId);
        List<RunningPoint> points = runningPointRepository.findByRunningRecordIdOrderBySequenceAsc(runId);
        return RunDetailResponse.from(record, points);
    }

    @Transactional(readOnly = true)
    public PersonalRecordsResponse getPersonalRecords(UUID userId) {
        RunningRecordStatus completed = RunningRecordStatus.COMPLETED;

        PersonalRecordItemResponse longestRun = runningRecordRepository
                .findTop1ByUserIdAndStatusOrderByDistanceMetersDesc(userId, completed)
                .map(PersonalRecordItemResponse::from).orElse(null);

        PersonalRecordItemResponse fastestPace = runningRecordRepository
                .findFastestPace(userId, completed, 1000.0)
                .map(PersonalRecordItemResponse::from).orElse(null);

        PersonalRecordItemResponse mostCalories = runningRecordRepository
                .findTop1ByUserIdAndStatusAndCaloriesBurnedNotNullOrderByCaloriesBurnedDesc(userId, completed)
                .map(PersonalRecordItemResponse::from).orElse(null);

        PersonalRecordItemResponse best5k = runningRecordRepository
                .findBestTimeForDistance(userId, completed, 5000.0)
                .map(PersonalRecordItemResponse::from).orElse(null);

        PersonalRecordItemResponse best10k = runningRecordRepository
                .findBestTimeForDistance(userId, completed, 10000.0)
                .map(PersonalRecordItemResponse::from).orElse(null);

        long totalCompletedRuns = runningRecordRepository.countByUserIdAndStatus(userId, completed);
        double totalDistanceMeters = runningRecordRepository.sumDistanceMetersByUserIdAndStatus(userId, completed);

        return PersonalRecordsResponse.builder()
                .longestRun(longestRun)
                .fastestPace(fastestPace)
                .mostCalories(mostCalories)
                .best5k(best5k)
                .best10k(best10k)
                .totalCompletedRuns(totalCompletedRuns)
                .totalDistanceMeters(totalDistanceMeters)
                .build();
    }

    @Transactional(readOnly = true)
    public RunningStatsResponse getRunningStats(UUID userId, String period) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        Instant periodStart;
        Instant periodEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        switch (period.toLowerCase()) {
            case "weekly" -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                periodStart = monday.atStartOfDay(ZoneOffset.UTC).toInstant();
            }
            case "yearly" -> {
                LocalDate jan1 = today.with(TemporalAdjusters.firstDayOfYear());
                periodStart = jan1.atStartOfDay(ZoneOffset.UTC).toInstant();
            }
            case "all" -> {
                periodStart = Instant.EPOCH;
                periodEnd = now;
            }
            default -> { // monthly
                LocalDate first = today.with(TemporalAdjusters.firstDayOfMonth());
                periodStart = first.atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        }

        RunningRecordStatus completed = RunningRecordStatus.COMPLETED;

        // DB aggregate projection으로 기간 내 통계 조회
        RunningStatsProjection stats = runningRecordRepository
                .aggregateStatsByPeriod(userId, completed.name(), periodStart, periodEnd);

        long totalRuns = stats.getTotalRuns();
        double totalDistanceMeters = stats.getTotalDistanceMeters();
        long totalDurationSeconds = stats.getTotalDurationSeconds();
        long totalCaloriesBurned = stats.getTotalCaloriesBurned();
        double longestRunMeters = stats.getLongestRunMeters();
        long activeDays = stats.getActiveDays();

        int avgPace = (totalDistanceMeters > 1.0) ? (int) (totalDurationSeconds / (totalDistanceMeters / 1000.0)) : 0;

        // streak 계산 — 전체 완료 런 기준 (기간 무관)
        List<LocalDate> runDates = runningRecordRepository
                .findDistinctRunDatesByUserId(userId, completed.name())
                .stream()
                .map(RunDateProjection::getRunDate)
                .collect(java.util.stream.Collectors.toList());
        int[] streaks = calculateStreaksFromDates(runDates, today);

        return RunningStatsResponse.builder()
                .period(period.toLowerCase())
                .totalRuns(totalRuns)
                .totalDistanceMeters(totalDistanceMeters)
                .totalDurationSeconds(totalDurationSeconds)
                .totalCaloriesBurned(totalCaloriesBurned)
                .averagePaceSecondsPerKm(avgPace)
                .longestRunMeters(longestRunMeters)
                .currentStreakDays(streaks[0])
                .longestStreakDays(streaks[1])
                .activeDays(activeDays)
                .periodStart(periodStart)
                .periodEnd(periodEnd.equals(now) ? periodEnd : periodEnd.minus(1, ChronoUnit.SECONDS))
                .build();
    }

    // [0] = currentStreak, [1] = longestStreak
    private int[] calculateStreaksFromDates(List<LocalDate> runDates, LocalDate today) {
        if (runDates.isEmpty()) return new int[]{0, 0};

        Set<LocalDate> runDateSet = new HashSet<>(runDates);

        // 현재 streak: 오늘 또는 어제부터 연속일 수
        int current = 0;
        LocalDate check = today;
        if (!runDateSet.contains(check)) {
            check = today.minusDays(1);
        }
        while (runDateSet.contains(check)) {
            current++;
            check = check.minusDays(1);
        }

        // 최장 streak: 날짜 정렬 후 연속일 카운트
        List<LocalDate> sorted = new ArrayList<>(runDateSet);
        sorted.sort(null);
        int longest = 1, cur = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                cur++;
                if (cur > longest) longest = cur;
            } else {
                cur = 1;
            }
        }

        return new int[]{current, longest};
    }

    @Transactional
    public void deleteRun(UUID userId, UUID runId) {
        RunningRecord record = findOwnedRecord(runId, userId);
        runningRecordRepository.delete(record);
        log.info("Run deleted: runId={}", runId);
    }

    @Transactional
    public void trimRun(UUID userId, UUID runId, double targetDistanceMeters) {
        RunningRecord record = findOwnedRecord(runId, userId);

        if (record.getDistanceMeters() == null || record.getDistanceMeters() <= 0) {
            throw new RunwayException(ErrorCode.INVALID_REQUEST, "기록된 거리가 없습니다.");
        }
        if (targetDistanceMeters <= 0 || targetDistanceMeters >= record.getDistanceMeters()) {
            throw new RunwayException(ErrorCode.INVALID_REQUEST,
                    "수정 거리는 0보다 크고 현재 기록(" +
                    String.format("%.1f", record.getDistanceMeters() / 1000.0) + "km)보다 작아야 합니다.");
        }

        // 포인트 조회 후 커트 지점 계산
        List<RunningPoint> points = runningPointRepository
                .findByRunningRecordIdOrderBySequenceAsc(runId);

        double cumulative = 0.0;
        int cutoffSeq = -1;
        for (int i = 1; i < points.size(); i++) {
            RunningPoint prev = points.get(i - 1);
            RunningPoint curr = points.get(i);
            cumulative += haversineMeters(
                    prev.getLocation().getY(), prev.getLocation().getX(),
                    curr.getLocation().getY(), curr.getLocation().getX()
            );
            if (cumulative >= targetDistanceMeters) {
                cutoffSeq = curr.getSequence();
                break;
            }
        }

        if (cutoffSeq > 0) {
            runningPointRepository.deleteByRunningRecordIdAndSequenceGreaterThan(runId, cutoffSeq);
        }

        record.trim(targetDistanceMeters);
        log.info("Run trimmed: runId={}, targetDistanceMeters={}", runId, targetDistanceMeters);
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000.0;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private RunningRecord findOwnedRecord(UUID runId, UUID userId) {
        return runningRecordRepository.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new RunwayException(ErrorCode.RUN_NOT_FOUND));
    }
}
