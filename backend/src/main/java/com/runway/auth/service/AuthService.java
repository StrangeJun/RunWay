package com.runway.auth.service;

import com.runway.auth.dto.*;
import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.common.security.JwtProvider;
import com.runway.user.domain.User;
import com.runway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new RunwayException(ErrorCode.DUPLICATED_EMAIL);
        }
        if (userRepository.existsByNicknameAndDeletedAtIsNull(request.getNickname())) {
            throw new RunwayException(ErrorCode.DUPLICATED_NICKNAME);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        User saved = userRepository.save(user);
        log.info("User signed up: {}", saved.getEmail());
        return SignupResponse.from(saved);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new RunwayException(ErrorCode.INVALID_REQUEST, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RunwayException(ErrorCode.INVALID_REQUEST, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());

        user.updateRefreshTokenHash(jwtProvider.hashToken(refreshToken));
        log.info("User logged in: {}", user.getEmail());
        return LoginResponse.of(accessToken, refreshToken, user);
    }

    @Transactional
    public ReissueResponse reissue(ReissueRequest request) {
        String refreshToken = request.getRefreshToken();

        // 만료 → EXPIRED_TOKEN, 위조 → INVALID_TOKEN 구분
        UUID userId = jwtProvider.validateRefreshTokenAndGetUserId(refreshToken);

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RunwayException(ErrorCode.USER_NOT_FOUND));

        String incomingHash = jwtProvider.hashToken(refreshToken);
        if (!incomingHash.equals(user.getRefreshTokenHash())) {
            // DB의 해시와 불일치 → 이미 rotate되었거나 탈취된 토큰
            throw new RunwayException(ErrorCode.INVALID_TOKEN);
        }

        String newAccessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());

        user.updateRefreshTokenHash(jwtProvider.hashToken(newRefreshToken));
        log.info("Token reissued for user: {}", user.getEmail());
        return ReissueResponse.of(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RunwayException(ErrorCode.USER_NOT_FOUND));

        user.updateRefreshTokenHash(null);
        log.info("User logged out: {}", user.getEmail());
    }
}
