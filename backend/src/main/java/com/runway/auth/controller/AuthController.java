package com.runway.auth.controller;

import com.runway.auth.dto.*;
import com.runway.auth.service.AuthService;
import com.runway.auth.service.GoogleAuthService;
import com.runway.auth.service.KakaoAuthService;
import com.runway.auth.service.AuthVerificationService;
import com.runway.auth.domain.VerificationPurpose;
import com.runway.common.response.ApiResponse;
import com.runway.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final KakaoAuthService kakaoAuthService;
    private final AuthVerificationService authVerificationService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @Operation(summary = "닉네임 중복 확인")
    @PostMapping("/nickname/check")
    public ResponseEntity<ApiResponse<NicknameAvailabilityResponse>> checkNickname(
            @Valid @RequestBody NicknameAvailabilityRequest request) {
        NicknameAvailabilityResponse response =
                authService.checkNicknameAvailability(request);
        return ResponseEntity.ok(ApiResponse.success(
                response.available() ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다.",
                response
        ));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", response));
    }

    @Operation(summary = "회원가입 이메일 인증번호 발송")
    @PostMapping("/email-verification/signup/request")
    public ResponseEntity<ApiResponse<ActionResponse>> requestSignupEmailCode(
            @Valid @RequestBody EmailCodeRequest request) {
        authVerificationService.requestSignupCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "인증번호를 발송했습니다.",
                ActionResponse.accepted()
        ));
    }

    @Operation(summary = "회원가입 이메일 인증번호 확인")
    @PostMapping("/email-verification/signup/verify")
    public ResponseEntity<ApiResponse<VerificationTokenResponse>> verifySignupEmailCode(
            @Valid @RequestBody VerifyEmailCodeRequest request) {
        String token = authVerificationService.verifyCode(
                request.getEmail(),
                request.getCode(),
                VerificationPurpose.SIGNUP
        );
        return ResponseEntity.ok(ApiResponse.success(
                "이메일 인증이 완료되었습니다.",
                new VerificationTokenResponse(token)
        ));
    }

    @Operation(summary = "비밀번호 재설정 인증번호 발송")
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<ActionResponse>> requestPasswordReset(
            @Valid @RequestBody EmailCodeRequest request) {
        authVerificationService.requestPasswordResetCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "가입된 이메일인 경우 인증번호를 발송했습니다.",
                ActionResponse.accepted()
        ));
    }

    @Operation(summary = "비밀번호 재설정 인증번호 확인")
    @PostMapping("/password-reset/verify")
    public ResponseEntity<ApiResponse<VerificationTokenResponse>> verifyPasswordResetCode(
            @Valid @RequestBody VerifyEmailCodeRequest request) {
        String token = authVerificationService.verifyCode(
                request.getEmail(),
                request.getCode(),
                VerificationPurpose.PASSWORD_RESET
        );
        return ResponseEntity.ok(ApiResponse.success(
                "인증이 완료되었습니다.",
                new VerificationTokenResponse(token)
        ));
    }

    @Operation(summary = "비밀번호 재설정")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<ActionResponse>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "비밀번호가 변경되었습니다.",
                ActionResponse.accepted()
        ));
    }

    @Operation(summary = "Access Token 재발급")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<ReissueResponse>> reissue(
            @Valid @RequestBody ReissueRequest request) {
        ReissueResponse response = authService.reissue(request);
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", response));
    }

    @Operation(summary = "Google 소셜 로그인")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {
        LoginResponse response = googleAuthService.loginWithGoogle(request.getIdToken());
        return ResponseEntity.ok(ApiResponse.success("Google 로그인이 완료되었습니다.", response));
    }

    @Operation(summary = "카카오 소셜 로그인")
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request) {
        LoginResponse response = kakaoAuthService.loginWithKakao(request.getAccessToken());
        return ResponseEntity.ok(ApiResponse.success("카카오 로그인이 완료되었습니다.", response));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) LogoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다."));
    }
}
