package com.scorm.generator.dto;

import com.scorm.generator.entity.OrganizationActivityAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationActivityResponse {
    private Long id;
    private OrganizationActivityAction action;
    private Long targetId;
    private Long userId;
    private String userName;
    private OffsetDateTime createdAt;
}

