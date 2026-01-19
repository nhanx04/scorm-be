package com.scorm.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {
    private Long orgId;
    private String orgName;
    private String description;
    private Integer maxAuthors;
    private Long logoMediaId;
    private Long ownerId;
}

