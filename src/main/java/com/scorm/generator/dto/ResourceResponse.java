package com.scorm.generator.dto;

import com.scorm.generator.entity.OrganizationResourceType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ResourceResponse {
    private Long id;
    private OrganizationResourceType type;
    private String name;
    private String thumbnail;
    private String instructor;
    private Integer folderItemCount;
    private Long mediaAssetId;
    private Long courseId;
    private Long folderId;
    private Long sharedBy;
    private String sharedByName;
    private OffsetDateTime createdAt;
}
