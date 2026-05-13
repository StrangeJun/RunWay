package com.runway.course.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class NearbyCourseItem {

    private UUID courseId;
    private String name;
    private String description;
    private Double distanceMeters;
    private Double distanceFromMeMeters;
    private Boolean isLoop;
    private Integer attemptCount;
    private Integer completionCount;
    private GeoPoint startPoint;
}
