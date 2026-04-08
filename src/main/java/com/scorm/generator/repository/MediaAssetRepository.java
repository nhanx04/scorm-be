package com.scorm.generator.repository;

import com.scorm.generator.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    List<MediaAsset> findByUser_UserIdOrderByUploadedAtDesc(Long userId);

    List<MediaAsset> findByLibrary_LibraryIdOrderByUploadedAtDesc(Long libraryId);

    List<MediaAsset> findByMediaIdInAndLibrary_LibraryId(List<Long> mediaIds, Long libraryId);

    long countByLibrary_LibraryId(Long libraryId);
}
