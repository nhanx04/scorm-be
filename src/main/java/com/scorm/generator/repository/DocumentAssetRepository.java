package com.scorm.generator.repository;

import com.scorm.generator.entity.DocumentAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAssetRepository extends JpaRepository<DocumentAsset, Long> {
}

