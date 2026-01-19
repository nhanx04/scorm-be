package com.scorm.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scorm.generator.dto.MediaUploadResponse;
import com.scorm.generator.entity.*;
import com.scorm.generator.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class MediaAssetService {

    private final MediaAssetRepository mediaAssetRepository;
    private final ImageAssetRepository imageAssetRepository;
    private final AudioAssetRepository audioAssetRepository;
    private final DocumentAssetRepository documentAssetRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final MyLibraryRepository myLibraryRepository;
    private final S3StorageService storageService;
    private final ObjectMapper objectMapper;

    private final String publicBaseUrl;
    private final String bucket;
    private final String endpoint;

    public MediaAssetService(
            MediaAssetRepository mediaAssetRepository,
            ImageAssetRepository imageAssetRepository,
            AudioAssetRepository audioAssetRepository,
            DocumentAssetRepository documentAssetRepository,
            VideoAssetRepository videoAssetRepository,
            MyLibraryRepository myLibraryRepository,
            S3StorageService storageService,
            ObjectMapper objectMapper,
            @Value("${app.r2.public-base-url}") String publicBaseUrl,
            @Value("${app.r2.bucket}") String bucket,
            @Value("${app.r2.endpoint}") String endpoint) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.imageAssetRepository = imageAssetRepository;
        this.audioAssetRepository = audioAssetRepository;
        this.documentAssetRepository = documentAssetRepository;
        this.videoAssetRepository = videoAssetRepository;
        this.myLibraryRepository = myLibraryRepository;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.publicBaseUrl = publicBaseUrl;
        this.bucket = bucket;
        this.endpoint = endpoint;
    }

    public MediaUploadResponse uploadImage(MultipartFile file, String title, String description, Long libraryId,
            Authentication authentication) {
        return uploadFileAsset("IMAGE", "images", file, title, description, libraryId, authentication);
    }

    public MediaUploadResponse uploadAudio(MultipartFile file, String title, String description, Long libraryId,
            Authentication authentication) {
        return uploadFileAsset("AUDIO", "audios", file, title, description, libraryId, authentication);
    }

    public MediaUploadResponse uploadDocument(MultipartFile file, String title, String description, Long libraryId,
            Authentication authentication) {
        return uploadFileAsset("DOCUMENT", "documents", file, title, description, libraryId, authentication);
    }

    public MediaUploadResponse createVideo(String title, String description, Long libraryId, String youtubeUrl,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        if (youtubeUrl == null || youtubeUrl.isBlank()) {
            throw new RuntimeException("youtubeUrl is required");
        }

        MyLibrary library = myLibraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));

        MediaAsset media = MediaAsset.builder()
                .title(title)
                .description(description)
                .originalFileName(null)
                .mediaType("VIDEO")
                .library(library)
                .user(currentUser)
                .metadata(buildVideoMetadata(youtubeUrl))
                .build();

        MediaAsset saved = mediaAssetRepository.save(media);

        VideoAsset videoAsset = VideoAsset.builder()
                .mediaId(saved.getMediaId())
                .mediaAsset(saved)
                .youtubeUrl(youtubeUrl)
                .build();
        videoAssetRepository.save(videoAsset);

        return toResponse(saved);
    }

    private MediaUploadResponse uploadFileAsset(
            String mediaType,
            String prefix,
            MultipartFile file,
            String title,
            String description,
            Long libraryId,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("file is required");
        }
        if (libraryId == null) {
            throw new RuntimeException("libraryId is required");
        }

        MyLibrary library = myLibraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));

        String key = storageService.uploadFile(prefix, file);

        MediaAsset media = MediaAsset.builder()
                .title(title)
                .description(description)
                .originalFileName(file.getOriginalFilename())
                .mediaType(mediaType)
                .library(library)
                .user(currentUser)
                .metadata(buildStorageMetadata(key, file, Map.of()))
                .build();

        MediaAsset saved = mediaAssetRepository.save(media);

        if ("IMAGE".equals(mediaType)) {
            imageAssetRepository
                    .save(ImageAsset.builder().mediaAsset(saved).metadata(null).build());
        } else if ("AUDIO".equals(mediaType)) {
            audioAssetRepository
                    .save(AudioAsset.builder().mediaAsset(saved).metadata(null).build());
        } else if ("DOCUMENT".equals(mediaType)) {
            documentAssetRepository
                    .save(DocumentAsset.builder().mediaAsset(saved).metadata(null).build());
        }

        return toResponse(saved);
    }

    private ObjectNode buildStorageMetadata(String key, MultipartFile file, Map<String, Object> extra) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("provider", "CLOUDFLARE_R2");
        node.put("bucket", bucket);
        node.put("endpoint", endpoint);
        node.put("publicBaseUrl", publicBaseUrl);
        node.put("key", key);
        node.put("publicUrl", publicBaseUrl + "/" + key);

        if (file != null) {
            if (file.getContentType() != null) {
                node.put("contentType", file.getContentType());
            }
            node.put("size", file.getSize());
            if (file.getOriginalFilename() != null) {
                node.put("originalFileName", file.getOriginalFilename());
            }
        }

        extra.forEach((k, v) -> {
            JsonNode value = objectMapper.valueToTree(v);
            node.set(k, value);
        });

        return node;
    }

    private ObjectNode buildVideoMetadata(String youtubeUrl) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("provider", "YOUTUBE");
        node.put("youtubeUrl", youtubeUrl);
        return node;
    }

    private MediaUploadResponse toResponse(MediaAsset media) {
        return MediaUploadResponse.builder()
                .mediaId(media.getMediaId())
                .title(media.getTitle())
                .description(media.getDescription())
                .originalFileName(media.getOriginalFileName())
                .mediaType(media.getMediaType())
                .uploadedAt(media.getUploadedAt())
                .metadata(media.getMetadata())
                .build();
    }
}
