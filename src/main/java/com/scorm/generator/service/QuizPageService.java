package com.scorm.generator.service;

import com.scorm.generator.dto.QuizPageCreateRequest;
import com.scorm.generator.dto.QuizPageResponse;
import com.scorm.generator.dto.QuizPageUpdateRequest;
import org.springframework.security.core.Authentication;

public interface QuizPageService {
    QuizPageResponse create(Long pageId, QuizPageCreateRequest request, Authentication authentication);

    QuizPageResponse update(Long pageId, QuizPageUpdateRequest request, Authentication authentication);
}

