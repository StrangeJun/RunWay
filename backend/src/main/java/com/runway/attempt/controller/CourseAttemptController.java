package com.runway.attempt.controller;

import com.runway.attempt.dto.*;
import com.runway.attempt.service.CourseAttemptService;
import com.runway.common.response.ApiResponse;
import com.runway.common.response.PageResponse;
import com.runway.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Course Attempt", description = "코스 도전 및 리더보드 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseAttemptController {

    private final CourseAttemptService courseAttemptService;

    @Operation(summary = "코스 도전 시작", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/courses/{courseId}/attempts/start")
    public ResponseEntity<ApiResponse<StartAttemptResponse>> startAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId,
            @RequestBody(required = false) StartAttemptRequest request) {
        StartAttemptResponse data = courseAttemptService.startAttempt(
                principal.getUserId(), courseId, request);
        return ResponseEntity.ok(ApiResponse.success("코스 도전이 시작되었습니다.", data));
    }

    @Operation(summary = "코스 도전 완주", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/course-attempts/{attemptId}/finish")
    public ResponseEntity<ApiResponse<FinishAttemptResponse>> finishAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID attemptId,
            @Valid @RequestBody FinishAttemptRequest request) {
        FinishAttemptResponse data = courseAttemptService.finishAttempt(
                principal.getUserId(), attemptId, request);
        return ResponseEntity.ok(ApiResponse.success("코스 도전이 완료되었습니다.", data));
    }

    @Operation(summary = "코스 도전 포기", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/course-attempts/{attemptId}/abandon")
    public ResponseEntity<ApiResponse<AbandonAttemptResponse>> abandonAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID attemptId,
            @RequestBody(required = false) AbandonAttemptRequest request) {
        AbandonAttemptResponse data = courseAttemptService.abandonAttempt(
                principal.getUserId(), attemptId, request);
        return ResponseEntity.ok(ApiResponse.success("코스 도전이 중단되었습니다.", data));
    }

    @Operation(summary = "코스 리더보드 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/courses/{courseId}/leaderboard")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getLeaderboard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        LeaderboardResponse data = courseAttemptService.getLeaderboard(
                principal.getUserId(), courseId, page, size);
        return ResponseEntity.ok(ApiResponse.success("코스 리더보드 조회가 완료되었습니다.", data));
    }

    @Operation(summary = "내 코스 도전 기록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/courses/{courseId}/attempts/me")
    public ResponseEntity<ApiResponse<PageResponse<CourseAttemptResponse>>> getMyAttempts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<CourseAttemptResponse> data = courseAttemptService.getMyAttempts(
                principal.getUserId(), courseId, page, size);
        return ResponseEntity.ok(ApiResponse.success("내 코스 도전 기록 조회가 완료되었습니다.", data));
    }
}
