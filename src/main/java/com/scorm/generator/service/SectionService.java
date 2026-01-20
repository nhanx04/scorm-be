package com.scorm.generator.service;

import com.scorm.generator.dto.SectionCreateRequest;
import com.scorm.generator.dto.SectionResponse;
import com.scorm.generator.dto.SectionUpdateRequest;
import org.springframework.security.core.Authentication;

public interface SectionService {
    SectionResponse create(Long courseId, SectionCreateRequest request, Authentication authentication);

    SectionResponse update(Long sectionId, SectionUpdateRequest request, Authentication authentication);

    void delete(Long sectionId, Authentication authentication);
}

