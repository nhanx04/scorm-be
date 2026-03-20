package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.scorm.generator.entity.Section;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponse {
    private Long sectionId;
    private String title;
    private String description;
    private Integer orderIndex;
    private String learningObjective;
    private String textHtml;
    private JsonNode themeOverride;
    private String layoutMode;
    private JsonNode layoutMeta;
    private Long courseId;

    public static SectionResponse fromEntity(Section section) {
        return SectionResponse.builder()
                .sectionId(section.getSectionId())
                .title(section.getTitle())
                .description(section.getDescription())
                .orderIndex(section.getOrderIndex())
                .learningObjective(section.getLearningObjective())
                .textHtml(section.getTextHtml())
                .themeOverride(section.getThemeOverride())
                .layoutMode(section.getLayoutMode())
                .layoutMeta(section.getLayoutMeta())
                .courseId(section.getCourse() != null ? section.getCourse().getCourseId() : null)
                .build();
    }
}
