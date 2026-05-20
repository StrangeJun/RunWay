package com.runway.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CourseReportRequest {

    @NotBlank
    private String reason;

    private String description;
}
