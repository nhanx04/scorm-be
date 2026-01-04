package com.scorm.generator.repository;

import com.scorm.generator.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    java.util.List<Image> findAllByOrderByCreatedAtDesc();
}
