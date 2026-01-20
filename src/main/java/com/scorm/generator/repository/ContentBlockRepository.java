package com.scorm.generator.repository;

import com.scorm.generator.entity.ContentBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentBlockRepository extends JpaRepository<ContentBlock, Long> {
    List<ContentBlock> findByContentPage_PageIdOrderByOrderIndexAsc(Long pageId);
}
