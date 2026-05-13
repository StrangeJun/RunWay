package com.runway.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReissueResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;

    public static ReissueResponse of(String accessToken, String refreshToken) {
        return ReissueResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}
