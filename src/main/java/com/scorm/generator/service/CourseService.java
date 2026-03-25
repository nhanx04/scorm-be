package com.scorm.generator.service;

import com.scorm.generator.dto.CourseCreateRequest;
import com.scorm.generator.dto.CourseDetailResponse;
import com.scorm.generator.dto.CourseResponse;
import com.scorm.generator.dto.CourseUpdateRequest;
import com.scorm.generator.dto.AI.AiCourseOutline;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface CourseService {
    CourseResponse create(CourseCreateRequest request, Authentication authentication);

    List<CourseResponse> listMine(Authentication authentication);

    CourseDetailResponse getById(Long courseId, Authentication authentication);

    CourseResponse update(Long courseId, CourseUpdateRequest request, Authentication authentication);

    void delete(Long courseId, Authentication authentication);

    CourseResponse saveAiCourseOutline(AiCourseOutline outline, Authentication authentication);
}
