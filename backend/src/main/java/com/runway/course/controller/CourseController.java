package com.runway.course.controller;

import com.runway.common.response.ApiResponse;
import com.runway.common.response.PageResponse;
import com.runway.common.security.UserPrincipal;
import com.runway.course.dto.*;
import com.runway.course.service.CourseReportService;
import com.runway.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.http.HttpStatus;

@Tag(name = "Course", description = "코스 API")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseReportService courseReportService;
    private final com.runway.course.service.CourseRatingService courseRatingService;

    @Operation(summary = "러닝 기록 기반 코스 생성", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/from-run/{runId}")
    public ResponseEntity<ApiResponse<CourseResponse>> createFromRun(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID runId,
            @Valid @RequestBody CreateCourseFromRunRequest request) {
        CourseResponse data = courseService.createCourseFromRun(
                principal.getUserId(), runId, request);
        return ResponseEntity.ok(ApiResponse.success("코스가 생성되었습니다.", data));
    }

    @Operation(summary = "인근 코스 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<PageResponse<NearbyCourseItem>>> getNearby(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "3000") Double radiusMeters,
            @RequestParam(required = false) Double minDistanceMeters,
            @RequestParam(required = false) Double maxDistanceMeters,
            @RequestParam(required = false) Boolean isLoop,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<NearbyCourseItem> data = courseService.getNearby(
                latitude, longitude, radiusMeters,
                minDistanceMeters, maxDistanceMeters, isLoop, keyword,
                page, size);
        return ResponseEntity.ok(ApiResponse.success("인근 코스 조회에 성공했습니다.", data));
    }

    @Operation(summary = "내가 만든 코스 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getMyCourses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<CourseResponse> data = courseService.getMyCourses(
                principal.getUserId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success("내 코스 목록 조회에 성공했습니다.", data));
    }

    @Operation(summary = "코스 상세 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> getCourseDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId) {
        CourseDetailResponse data = courseService.getCourseDetail(
                principal.getUserId(), courseId);
        return ResponseEntity.ok(ApiResponse.success("코스 상세 조회에 성공했습니다.", data));
    }

    @Operation(summary = "코스 경로 포인트 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{courseId}/points")
    public ResponseEntity<ApiResponse<CoursePointsResponse>> getCoursePoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId) {
        CoursePointsResponse data = courseService.getCoursePoints(
                principal.getUserId(), courseId);
        return ResponseEntity.ok(ApiResponse.success("코스 경로 조회에 성공했습니다.", data));
    }

    @Operation(summary = "코스 기본 정보 수정", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateCourseRequest request) {
        CourseResponse data = courseService.updateCourse(
                principal.getUserId(), courseId, request);
        return ResponseEntity.ok(ApiResponse.success("코스가 수정되었습니다.", data));
    }

    @Operation(summary = "코스 공개", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{courseId}/publish")
    public ResponseEntity<ApiResponse<CourseStatusResponse>> publishCourse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId) {
        CourseStatusResponse data = courseService.publishCourse(
                principal.getUserId(), courseId);
        return ResponseEntity.ok(ApiResponse.success("코스가 공개되었습니다.", data));
    }

    @Operation(summary = "코스 보관 처리", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{courseId}/archive")
    public ResponseEntity<ApiResponse<CourseStatusResponse>> archiveCourse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId) {
        CourseStatusResponse data = courseService.archiveCourse(
                principal.getUserId(), courseId);
        return ResponseEntity.ok(ApiResponse.success("코스가 보관 처리되었습니다.", data));
    }

    @Operation(summary = "코스 신고", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{courseId}/reports")
    public ResponseEntity<ApiResponse<CourseReportResponse>> reportCourse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseReportRequest request) {
        CourseReportResponse data = courseReportService.reportCourse(
                principal.getUserId(), courseId, request);
        return ResponseEntity.ok(ApiResponse.success("신고가 접수되었습니다.", data));
    }

    @Operation(summary = "코스 평점 등록/수정", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{courseId}/ratings")
    public ResponseEntity<ApiResponse<CourseRatingResponse>> rateCourse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseRatingRequest request) {
        CourseRatingResponse data = courseRatingService.rateCourse(
                principal.getUserId(), courseId, request);
        return ResponseEntity.ok(ApiResponse.success("평점이 등록되었습니다.", data));
    }

    @Operation(summary = "즐겨찾기 코스 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getFavoriteCourses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<CourseResponse> data = courseService.getFavoriteCourses(
                principal.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("즐겨찾기 코스 목록 조회에 성공했습니다.", data));
    }

    @Operation(summary = "참여한 코스 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/participated")
    public ResponseEntity<ApiResponse<PageResponse<ParticipatedCourseItem>>> getParticipatedCourses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ParticipatedCourseItem> data = courseService.getParticipatedCourses(
                principal.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("참여한 코스 목록 조회에 성공했습니다.", data));
    }

    @Operation(summary = "코스 즐겨찾기 추가", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{courseId}/favorite")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId) {
        courseService.addFavorite(principal.getUserId(), courseId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("즐겨찾기에 추가되었습니다.", null));
    }

    @Operation(summary = "코스 즐겨찾기 제거", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{courseId}/favorite")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId) {
        courseService.removeFavorite(principal.getUserId(), courseId);
        return ResponseEntity.ok(ApiResponse.success("즐겨찾기에서 제거되었습니다.", null));
    }
}
