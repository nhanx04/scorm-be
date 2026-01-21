package com.scorm.generator.repository;

import com.scorm.generator.entity.ScormActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScormActivityRepository extends JpaRepository<ScormActivity, Long> {
}

