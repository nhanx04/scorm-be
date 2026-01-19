package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryDetailResponse {
    private Long libraryId;
    private String libraryName;
    private String description;
    private String scopeType;
    private OffsetDateTime updatedAt;
    private List<MediaAssetItem> assets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaAssetItem {
        private Long mediaId;
        private String title;
        private String description;
        private String originalFileName;
        private String mediaType;
        private OffsetDateTime uploadedAt;
        private OffsetDateTime updatedAt;
        private JsonNode metadata;
    }
}

