package com.scorm.generator.service;

import com.scorm.generator.dto.ContentBlockCreateRequest;
import com.scorm.generator.dto.ContentBlockResponse;
import com.scorm.generator.dto.ContentBlockUpdateRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ContentBlockService {
    ContentBlockResponse create(Long contentPageId, ContentBlockCreateRequest request, Authentication authentication);
    ContentBlockResponse update(Long blockId, ContentBlockUpdateRequest request, Authentication authentication);
    void delete(Long blockId, Authentication authentication);
    ContentBlockResponse getById(Long blockId, Authentication authentication);
    List<ContentBlockResponse> getByContentPageId(Long contentPageId, Authentication authentication);
}

