package com.runway.course.domain;

import com.runway.course.domain.enums.CourseReportReason;
import com.runway.course.domain.enums.CourseReportStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "course_id", nullable = false, columnDefinition = "uuid")
    private UUID courseId;

    @Column(name = "reporter_id", nullable = false, columnDefinition = "uuid")
    private UUID reporterId;

    @Column(nullable = false, length = 30)
    private CourseReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    private CourseReportStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public CourseReport(UUID courseId, UUID reporterId, CourseReportReason reason, String description) {
        this.courseId = courseId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.description = description;
        this.status = CourseReportStatus.PENDING;
        this.createdAt = Instant.now();
    }
}
