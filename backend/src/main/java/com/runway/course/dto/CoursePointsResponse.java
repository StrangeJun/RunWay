package com.runway.course.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CoursePointsResponse {

    private UUID courseId;
    private List<CoursePointResponse> points;
}
