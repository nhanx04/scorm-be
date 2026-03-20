package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.scorm.generator.entity.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse {
    private Long pageId;
    private String title;
    private Integer orderIndex;
    private String pageType;
    private String textHtml;
    private JsonNode themeOverride;
    private String layoutMode;
    private String layoutType;
    private JsonNode layoutMeta;
    private Long sectionId;

    public static PageResponse fromEntity(Page page) {
        return PageResponse.builder()
                .pageId(page.getPageId())
                .title(page.getTitle())
                .orderIndex(page.getOrderIndex())
                .pageType(page.getPageType())
                .textHtml(page.getTextHtml())
                .themeOverride(page.getThemeOverride())
                .layoutMode(page.getLayoutMode())
                .layoutType(page.getLayoutType())
                .layoutMeta(page.getLayoutMeta())
                .sectionId(page.getSection() != null ? page.getSection().getSectionId() : null)
                .build();
    }
}
