package com.scorm.generator.dto;

import com.scorm.generator.entity.OrganizationResourceType;
import lombok.Data;

@Data
public class ShareResourceRequest {
    private OrganizationResourceType type;
    private Long mediaAssetId;
    private Long courseId;
    private Long folderId;
}

