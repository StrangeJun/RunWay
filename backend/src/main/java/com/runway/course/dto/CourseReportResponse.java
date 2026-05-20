package com.runway.course.dto;

import com.runway.course.domain.CourseReport;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CourseReportResponse {

    private UUID reportId;
    private UUID courseId;
    private String status;
    private Instant createdAt;

    public static CourseReportResponse from(CourseReport report) {
        return CourseReportResponse.builder()
                .reportId(report.getId())
                .courseId(report.getCourseId())
                .status(report.getStatus().toDbValue())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
