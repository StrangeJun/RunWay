package com.runway.attempt.repository;

import com.runway.attempt.domain.CourseAttempt;
import com.runway.attempt.domain.enums.AttemptVerificationStatus;
import com.runway.attempt.domain.enums.CourseAttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CourseAttemptRepository extends JpaRepository<CourseAttempt, UUID> {

    Optional<CourseAttempt> findByIdAndUserId(UUID attemptId, UUID userId);

    Page<CourseAttempt> findByCourseIdAndUserIdOrderByStartedAtDesc(
            UUID courseId, UUID userId, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, CourseAttemptStatus status);

    Optional<CourseAttempt> findFirstByUserIdAndStatusOrderByCompletedAtAsc(
            UUID userId, CourseAttemptStatus status);

    long countByCourseIdAndUserIdAndStatusAndVerificationStatus(
            UUID courseId, UUID userId,
            CourseAttemptStatus status, AttemptVerificationStatus verificationStatus);

    /** 사용자의 해당 코스 완주 기록 중 최고 기록 (duration_seconds 최솟값) */
    @Query("SELECT MIN(a.durationSeconds) FROM CourseAttempt a " +
           "WHERE a.courseId = :courseId AND a.userId = :userId " +
           "AND a.status = com.runway.attempt.domain.enums.CourseAttemptStatus.COMPLETED")
    Optional<Integer> findMinDurationSecondsByCourseIdAndUserId(
            @Param("courseId") UUID courseId, @Param("userId") UUID userId);

    /** 사용자의 해당 코스 완주 횟수 */
    long countByCourseIdAndUserIdAndStatus(
            UUID courseId, UUID userId, CourseAttemptStatus status);
}
