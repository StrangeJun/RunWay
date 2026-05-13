package com.runway.course.dto;

import com.runway.course.domain.Course;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CourseStatusResponse {

    private UUID courseId;
    private String status;

    public static CourseStatusResponse from(Course course) {
        return CourseStatusResponse.builder()
                .courseId(course.getId())
                .status(course.getStatus().getDbValue())
                .build();
    }
}
