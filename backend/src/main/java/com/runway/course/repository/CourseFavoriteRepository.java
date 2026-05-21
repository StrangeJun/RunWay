package com.runway.course.repository;

import com.runway.course.domain.CourseFavorite;
import com.runway.course.domain.CourseFavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseFavoriteRepository extends JpaRepository<CourseFavorite, CourseFavoriteId> {

    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);

    void deleteByUserIdAndCourseId(UUID userId, UUID courseId);
}
