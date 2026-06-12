package com.runway.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runway.auth.dto.LoginResponse;
import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.common.security.JwtProvider;
import com.runway.user.domain.User;
import com.runway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Transactional
    public LoginResponse loginWithGoogle(String idToken) {
        GoogleUserInfo info = verifyIdToken(idToken);
        User user = findOrCreateUser(info);

        String accessToken  = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());
        user.updateRefreshTokenHash(jwtProvider.hashToken(refreshToken));

        log.info("Google login: {}", user.getEmail());
        return LoginResponse.of(accessToken, refreshToken, user);
    }

    // ── Google tokeninfo 검증 ─────────────────────────────────────────────────

    private GoogleUserInfo verifyIdToken(String idToken) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TOKENINFO_URL + idToken))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RunwayException(ErrorCode.INVALID_REQUEST, "유효하지 않은 Google 토큰입니다.");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String sub   = getField(json, "sub");
            String email = getField(json, "email");
            String name  = json.has("name") ? json.get("name").asText() : null;

            return new GoogleUserInfo(sub, email, name);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RunwayException(ErrorCode.INTERNAL_SERVER_ERROR, "Google 토큰 검증 중 오류가 발생했습니다.");
        }
    }

    private String getField(JsonNode json, String field) {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new RunwayException(ErrorCode.INVALID_REQUEST, "유효하지 않은 Google 토큰입니다.");
        }
        return json.get(field).asText();
    }

    // ── 사용자 조회 / 자동 생성 ───────────────────────────────────────────────

    private User findOrCreateUser(GoogleUserInfo info) {
        // 기존 소셜 계정 검색
        return userRepository.findBySocialProviderAndSocialId("google", info.sub())
                .orElseGet(() -> {
                    // 동일 이메일로 일반 계정이 있으면 소셜 연결
                    return userRepository.findByEmailAndDeletedAtIsNull(info.email())
                            .map(existing -> {
                                existing.linkSocial("google", info.sub());
                                return existing;
                            })
                            // 신규 유저 생성
                            .orElseGet(() -> {
                                String nickname = generateNickname(info);
                                User newUser = User.builder()
                                        .email(info.email())
                                        .nickname(nickname)
                                        .socialProvider("google")
                                        .socialId(info.sub())
                                        .build();
                                return userRepository.save(newUser);
                            });
                });
    }

    private String generateNickname(GoogleUserInfo info) {
        String base = info.name() != null
                ? info.name().replaceAll("\\s+", "").substring(0, Math.min(info.name().replaceAll("\\s+", "").length(), 15))
                : info.email().split("@")[0];

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByNicknameAndDeletedAtIsNull(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private record GoogleUserInfo(String sub, String email, String name) {}
}
