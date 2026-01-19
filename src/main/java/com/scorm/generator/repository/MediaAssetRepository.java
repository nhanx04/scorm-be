package com.scorm.generator.repository;

import com.scorm.generator.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    List<MediaAsset> findByLibrary_LibraryIdOrderByUploadedAtDesc(Long libraryId);
}
