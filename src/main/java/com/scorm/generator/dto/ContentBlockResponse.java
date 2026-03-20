package com.scorm.generator.dto;

import com.scorm.generator.entity.ContentBlock;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContentBlockResponse {
    Long blockId;
    Integer orderIndex;
    String blockType;
    String textHtml;
    com.fasterxml.jackson.databind.JsonNode themeOverride;
    com.fasterxml.jackson.databind.JsonNode layoutMeta;
    Long contentPageId;

    public static ContentBlockResponse fromEntity(ContentBlock block) {
        return ContentBlockResponse.builder()
                .blockId(block.getBlockId())
                .orderIndex(block.getOrderIndex())
                .blockType(block.getBlockType())
                .textHtml(block.getTextHtml())
                .themeOverride(block.getThemeOverride())
                .layoutMeta(block.getLayoutMeta())
                .contentPageId(block.getContentPage() != null ? block.getContentPage().getPageId() : null)
                .build();
    }
}
