package com.scorm.generator.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuizPageUpdateRequest {
    private BigDecimal passingScore;
    private Integer attemptAllowed;
}

