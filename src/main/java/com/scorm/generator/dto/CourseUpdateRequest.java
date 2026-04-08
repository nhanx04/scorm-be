package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CourseUpdateRequest {
    private String title;
    private String description;
    private String coverImageUrl;
    @DecimalMin(value = "0.0", message = "passingScore must be >= 0")
    @DecimalMax(value = "100.0", message = "passingScore must be <= 100")
    private BigDecimal passingScore;
    @Min(value = 0, message = "attemptLimit must be >= 0")
    private Integer attemptLimit;
    @Min(value = 0, message = "durationMin must be >= 0")
    private Integer durationMin;
    private String status;
    private List<String> tags;
    private Boolean isFavorite;
    private String textHtml;
    private JsonNode themeOverride;
    private String layoutMode;
    private JsonNode layoutMeta;
    private JsonNode extraInfor;
    private JsonNode editorState;
    private String editorVersion;
    private String editorStatus;
}