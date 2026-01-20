package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionChoiceOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionChoiceOptionRepository extends JpaRepository<QuestionChoiceOption, Long> {
    List<QuestionChoiceOption> findByQuestion_QuestionIdOrderByOrderIndexAsc(Long questionId);

    void deleteByQuestion_QuestionId(Long questionId);
}

