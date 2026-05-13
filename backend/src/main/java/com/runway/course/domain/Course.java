package com.runway.course.domain;

import com.runway.course.domain.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "creator_id", nullable = false, columnDefinition = "uuid")
    private UUID creatorId;

    @Column(name = "source_record_id", columnDefinition = "uuid")
    private UUID sourceRecordId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // CourseStatus.JpaConverter autoApply=true — @Convert 불필요
    @Column(nullable = false, length = 30)
    private CourseStatus status;

    @Column(name = "distance_meters", nullable = false)
    private Double distanceMeters;

    @Column(name = "is_loop", nullable = false)
    private Boolean isLoop;

    @Column(name = "start_location", columnDefinition = "geography(Point,4326)", nullable = false)
    private Point startLocation;

    @Column(name = "end_location", columnDefinition = "geography(Point,4326)", nullable = false)
    private Point endLocation;

    @Column(columnDefinition = "geography(LineString,4326)")
    private LineString path;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "completion_count", nullable = false)
    private Integer completionCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder
    private Course(UUID creatorId, UUID sourceRecordId, String name, String description,
                   CourseStatus status, Double distanceMeters, Boolean isLoop,
                   Point startLocation, Point endLocation) {
        this.creatorId = creatorId;
        this.sourceRecordId = sourceRecordId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.distanceMeters = distanceMeters != null ? distanceMeters : 0.0;
        this.isLoop = Boolean.TRUE.equals(isLoop);
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.attemptCount = 0;
        this.completionCount = 0;
    }

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateInfo(String name, String description, Boolean isLoop) {
        this.name = name;
        this.description = description;
        this.isLoop = Boolean.TRUE.equals(isLoop);
    }

    public void updatePath(LineString path) {
        this.path = path;
    }

    public void publish() {
        this.status = CourseStatus.PUBLISHED;
    }

    public void archive() {
        this.status = CourseStatus.ARCHIVED;
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
    }

    public void incrementCompletionCount() {
        this.completionCount++;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.creatorId.equals(userId);
    }

    public boolean isVisibleTo(UUID userId) {
        return this.status == CourseStatus.PUBLISHED || this.creatorId.equals(userId);
    }
}
