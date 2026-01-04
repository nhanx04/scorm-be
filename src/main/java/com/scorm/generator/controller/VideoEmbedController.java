package com.scorm.generator.controller;

import com.scorm.generator.dto.SaveVideoEmbedRequest;
import com.scorm.generator.dto.VideoEmbedResourceDTO;
import com.scorm.generator.entity.VideoEmbed;
import com.scorm.generator.service.VideoEmbedStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class VideoEmbedController {

    private final VideoEmbedStoreService videoEmbedStoreService;

    /**
     * Lưu link nhúng video như 1 tài nguyên media (persist DB).
     * - Input: url (watch/share url)
     * - Output: {id, embedUrl, createdAt, updatedAt}
     */
    @PostMapping("/video-embed")
    public ResponseEntity<VideoEmbedResourceDTO> create(@RequestBody SaveVideoEmbedRequest request) {
        VideoEmbed entity = videoEmbedStoreService.create(request.getUrl(), request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(entity));
    }

    @GetMapping("/video-embeds")
    public ResponseEntity<java.util.List<VideoEmbedResourceDTO>> getAll() {
        java.util.List<VideoEmbed> videos = videoEmbedStoreService.findAll();
        java.util.List<VideoEmbedResourceDTO> dtos = videos.stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/video-embed/{id}")
    public ResponseEntity<VideoEmbedResourceDTO> getById(@PathVariable Long id) {
        VideoEmbed entity = videoEmbedStoreService.getById(id);
        return ResponseEntity.ok(toDto(entity));
    }

    @DeleteMapping("/video-embed/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        videoEmbedStoreService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private VideoEmbedResourceDTO toDto(VideoEmbed entity) {
        return VideoEmbedResourceDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .embedUrl(entity.getEmbedUrl())
                .thumbnailUrl(entity.getThumbnailUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
