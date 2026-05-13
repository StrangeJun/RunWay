package com.runway.course.service;

import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.common.response.PageResponse;
import com.runway.course.domain.Course;
import com.runway.course.domain.CoursePoint;
import com.runway.course.domain.enums.CourseStatus;
import com.runway.course.dto.*;
import com.runway.course.repository.CoursePointRepository;
import com.runway.course.repository.CourseRepository;
import com.runway.run.domain.RunningPoint;
import com.runway.run.domain.enums.RunningRecordStatus;
import com.runway.run.repository.RunningPointRepository;
import com.runway.run.repository.RunningRecordRepository;
import com.runway.user.domain.User;
import com.runway.user.repository.UserRepository;
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

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final int MAX_COURSE_POINTS = 200;

    private final CourseRepository courseRepository;
    private final CoursePointRepository coursePointRepository;
    private final RunningRecordRepository runningRecordRepository;
    private final RunningPointRepository runningPointRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Transactional
    public CourseResponse createCourseFromRun(UUID userId, UUID runId,
                                              CreateCourseFromRunRequest request) {
        var record = runningRecordRepository.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new RunwayException(ErrorCode.RUN_NOT_FOUND));

        if (record.getStatus() != RunningRecordStatus.COMPLETED) {
            throw new RunwayException(ErrorCode.NOT_COMPLETED_RUN);
        }

        List<RunningPoint> runningPoints =
                runningPointRepository.findByRunningRecordIdOrderBySequenceAsc(runId);

        if (runningPoints.size() < 2) {
            throw new RunwayException(ErrorCode.INVALID_REQUEST,
                    "코스 생성을 위해 최소 2개 이상의 GPS 포인트가 필요합니다.");
        }

        // 다운샘플링: MAX_COURSE_POINTS 이하로 샘플링
        List<RunningPoint> sampled = downsample(runningPoints);

        // 코스 경로 LineString 생성 (다운샘플링된 포인트 기준)
        Coordinate[] coords = sampled.stream()
                .map(p -> p.getLocation().getCoordinate())
                .toArray(Coordinate[]::new);
        LineString path = GEOMETRY_FACTORY.createLineString(coords);

        CourseStatus status = Boolean.TRUE.equals(request.getPublish())
                ? CourseStatus.PUBLISHED : CourseStatus.DRAFT;

        Course course = Course.builder()
                .creatorId(userId)
                .sourceRecordId(runId)
                .name(request.getName())
                .description(request.getDescription())
                .status(status)
                .distanceMeters(record.getDistanceMeters())
                .isLoop(request.getIsLoop())
                .startLocation(runningPoints.get(0).getLocation())
                .endLocation(runningPoints.get(runningPoints.size() - 1).getLocation())
                .build();

        courseRepository.save(course);
        course.updatePath(path);

        // course_points 저장 (courseId 확정 후)
        List<CoursePoint> coursePoints = buildCoursePoints(course.getId(), sampled);
        coursePointRepository.saveAll(coursePoints);

        log.info("Course created: courseId={} runId={} points={} status={}",
                course.getId(), runId, coursePoints.size(), status.getDbValue());
        return CourseResponse.from(course);
    }

    @Transactional(readOnly = true)
    public PageResponse<NearbyCourseItem> getNearby(
            Double latitude, Double longitude, Double radiusMeters,
            Double minDistanceMeters, Double maxDistanceMeters, Boolean isLoop,
            int page, int size) {

        // CTE로 현재 위치 포인트를 한 번 계산하고 재사용
        String refPoint = "ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography";

        List<Object> baseParams = new ArrayList<>();
        baseParams.add(longitude);    // ?1 — ST_MakePoint(lon, lat)
        baseParams.add(latitude);     // ?2
        baseParams.add(radiusMeters); // ?3 — ST_DWithin 반경

        StringBuilder optFilter = new StringBuilder();
        List<Object> filterParams = new ArrayList<>();

        if (minDistanceMeters != null) {
            optFilter.append(" AND c.distance_meters >= ?");
            filterParams.add(minDistanceMeters);
        }
        if (maxDistanceMeters != null) {
            optFilter.append(" AND c.distance_meters <= ?");
            filterParams.add(maxDistanceMeters);
        }
        if (isLoop != null) {
            optFilter.append(" AND c.is_loop = ?");
            filterParams.add(isLoop);
        }

        String baseSql =
                "WITH ref AS (SELECT " + refPoint + " AS pt) " +
                "FROM courses c, ref " +
                "WHERE ST_DWithin(c.start_location, ref.pt, ?) " +
                "  AND c.status = 'published' AND c.deleted_at IS NULL" +
                optFilter;

        String dataSql =
                "WITH ref AS (SELECT " + refPoint + " AS pt) " +
                "SELECT c.id, c.name, c.description, c.distance_meters, " +
                "       ST_Distance(c.start_location, ref.pt) AS dist_from_me, " +
                "       c.is_loop, c.attempt_count, c.completion_count, " +
                "       ST_Y(c.start_location::geometry) AS start_lat, " +
                "       ST_X(c.start_location::geometry) AS start_lon " +
                "FROM courses c, ref " +
                "WHERE ST_DWithin(c.start_location, ref.pt, ?) " +
                "  AND c.status = 'published' AND c.deleted_at IS NULL" +
                optFilter +
                " ORDER BY dist_from_me ASC" +
                " LIMIT ? OFFSET ?";

        String countSql =
                "WITH ref AS (SELECT " + refPoint + " AS pt) " +
                "SELECT COUNT(*) " +
                "FROM courses c, ref " +
                "WHERE ST_DWithin(c.start_location, ref.pt, ?) " +
                "  AND c.status = 'published' AND c.deleted_at IS NULL" +
                optFilter;

        // 데이터 쿼리 파라미터 구성
        List<Object> dataParams = new ArrayList<>(baseParams);
        dataParams.addAll(filterParams);
        dataParams.add(size);
        dataParams.add((long) page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = buildQuery(dataSql, dataParams).getResultList();
        List<NearbyCourseItem> items = rows.stream().map(this::toNearbyCourseItem).toList();

        // 카운트 쿼리 파라미터 구성 (LIMIT/OFFSET 없음)
        List<Object> countParams = new ArrayList<>(baseParams);
        countParams.addAll(filterParams);
        long total = ((Number) buildQuery(countSql, countParams).getSingleResult()).longValue();

        return PageResponse.from(new PageImpl<>(items, PageRequest.of(page, size), total));
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getMyCourses(UUID userId, String statusParam,
                                                     int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);

        if (statusParam != null) {
            CourseStatus status;
            try {
                status = CourseStatus.from(statusParam);
            } catch (IllegalArgumentException e) {
                throw new RunwayException(ErrorCode.INVALID_REQUEST,
                        "유효하지 않은 코스 상태입니다: " + statusParam);
            }
            return PageResponse.from(
                    courseRepository
                            .findByCreatorIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                                    userId, status, pageable)
                            .map(CourseResponse::from));
        }

        return PageResponse.from(
                courseRepository
                        .findByCreatorIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, pageable)
                        .map(CourseResponse::from));
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(UUID userId, UUID courseId) {
        Course course = findVisibleCourse(courseId, userId);
        User creator = userRepository.findById(course.getCreatorId())
                .orElseThrow(() -> new RunwayException(ErrorCode.USER_NOT_FOUND));
        return CourseDetailResponse.from(course, creator);
    }

    @Transactional(readOnly = true)
    public CoursePointsResponse getCoursePoints(UUID userId, UUID courseId) {
        Course course = findVisibleCourse(courseId, userId);
        List<CoursePointResponse> points = coursePointRepository
                .findByCourseIdOrderBySequenceAsc(course.getId())
                .stream()
                .map(CoursePointResponse::from)
                .toList();
        return CoursePointsResponse.builder()
                .courseId(course.getId())
                .points(points)
                .build();
    }

    @Transactional
    public CourseResponse updateCourse(UUID userId, UUID courseId, UpdateCourseRequest request) {
        Course course = findOwnedCourse(courseId, userId);
        course.updateInfo(request.getName(), request.getDescription(), request.getIsLoop());
        log.info("Course updated: courseId={}", courseId);
        return CourseResponse.from(course);
    }

    @Transactional
    public CourseStatusResponse publishCourse(UUID userId, UUID courseId) {
        Course course = findOwnedCourse(courseId, userId);
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            throw new RunwayException(ErrorCode.INVALID_COURSE_STATUS);
        }
        course.publish();
        log.info("Course published: courseId={}", courseId);
        return CourseStatusResponse.from(course);
    }

    @Transactional
    public CourseStatusResponse archiveCourse(UUID userId, UUID courseId) {
        Course course = findOwnedCourse(courseId, userId);
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new RunwayException(ErrorCode.INVALID_COURSE_STATUS);
        }
        course.archive();
        log.info("Course archived: courseId={}", courseId);
        return CourseStatusResponse.from(course);
    }

    // --- private helpers ---

    private Course findOwnedCourse(UUID courseId, UUID userId) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.isOwnedBy(userId)) {
            throw new RunwayException(ErrorCode.FORBIDDEN);
        }
        return course;
    }

    private Course findVisibleCourse(UUID courseId, UUID userId) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_NOT_FOUND));
        if (!course.isVisibleTo(userId)) {
            throw new RunwayException(ErrorCode.FORBIDDEN);
        }
        return course;
    }

    private List<RunningPoint> downsample(List<RunningPoint> all) {
        if (all.size() <= MAX_COURSE_POINTS) {
            return new ArrayList<>(all);
        }
        // 간격 샘플링: 첫 점과 끝 점은 항상 포함
        List<RunningPoint> result = new ArrayList<>();
        int step = all.size() / MAX_COURSE_POINTS;
        for (int i = 0; i < all.size(); i++) {
            if (i == 0 || i == all.size() - 1 || i % step == 0) {
                result.add(all.get(i));
            }
        }
        return result;
    }

    private List<CoursePoint> buildCoursePoints(UUID courseId, List<RunningPoint> sampledPoints) {
        List<CoursePoint> result = new ArrayList<>();
        for (int i = 0; i < sampledPoints.size(); i++) {
            RunningPoint rp = sampledPoints.get(i);
            result.add(CoursePoint.builder()
                    .courseId(courseId)
                    .sequence(i)
                    .location(rp.getLocation())
                    .altitudeMeters(rp.getAltitudeMeters())
                    .build());
        }
        return result;
    }

    private jakarta.persistence.Query buildQuery(String sql, List<Object> params) {
        jakarta.persistence.Query query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }
        return query;
    }

    private NearbyCourseItem toNearbyCourseItem(Object[] row) {
        return NearbyCourseItem.builder()
                .courseId(UUID.fromString(row[0].toString()))
                .name((String) row[1])
                .description((String) row[2])
                .distanceMeters(((Number) row[3]).doubleValue())
                .distanceFromMeMeters(((Number) row[4]).doubleValue())
                .isLoop((Boolean) row[5])
                .attemptCount(((Number) row[6]).intValue())
                .completionCount(((Number) row[7]).intValue())
                .startPoint(new GeoPoint(
                        ((Number) row[8]).doubleValue(),   // start_lat
                        ((Number) row[9]).doubleValue()))  // start_lon
                .build();
    }
}
