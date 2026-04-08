package com.scorm.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationFolderAssetsResponse {
    private Long orgId;
    private Long resourceId;
    private Long folderId;
    private String folderName;
    private List<LibraryDetailResponse.MediaAssetItem> items;
}

