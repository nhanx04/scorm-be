package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionFillBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionFillBlankRepository extends JpaRepository<QuestionFillBlank, Long> {
    void deleteByQuestion_QuestionId(Long questionId);
}
