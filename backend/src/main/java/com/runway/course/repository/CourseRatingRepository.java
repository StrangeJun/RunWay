package com.runway.course.repository;

import com.runway.course.domain.CourseRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CourseRatingRepository extends JpaRepository<CourseRating, UUID> {

    Optional<CourseRating> findByCourseIdAndUserId(UUID courseId, UUID userId);

    long countByCourseId(UUID courseId);

    @Query("SELECT AVG(r.rating) FROM CourseRating r WHERE r.courseId = :courseId")
    Double findAvgRatingByCourseId(@Param("courseId") UUID courseId);
}
