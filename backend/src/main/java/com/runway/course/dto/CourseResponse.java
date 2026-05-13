package com.runway.course.dto;

import com.runway.course.domain.Course;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CourseResponse {

    private UUID courseId;
    private String name;
    private String description;
    private String status;
    private Double distanceMeters;
    private Boolean isLoop;
    private GeoPoint startPoint;
    private GeoPoint endPoint;
    private Integer attemptCount;
    private Integer completionCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static CourseResponse from(Course course) {
        return CourseResponse.builder()
                .courseId(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .status(course.getStatus().getDbValue())
                .distanceMeters(course.getDistanceMeters())
                .isLoop(course.getIsLoop())
                .startPoint(new GeoPoint(
                        course.getStartLocation().getY(),  // Y = latitude
                        course.getStartLocation().getX())) // X = longitude
                .endPoint(new GeoPoint(
                        course.getEndLocation().getY(),
                        course.getEndLocation().getX()))
                .attemptCount(course.getAttemptCount())
                .completionCount(course.getCompletionCount())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
