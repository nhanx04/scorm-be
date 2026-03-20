package com.scorm.generator.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuizPageCreateRequest {
    private BigDecimal passingScore;
    private Integer attemptAllowed;
    private com.fasterxml.jackson.databind.JsonNode themeOverride;
    private String layoutMode;
    private com.fasterxml.jackson.databind.JsonNode layoutMeta;
    private com.fasterxml.jackson.databind.JsonNode templateData;
}
