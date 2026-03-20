package com.scorm.generator.dto.Question;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionBaseDto {
    private Long questionId;
    private String title;
    private String instruction;
    private String promptHtml;
    private String textHtml;
    private String questionType;
    private JsonNode themeOverride;
    private String layoutMode;
    private JsonNode layoutMeta;
    private JsonNode templateData;
    private BigDecimal points;
    private Boolean shuffleOptions;
    private Boolean caseSensitive;
    private JsonNode extraConfig;
}
