package com.scorm.generator.dto.Question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSummaryDto {
    private Long questionId;
    private String title;
    private String instruction;
    private String promptHtml;
    private String textHtml;
    private String questionType;
    private com.fasterxml.jackson.databind.JsonNode themeOverride;
    private String layoutMode;
    private com.fasterxml.jackson.databind.JsonNode layoutMeta;
    private com.fasterxml.jackson.databind.JsonNode templateData;
    private java.math.BigDecimal points;
    private Boolean shuffleOptions;
    private Boolean caseSensitive;
    private com.fasterxml.jackson.databind.JsonNode extraConfig;
}
