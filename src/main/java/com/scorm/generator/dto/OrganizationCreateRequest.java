package com.scorm.generator.dto;

import lombok.Data;

@Data
public class OrganizationCreateRequest {
    private String orgName;
    private String description;
    private Integer maxAuthors;
    private Long logoMediaId;
}

