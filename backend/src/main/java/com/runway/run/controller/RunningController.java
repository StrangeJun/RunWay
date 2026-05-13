package com.runway.run.controller;

import com.runway.common.response.ApiResponse;
import com.runway.common.response.PageResponse;
import com.runway.common.security.UserPrincipal;
import com.runway.run.dto.*;
import com.runway.run.service.RunningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Running", description = "러닝 기록 API")
@RestController
@RequestMapping("/api/runs")
@RequiredArgsConstructor
public class RunningController {

    private final RunningService runningService;

    @Operation(summary = "런 시작", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<StartRunResponse>> startRun(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) StartRunRequest request) {
        StartRunResponse data = runningService.startRun(
                principal.getUserId(),
                request != null ? request : new StartRunRequest()
        );
        return ResponseEntity.ok(ApiResponse.success("러닝이 시작되었습니다.", data));
    }

    @Operation(summary = "GPS 포인트 배치 저장", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{runId}/points")
    public ResponseEntity<ApiResponse<SavePointsResponse>> savePoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID runId,
            @Valid @RequestBody SavePointsRequest request) {
        SavePointsResponse data = runningService.savePoints(principal.getUserId(), runId, request);
        return ResponseEntity.ok(ApiResponse.success("GPS 포인트가 저장되었습니다.", data));
    }

    @Operation(summary = "런 일시정지", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{runId}/pause")
    public ResponseEntity<ApiResponse<RunStatusResponse>> pauseRun(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID runId) {
        RunStatusResponse data = runningService.pauseRun(principal.getUserId(), runId);
        return ResponseEntity.ok(ApiResponse.success("런이 일시정지되었습니다.", data));
    }

    @Operation(summary = "런 재개", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{runId}/resume")
    public ResponseEntity<ApiResponse<RunStatusResponse>> resumeRun(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID runId) {
        RunStatusResponse data = runningService.resumeRun(principal.getUserId(), runId);
        return ResponseEntity.ok(ApiResponse.success("런이 재개되었습니다.", data));
    }

    @Operation(summary = "런 완료", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{runId}/finish")
    public ResponseEntity<ApiResponse<FinishRunResponse>> finishRun(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID runId,
            @Valid @RequestBody FinishRunRequest request) {
        FinishRunResponse data = runningService.finishRun(principal.getUserId(), runId, request);
        return ResponseEntity.ok(ApiResponse.success("런이 완료되었습니다.", data));
    }

    @Operation(summary = "런 중단", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{runId}/abandon")
    public ResponseEntity<ApiResponse<RunStatusResponse>> abandonRun(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID runId) {
        RunStatusResponse data = runningService.abandonRun(principal.getUserId(), runId);
        return ResponseEntity.ok(ApiResponse.success("런이 중단되었습니다.", data));
    }

    @Operation(summary = "내 러닝 기록 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<RunSummaryResponse>>> getMyRuns(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<RunSummaryResponse> data = runningService.getMyRuns(principal.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("내 러닝 기록 목록 조회가 완료되었습니다.", data));
    }

    @Operation(summary = "러닝 기록 상세 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{runId}")
    public ResponseEntity<ApiResponse<RunDetailResponse>> getRunDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID runId) {
        RunDetailResponse data = runningService.getRunDetail(principal.getUserId(), runId);
        return ResponseEntity.ok(ApiResponse.success("러닝 기록 상세 조회가 완료되었습니다.", data));
    }

    @Operation(summary = "러닝 기록 삭제", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{runId}")
    public ResponseEntity<ApiResponse<Void>> deleteRun(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID runId) {
        runningService.deleteRun(principal.getUserId(), runId);
        return ResponseEntity.ok(ApiResponse.success("러닝 기록이 삭제되었습니다."));
    }
}
