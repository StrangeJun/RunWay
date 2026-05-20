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
import com.runway.run.repository.RunningPointRepository;
import com.runway.run.repository.RunningRecordRepository;
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
import java.util.List;
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
        for (SavePointsRequest.PointData p : request.getPoints()) {
            runningPointRepository.insertIgnoreConflict(
                    runId, p.getSequence(), p.getLatitude(), p.getLongitude(),
                    p.getAltitudeMeters(), p.getSpeedMps(), p.getRecordedAt()
            );
        }
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

    @Transactional
    public void deleteRun(UUID userId, UUID runId) {
        RunningRecord record = findOwnedRecord(runId, userId);
        runningRecordRepository.delete(record);
        // DB의 ON DELETE CASCADE가 running_points를 자동으로 삭제함
        log.info("Run deleted: runId={}", runId);
    }

    private RunningRecord findOwnedRecord(UUID runId, UUID userId) {
        return runningRecordRepository.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new RunwayException(ErrorCode.RUN_NOT_FOUND));
    }
}
