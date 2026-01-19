package com.scorm.generator.repository;

import com.scorm.generator.entity.AudioAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioAssetRepository extends JpaRepository<AudioAsset, Long> {
}

