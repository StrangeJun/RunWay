package com.runway.training.controller;

import com.runway.common.response.ApiResponse;
import com.runway.training.dto.TrainingPlanRequest;
import com.runway.training.dto.TrainingPlanResponse;
import com.runway.training.service.GeminiTrainingPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training-plans")
@RequiredArgsConstructor
public class TrainingPlanController {

    private final GeminiTrainingPlanService trainingPlanService;

    @Operation(summary = "AI 주간 러닝 훈련계획 추천", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<TrainingPlanResponse>> recommend(
            @Valid @RequestBody TrainingPlanRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "주간 훈련계획이 생성되었습니다.",
                trainingPlanService.recommend(request)
        ));
    }
}
