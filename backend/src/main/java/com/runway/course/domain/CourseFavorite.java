package com.runway.course.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_favorites")
@IdClass(CourseFavoriteId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseFavorite {

    @Id
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Id
    @Column(name = "course_id", nullable = false, columnDefinition = "uuid")
    private UUID courseId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private CourseFavorite(UUID userId, UUID courseId) {
        this.userId = userId;
        this.courseId = courseId;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }
}
