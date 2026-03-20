package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class PageUpdateRequest {
    private String title;
    private Integer orderIndex;
    private String pageType;
    private String textHtml;
    private JsonNode themeOverride;
    private String layoutMode;
    private String layoutType;
    private JsonNode layoutMeta;
}
