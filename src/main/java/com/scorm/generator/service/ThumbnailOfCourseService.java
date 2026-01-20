package com.scorm.generator.service;

import com.scorm.generator.dto.ThumbnailOfCourseResponse;
import com.scorm.generator.dto.ThumbnailOfCourseUpsertRequest;
import org.springframework.security.core.Authentication;

public interface ThumbnailOfCourseService {
    ThumbnailOfCourseResponse upsert(Long courseId, ThumbnailOfCourseUpsertRequest request, Authentication authentication);

    ThumbnailOfCourseResponse getByCourseId(Long courseId, Authentication authentication);

    void delete(Long courseId, Authentication authentication);
}

