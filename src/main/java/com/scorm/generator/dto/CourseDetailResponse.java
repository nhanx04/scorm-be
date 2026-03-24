package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
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
public class CourseDetailResponse {
    private Long courseId;
    private String title;
    private String description;
    private String coverImageUrl;
    private BigDecimal passingScore;
    private Integer attemptLimit;
    private Integer durationMin;
    private String status;
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

    private ThumbnailOfCourseResponse thumbnail;

    private List<SectionDetailDto> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionDetailDto {
        private Long sectionId;
        private String title;
        private String description;
        private Integer orderIndex;
        private String learningObjective;
        private String textHtml;
        private JsonNode themeOverride;
        private String layoutMode;
        private JsonNode layoutMeta;

        private List<PageDetailDto> pages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageDetailDto {
        private Long pageId;
        private String title;
        private Integer orderIndex;
        private String pageType;
        private String textHtml;
        private JsonNode themeOverride;
        private String layoutMode;
        private String layoutType;
        private JsonNode layoutMeta;

        private ContentPageDetailDto contentPage;
        private QuizPageResponse quizPage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentPageDetailDto {
        private Long pageId;
        private String layoutMode;
        private String layoutType;
        private JsonNode layoutMeta;

        private List<ContentBlockResponse> blocks;
    }
}
