package com.scorm.generator.dto;

import lombok.Data;

@Data
public class ContentBlockUpdateRequest {
    private Integer orderIndex;
    private String textHtml;
}

