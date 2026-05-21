package com.runway.course.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class PublishCourseRequest {

    @NotNull(message = "난이도는 필수입니다.")
    private String difficulty;

    @NotNull(message = "경사도는 필수입니다.")
    private String slopeLevel;

    @NotNull(message = "위험도는 필수입니다.")
    private String riskLevel;

    @NotNull(message = "노면 유형은 필수입니다.")
    private String surfaceType;

    @NotNull(message = "추천 시간대는 필수입니다.")
    private String recommendedTime;

    @Size(max = 1000, message = "주의사항은 최대 1000자까지 입력할 수 있습니다.")
    private String warnings;

    @Size(max = 1000, message = "설명은 최대 1000자까지 입력할 수 있습니다.")
    private String description;
}
