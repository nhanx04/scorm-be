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
    private String layoutType;

    public static ContentPageResponse fromEntity(ContentPage contentPage) {
        return ContentPageResponse.builder()
                .pageId(contentPage.getPageId())
                .layoutType(contentPage.getLayoutType())
                .build();
    }
}

