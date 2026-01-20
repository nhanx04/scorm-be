package com.scorm.generator.service;

import com.scorm.generator.dto.PageCreateRequest;
import com.scorm.generator.dto.PageResponse;
import com.scorm.generator.dto.PageUpdateRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface PageService {
    PageResponse create(Long sectionId, PageCreateRequest request, Authentication authentication);

    List<PageResponse> listBySectionId(Long sectionId, Authentication authentication);

    PageResponse getById(Long pageId, Authentication authentication);

    PageResponse update(Long pageId, PageUpdateRequest request, Authentication authentication);

    void delete(Long pageId, Authentication authentication);
}
