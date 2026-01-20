package com.scorm.generator.repository;

import com.scorm.generator.entity.ContentBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentBlockRepository extends JpaRepository<ContentBlock, Long> {
}

