package com.runway.auth.dto;

import com.runway.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class SignupResponse {

    private UUID userId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String bio;
    private Instant createdAt;

    public static SignupResponse from(User user) {
        return SignupResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
