package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionOfQuiz;
import com.scorm.generator.entity.Question.QuestionOfQuiz.QuestionOfQuizId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionOfQuizRepository extends JpaRepository<QuestionOfQuiz, QuestionOfQuizId> {
    boolean existsById_QuizQuestionIdAndId_QuizPageId(Long questionId, Long pageId);

    void deleteById_QuizQuestionIdAndId_QuizPageId(Long questionId, Long pageId);

    List<QuestionOfQuiz> findById_QuizPageId(Long pageId);

    long countById_QuizQuestionId(Long questionId);
}

