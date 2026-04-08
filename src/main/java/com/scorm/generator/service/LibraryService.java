package com.scorm.generator.service;

import com.scorm.generator.dto.LibraryDetailResponse;
import com.scorm.generator.dto.LibraryListItemResponse;
import com.scorm.generator.entity.MediaAsset;
import com.scorm.generator.entity.MyLibrary;
import com.scorm.generator.entity.User;
import com.scorm.generator.repository.MediaAssetRepository;
import com.scorm.generator.repository.MyLibraryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LibraryService {

    private final MyLibraryRepository myLibraryRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaAssetService mediaAssetService;

    public LibraryService(MyLibraryRepository myLibraryRepository,
            MediaAssetRepository mediaAssetRepository,
            MediaAssetService mediaAssetService) {
        this.myLibraryRepository = myLibraryRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.mediaAssetService = mediaAssetService;
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

    public List<LibraryListItemResponse> getAllLibraries() {
        return myLibraryRepository.findAll().stream()
                .map(l -> LibraryListItemResponse.builder()
                        .libraryId(l.getLibraryId())
                        .libraryName(l.getLibraryName())
                        .description(l.getDescription())
                        .scopeType(l.getScopeType())
                        .updatedAt(l.getUpdatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void deleteLibrary(Long libraryId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        MyLibrary library = getOwnedLibraryOrThrow(libraryId, currentUser);

        List<MediaAsset> assets = mediaAssetRepository.findByLibrary_LibraryIdOrderByUploadedAtDesc(libraryId);
        if (!assets.isEmpty()) {
            mediaAssetService.deleteAssetsInternal(assets);
        }

        myLibraryRepository.delete(library);
    }

    @Transactional
    public void bulkDeleteLibraries(List<Long> libraryIds, Authentication authentication) {
        if (libraryIds == null || libraryIds.isEmpty()) {
            throw new RuntimeException("libraryIds is required");
        }

        for (Long libraryId : libraryIds) {
            deleteLibrary(libraryId, authentication);
        }
    }

    public List<LibraryDetailResponse.MediaAssetItem> getLibraryAssets(Long libraryId) {
        if (libraryId == null) {
            throw new RuntimeException("libraryId is required");
        }

        if (!myLibraryRepository.existsById(libraryId)) {
            throw new RuntimeException("Library not found");
        }

        List<MediaAsset> assets = mediaAssetRepository.findByLibrary_LibraryIdOrderByUploadedAtDesc(libraryId);

        return assets.stream().map(a -> LibraryDetailResponse.MediaAssetItem.builder()
                .mediaId(a.getMediaId())
                .title(a.getTitle())
                .description(a.getDescription())
                .originalFileName(a.getOriginalFileName())
                .mediaType(a.getMediaType())
                .uploadedAt(a.getUploadedAt())
                .updatedAt(a.getUpdatedAt())
                .metadata(a.getMetadata())
                .build()).toList();
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

    private MyLibrary getOwnedLibraryOrThrow(Long libraryId, User currentUser) {
        if (libraryId == null) {
            throw new RuntimeException("libraryId is required");
        }

        MyLibrary library = myLibraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));

        if (!library.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("You do not have permission to delete this library");
        }

        return library;
    }
}
