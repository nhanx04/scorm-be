package com.scorm.generator.repository;

import com.scorm.generator.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByCourse_CourseIdOrderByOrderIndexAsc(Long courseId);
}

