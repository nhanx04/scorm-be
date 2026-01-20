package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionTrueFalse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionTrueFalseRepository extends JpaRepository<QuestionTrueFalse, Long> {
    void deleteByQuestion_QuestionId(Long questionId);
}
