package com.runway.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LogoutRequest {

    // Phase 1: 로그아웃은 SecurityContext의 인증 정보로 처리하므로
    // refreshToken은 수신만 하고 사용하지 않는다 (Phase 2 token rotation 대비)
    private String refreshToken;
}
