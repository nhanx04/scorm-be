package com.scorm.generator.dto;

import com.scorm.generator.entity.ContentPage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentPageResponse {
    private Long pageId;
    private String layoutMode;
    private String layoutType;
    private com.fasterxml.jackson.databind.JsonNode layoutMeta;

    public static ContentPageResponse fromEntity(ContentPage contentPage) {
        return ContentPageResponse.builder()
                .pageId(contentPage.getPageId())
                .layoutMode(contentPage.getLayoutMode())
                .layoutType(contentPage.getLayoutType())
                .layoutMeta(contentPage.getLayoutMeta())
                .build();
    }
}
