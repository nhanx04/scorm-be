package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.MediaOfQuestion;
import com.scorm.generator.entity.Question.MediaOfQuestion.MediaOfQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaOfQuestionRepository extends JpaRepository<MediaOfQuestion, MediaOfQuestionId> {
    List<MediaOfQuestion> findById_QuestionId(Long questionId);
}

