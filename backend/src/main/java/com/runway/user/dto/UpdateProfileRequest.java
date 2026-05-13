package com.runway.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
    private String nickname;

    private String profileImageUrl;

    @Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    private String bio;
}
