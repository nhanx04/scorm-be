package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class SectionCreateRequest {
    private String title;
    private String description;
    private Integer orderIndex;
    private String learningObjective;
    private String textHtml;
    private JsonNode themeOverride;
    private String layoutMode;
    private JsonNode layoutMeta;
}
