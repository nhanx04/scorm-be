package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.scorm.generator.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
    private Long courseId;
    private String title;
    private String description;
    private String coverImageUrl;
    private BigDecimal passingScore;
    private Integer attemptLimit;
    private Integer durationMin;
    private String status;

    // Thêm thuộc tính tags
    private List<String> tags;

    // Thêm thuộc tính isFavorite
    private Boolean isFavorite;

    private String textHtml;
    private JsonNode themeOverride;
    private String layoutMode;
    private JsonNode layoutMeta;
    private OffsetDateTime lastPublishedAt;
    private OffsetDateTime updatedAt;
    private JsonNode extraInfor;
    private JsonNode editorState;
    private String editorVersion;
    private String editorStatus;
    private OffsetDateTime createdAt;
    private Long courseUserId;

    public static CourseResponse fromEntity(Course course, JsonNode extraInfor) {
        return CourseResponse.builder()
                .courseId(course.getCourseId())
                .title(course.getTitle())
                .description(course.getDescription())
                .coverImageUrl(course.getCoverImageUrl())
                .passingScore(course.getPassingScore())
                .attemptLimit(course.getAttemptLimit())
                .durationMin(course.getDurationMin())
                .status(course.getStatus())
                .tags(course.getTags())
                .isFavorite(course.getIsFavorite())
                .textHtml(course.getTextHtml())
                .themeOverride(course.getThemeOverride())
                .layoutMode(course.getLayoutMode())
                .layoutMeta(course.getLayoutMeta())
                .lastPublishedAt(course.getLastPublishedAt())
                .updatedAt(course.getUpdatedAt())
                .extraInfor(extraInfor)
                .editorState(course.getEditorState())
                .editorVersion(course.getEditorVersion())
                .editorStatus(course.getEditorStatus())
                .createdAt(course.getCreatedAt())
                .courseUserId(course.getUser() != null ? course.getUser().getUserId() : null)
                .build();
    }
}