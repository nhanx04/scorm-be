package com.scorm.generator.repository;

import com.scorm.generator.entity.ScormResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScormResourceRepository extends JpaRepository<ScormResource, Long> {
}

