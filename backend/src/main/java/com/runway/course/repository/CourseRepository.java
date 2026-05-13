package com.runway.course.repository;

import com.runway.course.domain.Course;
import com.runway.course.domain.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByIdAndDeletedAtIsNull(UUID id);

    Page<Course> findByCreatorIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID creatorId, Pageable pageable);

    Page<Course> findByCreatorIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID creatorId, CourseStatus status, Pageable pageable);
}
