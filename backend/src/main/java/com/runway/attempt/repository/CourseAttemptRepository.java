package com.runway.attempt.repository;

import com.runway.attempt.domain.CourseAttempt;
import com.runway.attempt.domain.enums.CourseAttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CourseAttemptRepository extends JpaRepository<CourseAttempt, UUID> {

    Optional<CourseAttempt> findByIdAndUserId(UUID attemptId, UUID userId);

    Page<CourseAttempt> findByCourseIdAndUserIdOrderByStartedAtDesc(
            UUID courseId, UUID userId, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, CourseAttemptStatus status);

    Optional<CourseAttempt> findFirstByUserIdAndStatusOrderByCompletedAtAsc(
            UUID userId, CourseAttemptStatus status);
}
