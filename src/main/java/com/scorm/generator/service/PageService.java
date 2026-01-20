package com.scorm.generator.service;

import com.scorm.generator.dto.PageCreateRequest;
import com.scorm.generator.dto.PageResponse;
import com.scorm.generator.dto.PageUpdateRequest;
import org.springframework.security.core.Authentication;

public interface PageService {
    PageResponse create(Long sectionId, PageCreateRequest request, Authentication authentication);

    PageResponse update(Long pageId, PageUpdateRequest request, Authentication authentication);

    void delete(Long pageId, Authentication authentication);
}

