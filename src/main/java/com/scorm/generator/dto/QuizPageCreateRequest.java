package com.scorm.generator.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuizPageCreateRequest {
    private BigDecimal passingScore;
    private Integer attemptAllowed;
}

