package com.scorm.generator.repository;

import com.scorm.generator.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PageRepository extends JpaRepository<Page, Long> {
    List<Page> findBySection_SectionIdOrderByOrderIndexAsc(Long sectionId);
}

