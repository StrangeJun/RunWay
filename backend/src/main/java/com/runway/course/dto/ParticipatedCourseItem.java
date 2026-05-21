package com.runway.course.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ParticipatedCourseItem {

    private UUID courseId;
    private String name;
    private String description;
    private Double distanceMeters;
    private Boolean isLoop;
    private String status;
    private Integer attemptCountByMe;
    private Integer completionCountByMe;
    private Integer bestTimeSecondsByMe;
    private Instant lastAttemptAt;
}
