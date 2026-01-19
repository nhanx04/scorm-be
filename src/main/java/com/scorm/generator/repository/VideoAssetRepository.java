package com.scorm.generator.repository;

import com.scorm.generator.entity.VideoAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoAssetRepository extends JpaRepository<VideoAsset, Long> {
}

