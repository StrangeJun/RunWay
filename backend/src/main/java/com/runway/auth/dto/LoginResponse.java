package com.runway.auth.dto;

import com.runway.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UserSummary user;

    public static LoginResponse of(String accessToken, String refreshToken, User user) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(UserSummary.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .profileImageUrl(user.getProfileImageUrl())
                        .build())
                .build();
    }

    @Getter
    @Builder
    public static class UserSummary {
        private UUID userId;
        private String email;
        private String nickname;
        private String profileImageUrl;
    }
}
