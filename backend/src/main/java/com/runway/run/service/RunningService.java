package com.runway.run.service;

import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.common.response.PageResponse;
import com.runway.run.domain.RunningPoint;
import com.runway.run.domain.RunningRecord;
import com.runway.run.domain.enums.RunningRecordStatus;
import com.runway.run.dto.*;
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

        List<RunningPoint> points = request.getPoints().stream()
                .map(p -> RunningPoint.builder()
                        .runningRecordId(runId)
                        .sequence(p.getSequence())
                        // JTS Coordinate: x = longitude, y = latitude
                        .location(GEOMETRY_FACTORY.createPoint(new Coordinate(p.getLongitude(), p.getLatitude())))
                        .altitudeMeters(p.getAltitudeMeters())
                        .speedMps(p.getSpeedMps())
                        .recordedAt(p.getRecordedAt())
                        .build())
                .toList();

        runningPointRepository.saveAll(points);
        log.info("Points saved: runId={} count={}", runId, points.size());
        return new SavePointsResponse(runId, points.size());
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
