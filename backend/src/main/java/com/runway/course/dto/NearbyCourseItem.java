package com.runway.course.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
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
    private Double avgRating;
    private Long ratingCount;
    private List<GeoPoint> routePoints;
}
