package com.runway.course.repository;

import com.runway.course.domain.CoursePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CoursePointRepository extends JpaRepository<CoursePoint, UUID> {

    List<CoursePoint> findByCourseIdOrderBySequenceAsc(UUID courseId);

    void deleteByCourseId(UUID courseId);
}
