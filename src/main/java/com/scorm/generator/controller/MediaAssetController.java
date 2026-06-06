package com.scorm.generator.controller;

import com.scorm.generator.dto.MediaUploadResponse;
import com.scorm.generator.service.MediaAssetService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping({ "/media", "/media-assets" })
public class MediaAssetController {

    private final MediaAssetService mediaAssetService;

    public MediaAssetController(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("libraryId") Long libraryId,
            Authentication authentication) {

        log.info("Received file upload: name='{}', size={} bytes, type='{}'",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        MediaUploadResponse response = mediaAssetService.uploadImage(
                file, title, description, libraryId, authentication);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> uploadFile(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        log.info("Received file upload: name='{}', size={} bytes, type='{}'",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Use a default libraryId for simple file uploads (e.g., avatar)
        MediaUploadResponse response = mediaAssetService.uploadImage(
                file, null, null, -1L, authentication);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/audios/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> uploadAudio(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("libraryId") Long libraryId,
            Authentication authentication) {
        return ResponseEntity.ok(mediaAssetService.uploadAudio(file, title, description, libraryId, authentication));
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("libraryId") Long libraryId,
            Authentication authentication) {
        return ResponseEntity.ok(mediaAssetService.uploadDocument(file, title, description, libraryId, authentication));
    }

    @PostMapping(value = "/videos")
    public ResponseEntity<MediaUploadResponse> createVideo(
            @RequestBody VideoCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(mediaAssetService.createVideo(
                request.title,
                request.description,
                request.libraryId,
                request.youtubeUrl,
                authentication));
    }

    @GetMapping({ "/me", "/assets/me" })
    public ResponseEntity<java.util.List<MediaUploadResponse>> getMyMediaAssets(Authentication authentication) {
        return ResponseEntity.ok(mediaAssetService.getMyMediaAssets(authentication));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteMedia(@PathVariable("mediaId") Long mediaId,
            Authentication authentication) {
        mediaAssetService.deleteMediaAsset(mediaId, authentication);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> bulkDeleteMedia(@RequestBody BulkDeleteMediaRequest request,
            Authentication authentication) {
        mediaAssetService.bulkDeleteMediaAssets(request.mediaIds, request.libraryId, authentication);
        return ResponseEntity.noContent().build();
    }

    public static class VideoCreateRequest {
        public String title;
        public String description;
        public Long libraryId;
        public String youtubeUrl;
    }

    public static class BulkDeleteMediaRequest {
        public Long libraryId;
        public java.util.List<Long> mediaIds;
    }
}
