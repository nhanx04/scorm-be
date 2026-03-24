package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseUpdateRequest {
    private String title;
    private String description;
    private String coverImageUrl;
    private BigDecimal passingScore;
    private Integer attemptLimit;
    private Integer durationMin;
    private String status;
    private String textHtml;
    private JsonNode themeOverride;
    private String layoutMode;
    private JsonNode layoutMeta;
    private JsonNode extraInfor;
    private JsonNode editorState;
    private String editorVersion;
    private String editorStatus;
}
