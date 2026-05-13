package com.runway.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateCourseRequest {

    @NotBlank(message = "코스 이름은 필수입니다.")
    @Size(min = 2, max = 100, message = "코스 이름은 2~100자 사이여야 합니다.")
    private String name;

    @Size(max = 1000, message = "설명은 최대 1000자까지 입력할 수 있습니다.")
    private String description;

    private Boolean isLoop;
}
