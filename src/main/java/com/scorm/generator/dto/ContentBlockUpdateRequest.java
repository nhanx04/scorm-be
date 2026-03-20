package com.scorm.generator.dto;

import lombok.Data;

@Data
public class ContentBlockUpdateRequest {
    private Integer orderIndex;
    private String blockType;
    private String textHtml;
    private com.fasterxml.jackson.databind.JsonNode themeOverride;
    private com.fasterxml.jackson.databind.JsonNode layoutMeta;
}
