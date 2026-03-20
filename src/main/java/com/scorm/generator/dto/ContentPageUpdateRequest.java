package com.scorm.generator.dto;

import lombok.Data;

@Data
public class ContentPageUpdateRequest {
    private String layoutMode;
    private String layoutType;
    private com.fasterxml.jackson.databind.JsonNode layoutMeta;
}
