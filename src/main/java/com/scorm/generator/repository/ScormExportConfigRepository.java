package com.scorm.generator.repository;

import com.scorm.generator.entity.ScormExportConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScormExportConfigRepository extends JpaRepository<ScormExportConfig, Long> {
    Optional<ScormExportConfig> findByCourse_CourseId(Long courseId);
}

