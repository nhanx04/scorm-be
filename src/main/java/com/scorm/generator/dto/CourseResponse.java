package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.scorm.generator.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
    private Long courseId;
    private String title;
    private BigDecimal passingScore;
    private Integer attemptLimit;
    private Integer durationMin;
    private String status;
    private OffsetDateTime lastPublishedAt;
    private OffsetDateTime updatedAt;
    private JsonNode extraInfor;
    private OffsetDateTime createdAt;
    private Long courseUserId;

    public static CourseResponse fromEntity(Course course, JsonNode extraInfor) {
        return CourseResponse.builder()
                .courseId(course.getCourseId())
                .title(course.getTitle())
                .passingScore(course.getPassingScore())
                .attemptLimit(course.getAttemptLimit())
                .durationMin(course.getDurationMin())
                .status(course.getStatus())
                .lastPublishedAt(course.getLastPublishedAt())
                .updatedAt(course.getUpdatedAt())
                .extraInfor(extraInfor)
                .createdAt(course.getCreatedAt())
                .courseUserId(course.getUser() != null ? course.getUser().getUserId() : null)
                .build();
    }
}

