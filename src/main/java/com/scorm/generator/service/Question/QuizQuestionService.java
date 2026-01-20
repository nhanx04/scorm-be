package com.scorm.generator.service.Question;

import org.springframework.security.core.Authentication;

public interface QuizQuestionService {
    void attach(Long pageId, Long questionId, Authentication authentication);

    void detach(Long pageId, Long questionId, Authentication authentication);
}

