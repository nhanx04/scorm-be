package com.scorm.generator.repository;

import com.scorm.generator.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
}

