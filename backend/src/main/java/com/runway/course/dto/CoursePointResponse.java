package com.runway.course.dto;

import com.runway.course.domain.CoursePoint;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CoursePointResponse {

    private Integer sequence;
    private Double latitude;
    private Double longitude;
    private Double altitudeMeters;

    public static CoursePointResponse from(CoursePoint point) {
        return CoursePointResponse.builder()
                .sequence(point.getSequence())
                .latitude(point.getLocation().getY())   // Y = latitude
                .longitude(point.getLocation().getX())  // X = longitude
                .altitudeMeters(point.getAltitudeMeters())
                .build();
    }
}
