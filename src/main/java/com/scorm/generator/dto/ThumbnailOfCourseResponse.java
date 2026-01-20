package com.scorm.generator.dto;

import com.scorm.generator.entity.ThumbnailOfCourse;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ThumbnailOfCourseResponse {
    Long courseId;
    Long imageMediaId;

    public static ThumbnailOfCourseResponse fromEntity(ThumbnailOfCourse toc) {
        return ThumbnailOfCourseResponse.builder()
                .courseId(toc.getCourseId())
                .imageMediaId(toc.getImageAsset() != null ? toc.getImageAsset().getMediaId() : null)
                .build();
    }
}

