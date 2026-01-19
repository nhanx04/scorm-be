package com.scorm.generator.service;

import com.scorm.generator.dto.LibraryDetailResponse;
import com.scorm.generator.entity.MediaAsset;
import com.scorm.generator.entity.MyLibrary;
import com.scorm.generator.entity.User;
import com.scorm.generator.repository.MediaAssetRepository;
import com.scorm.generator.repository.MyLibraryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibraryService {

    private final MyLibraryRepository myLibraryRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public LibraryService(MyLibraryRepository myLibraryRepository, MediaAssetRepository mediaAssetRepository) {
        this.myLibraryRepository = myLibraryRepository;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    public MyLibrary create(String name, String description, String scopeType, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        if (name == null || name.isBlank()) {
            throw new RuntimeException("libraryName is required");
        }

        MyLibrary library = MyLibrary.builder()
                .libraryName(name)
                .description(description)
                .scopeType(scopeType == null || scopeType.isBlank() ? "PRIVATE" : scopeType)
                .owner(currentUser)
                .build();

        return myLibraryRepository.save(library);
    }

    public LibraryDetailResponse getLibraryDetail(Long libraryId) {
        if (libraryId == null) {
            throw new RuntimeException("libraryId is required");
        }

        MyLibrary library = myLibraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));

        List<MediaAsset> assets = mediaAssetRepository.findByLibrary_LibraryIdOrderByUploadedAtDesc(libraryId);

        return LibraryDetailResponse.builder()
                .libraryId(library.getLibraryId())
                .libraryName(library.getLibraryName())
                .description(library.getDescription())
                .scopeType(library.getScopeType())
                .updatedAt(library.getUpdatedAt())
                .assets(assets.stream().map(a -> LibraryDetailResponse.MediaAssetItem.builder()
                        .mediaId(a.getMediaId())
                        .title(a.getTitle())
                        .description(a.getDescription())
                        .originalFileName(a.getOriginalFileName())
                        .mediaType(a.getMediaType())
                        .uploadedAt(a.getUploadedAt())
                        .updatedAt(a.getUpdatedAt())
                        .metadata(a.getMetadata())
                        .build()).toList())
                .build();
    }
}
