package com.runway.course.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Entity
@Table(name = "course_points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "course_id", nullable = false, columnDefinition = "uuid")
    private UUID courseId;

    @Column(nullable = false)
    private Integer sequence;

    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    private Point location;

    @Column(name = "altitude_meters")
    private Double altitudeMeters;

    @Builder
    private CoursePoint(UUID courseId, Integer sequence, Point location, Double altitudeMeters) {
        this.courseId = courseId;
        this.sequence = sequence;
        this.location = location;
        this.altitudeMeters = altitudeMeters;
    }
}
