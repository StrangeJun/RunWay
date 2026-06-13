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
public class KakaoAuthService {

    private static final String KAKAO_TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info";
    private static final String KAKAO_USER_ME_URL    = "https://kapi.kakao.com/v2/user/me";

    @Value("${social.kakao.app-id}")
    private long kakaoAppId;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Transactional
    public LoginResponse loginWithKakao(String accessToken) {
        // 1) 토큰이 RunWay 앱에서 발급됐는지 app_id 검증
        verifyTokenAppId(accessToken);
        // 2) 사용자 정보 조회
        KakaoUserInfo info = fetchUserInfo(accessToken);
        User user = findOrCreateUser(info);

        String newAccessToken  = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getCredentialVersion());
        String newRefreshToken = jwtProvider.generateRefreshToken(
                user.getId(), user.getEmail(), user.getCredentialVersion());
        user.updateRefreshTokenHash(jwtProvider.hashToken(newRefreshToken));

        log.info("Kakao login: userId={}", user.getId());
        return LoginResponse.of(newAccessToken, newRefreshToken, user);
    }

    // ── app_id 검증 ───────────────────────────────────────────────────────────

    private void verifyTokenAppId(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(KAKAO_TOKEN_INFO_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RunwayException(ErrorCode.INVALID_REQUEST, "유효하지 않은 카카오 토큰입니다.");
            }

            JsonNode json = objectMapper.readTree(response.body());
            long appId = json.path("app_id").asLong(-1L);
            if (appId != kakaoAppId) {
                throw new RunwayException(ErrorCode.INVALID_REQUEST, "유효하지 않은 카카오 토큰입니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RunwayException(ErrorCode.INTERNAL_SERVER_ERROR, "카카오 토큰 검증 중 오류가 발생했습니다.");
        } catch (IOException e) {
            throw new RunwayException(ErrorCode.INTERNAL_SERVER_ERROR, "카카오 토큰 검증 중 오류가 발생했습니다.");
        }
    }

    // ── 사용자 정보 조회 ──────────────────────────────────────────────────────

    private KakaoUserInfo fetchUserInfo(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(KAKAO_USER_ME_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RunwayException(ErrorCode.INVALID_REQUEST, "유효하지 않은 카카오 토큰입니다.");
            }

            JsonNode json    = objectMapper.readTree(response.body());
            String kakaoId   = json.get("id").asText();
            JsonNode account = json.path("kakao_account");

            // 이메일: is_email_valid + is_email_verified 모두 true여야 신뢰할 수 있는 이메일
            String email = null;
            if (account.has("email")) {
                boolean valid    = account.path("is_email_valid").asBoolean(false);
                boolean verified = account.path("is_email_verified").asBoolean(false);
                if (valid && verified) {
                    email = account.get("email").asText();
                }
            }

            String nickname = account.path("profile").has("nickname")
                    ? account.path("profile").get("nickname").asText() : null;

            return new KakaoUserInfo(kakaoId, email, nickname);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RunwayException(ErrorCode.INTERNAL_SERVER_ERROR, "카카오 사용자 정보 조회 중 오류가 발생했습니다.");
        } catch (IOException e) {
            throw new RunwayException(ErrorCode.INTERNAL_SERVER_ERROR, "카카오 사용자 정보 조회 중 오류가 발생했습니다.");
        }
    }

    // ── 사용자 조회 / 자동 생성 ───────────────────────────────────────────────

    private User findOrCreateUser(KakaoUserInfo info) {
        return socialAccountRepository.findByProviderAndProviderUserId("kakao", info.kakaoId())
                .map(UserSocialAccount::getUser)
                .orElseGet(() -> {
                    // 검증된 이메일인 경우에만 기존 계정 탐색 (미검증 이메일로 계정 탈취 방지)
                    User user = (info.email() != null)
                            ? userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(info.email())
                                    .orElseGet(() -> createNewUser(info))
                            : createNewUser(info);

                    if (!socialAccountRepository.existsByUserAndProvider(user, "kakao")) {
                        socialAccountRepository.save(
                                UserSocialAccount.builder()
                                        .user(user).provider("kakao").providerUserId(info.kakaoId())
                                        .build());
                    }
                    return user;
                });
    }

    private User createNewUser(KakaoUserInfo info) {
        String email    = info.email() != null ? info.email()
                : "kakao_" + info.kakaoId() + "@kakao.runway";
        String nickname = generateNickname(info.nickname() != null ? info.nickname() : "Runner");
        return userRepository.save(User.builder()
                .email(email)
                .nickname(nickname)
                .build());
    }

    private String generateNickname(String base) {
        String clean     = base.replaceAll("\\s+", "");
        String candidate = clean.substring(0, Math.min(clean.length(), 15));
        int suffix = 1;
        while (userRepository.existsByNicknameAndDeletedAtIsNull(candidate)) {
            candidate = clean.substring(0, Math.min(clean.length(), 13)) + suffix++;
        }
        return candidate;
    }

    private record KakaoUserInfo(String kakaoId, String email, String nickname) {}
}
