package com.runway.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerificationTokenResponse {
    private String verificationToken;
}
