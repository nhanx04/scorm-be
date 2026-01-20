package com.scorm.generator.dto;

import lombok.Data;

@Data
public class ContentBlockCreateRequest {
    private Integer orderIndex;
    private String textHtml;
}

