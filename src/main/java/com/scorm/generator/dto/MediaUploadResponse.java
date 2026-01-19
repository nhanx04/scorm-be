package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {
    private Long mediaId;
    private String title;
    private String description;
    private String originalFileName;
    private String mediaType;
    private OffsetDateTime uploadedAt;
    private JsonNode metadata;
}

