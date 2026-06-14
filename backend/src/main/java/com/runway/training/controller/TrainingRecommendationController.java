package com.runway.training.controller;

import com.runway.common.response.ApiResponse;
import com.runway.common.security.UserPrincipal;
import com.runway.training.dto.TrainingRecommendationRequest;
import com.runway.training.dto.TrainingRecommendationResponse;
import com.runway.training.service.TrainingRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Training", description = "AI 러닝 훈련 추천 API")
@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingRecommendationController {

    private final TrainingRecommendationService trainingRecommendationService;

    @Operation(summary = "목표 기반 1주 훈련 추천", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/recommendation")
    public ResponseEntity<ApiResponse<TrainingRecommendationResponse>> recommend(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TrainingRecommendationRequest request
    ) {
        TrainingRecommendationResponse data =
                trainingRecommendationService.recommend(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("훈련 추천이 생성되었습니다.", data));
    }
}
