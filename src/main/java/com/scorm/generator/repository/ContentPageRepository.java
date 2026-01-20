package com.scorm.generator.repository;

import com.scorm.generator.entity.ContentPage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentPageRepository extends JpaRepository<ContentPage, Long> {
}

