package com.scorm.generator.service;

import com.scorm.generator.dto.ContentPageCreateRequest;
import com.scorm.generator.dto.ContentPageResponse;
import com.scorm.generator.dto.ContentPageUpdateRequest;
import org.springframework.security.core.Authentication;

public interface ContentPageService {
    ContentPageResponse create(Long pageId, ContentPageCreateRequest request, Authentication authentication);

    ContentPageResponse update(Long pageId, ContentPageUpdateRequest request, Authentication authentication);
}