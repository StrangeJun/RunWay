package com.runway.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runway.auth.dto.LoginResponse;
import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.common.security.JwtProvider;
import com.runway.user.domain.User;
import com.runway.user.domain.UserSocialAccount;
import com.runway.user.repository.UserRepository;
import com.runway.user.repository.UserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    private static final String GOOGLE_ISS_1 = "accounts.google.com";
    private static final String GOOGLE_ISS_2 = "https://accounts.google.com";

    @Value("${social.google.client-id}")
    private String googleClientId;

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Transactional
    public LoginResponse loginWithGoogle(String idToken) {
        GoogleUserInfo info = verifyIdToken(idToken);
        User user = findOrCreateUser(info);

        String accessToken  = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getCredentialVersion());
        String refreshToken = jwtProvider.generateRefreshToken(
                user.getId(), user.getEmail(), user.getCredentialVersion());
        user.updateRefreshTokenHash(jwtProvider.hashToken(refreshToken));

        log.info("Google login: userId={}", user.getId());
        return LoginResponse.of(accessToken, refreshToken, user);
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private GoogleUserInfo verifyIdToken(String idToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TOKENINFO_URL + idToken))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RunwayException(ErrorCode.INVALID_REQUEST, "유효하지 않은 Google 토큰입니다.");
            }

            JsonNode json = objectMapper.readTree(response.body());

            // iss 검증
            String iss = getField(json, "iss");
            if (!GOOGLE_ISS_1.equals(iss) && !GOOGLE_ISS_2.equals(iss)) {
                throw new RunwayException(ErrorCode.INVALID_REQUEST, "유효하지 않은 Google 토큰입니다.");
            }

            // aud 검증 — 이 앱에서 발급된 토큰인지 확인
            String aud = getField(json, "aud");
            if (!googleClientId.equals(aud)) {
                throw new RunwayException(ErrorCode.INVALID_REQUEST, "유효하지 않은 Google 토큰입니다.");
            }

            // email_verified 검증
            String emailVerified = json.has("email_verified") ? json.get("email_verified").asText() : "false";
            if (!"true".equals(emailVerified)) {
                throw new RunwayException(ErrorCode.INVALID_REQUEST, "이메일이 인증되지 않은 Google 계정입니다.");
            }

            // exp 검증 (Google tokeninfo가 이미 확인하지만 명시적 재확인)
            long exp = json.has("exp") ? json.get("exp").asLong() : 0L;
            if (exp < System.currentTimeMillis() / 1000) {
                throw new RunwayException(ErrorCode.INVALID_REQUEST, "만료된 Google 토큰입니다.");
            }

            String sub   = getField(json, "sub");
            String email = getField(json, "email");
            String name  = json.has("name") ? json.get("name").asText() : null;

            return new GoogleUserInfo(sub, email, name);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RunwayException(ErrorCode.INTERNAL_SERVER_ERROR, "Google 토큰 검증 중 오류가 발생했습니다.");
        } catch (IOException e) {
            throw new RunwayException(ErrorCode.INTERNAL_SERVER_ERROR, "Google 토큰 검증 중 오류가 발생했습니다.");
        }
    }

    private String getField(JsonNode json, String field) {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new RunwayException(ErrorCode.INVALID_REQUEST, "유효하지 않은 Google 토큰입니다.");
        }
        return json.get(field).asText();
    }

    private User findOrCreateUser(GoogleUserInfo info) {
        // 기존 소셜 계정 검색
        return socialAccountRepository.findByProviderAndProviderUserId("google", info.sub())
                .map(UserSocialAccount::getUser)
                .orElseGet(() -> {
                    User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(info.email())
                            .orElseGet(() -> {
                                String nickname = generateNickname(info);
                                return userRepository.save(User.builder()
                                        .email(info.email())
                                        .nickname(nickname)
                                        .build());
                            });
                    // 소셜 계정 연결 (없을 때만)
                    if (!socialAccountRepository.existsByUserAndProvider(user, "google")) {
                        socialAccountRepository.save(
                                UserSocialAccount.builder()
                                        .user(user).provider("google").providerUserId(info.sub())
                                        .build());
                    }
                    return user;
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
