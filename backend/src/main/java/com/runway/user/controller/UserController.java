package com.runway.user.controller;

import com.runway.common.response.ApiResponse;
import com.runway.common.security.UserPrincipal;
import com.runway.user.dto.UpdateProfileRequest;
import com.runway.user.dto.UserProfileResponse;
import com.runway.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 프로필 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse data = userService.getProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("내 프로필 조회가 완료되었습니다.", data));
    }

    @Operation(summary = "내 프로필 수정", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse data = userService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("내 프로필 수정이 완료되었습니다.", data));
    }

    @Operation(summary = "회원 탈퇴", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteUser(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다."));
    }
}
