package com.scorm.generator.service.Question;

import com.scorm.generator.dto.Question.QuestionCreateRequest;
import com.scorm.generator.dto.Question.QuestionDetailsRequest;
import com.scorm.generator.dto.Question.QuestionResponse;
import com.scorm.generator.dto.Question.QuestionUpdateRequest;

public interface QuestionService {
    QuestionResponse create(QuestionCreateRequest request);

    QuestionResponse getById(Long questionId);

    QuestionResponse update(Long questionId, QuestionUpdateRequest request);

    QuestionResponse replaceDetails(Long questionId, QuestionDetailsRequest request);

    void delete(Long questionId);
}

