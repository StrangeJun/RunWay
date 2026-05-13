package com.runway.attempt.domain;

import com.runway.attempt.domain.enums.AttemptVerificationStatus;
import com.runway.attempt.domain.enums.CourseAttemptStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "course_id", nullable = false, columnDefinition = "uuid")
    private UUID courseId;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    // running_records.id. SET NULL on run delete → nullable
    @Column(name = "running_record_id", columnDefinition = "uuid")
    private UUID runningRecordId;

    // CourseAttemptStatus.JpaConverter autoApply=true — @Convert 불필요
    @Column(nullable = false, length = 30)
    private CourseAttemptStatus status;

    // AttemptVerificationStatus.JpaConverter autoApply=true
    @Column(name = "verification_status", nullable = false, length = 30)
    private AttemptVerificationStatus verificationStatus;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private CourseAttempt(UUID courseId, UUID userId, UUID runningRecordId, Instant startedAt) {
        this.courseId = courseId;
        this.userId = userId;
        this.runningRecordId = runningRecordId;
        this.status = CourseAttemptStatus.IN_PROGRESS;
        this.verificationStatus = AttemptVerificationStatus.PENDING;
        this.startedAt = startedAt;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }

    // Phase 1: 완주 시 verification_status 를 verified 로 자동 설정 (동일 트랜잭션)
    public void complete(Instant completedAt, Integer durationSeconds, Double distanceMeters) {
        this.status = CourseAttemptStatus.COMPLETED;
        this.verificationStatus = AttemptVerificationStatus.VERIFIED;
        this.completedAt = completedAt;
        this.durationSeconds = durationSeconds;
        this.distanceMeters = distanceMeters;
    }

    // 포기 시 verification_status 는 PENDING 유지
    // REJECTED 는 Phase 2 GPS 검증 실패 시에만 사용
    public void abandon() {
        this.status = CourseAttemptStatus.ABANDONED;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.userId.equals(userId);
    }
}
