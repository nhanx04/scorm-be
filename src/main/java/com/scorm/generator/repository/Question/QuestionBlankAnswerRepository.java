package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionBlankAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionBlankAnswerRepository extends JpaRepository<QuestionBlankAnswer, Long> {

    @Modifying
    @Query("DELETE FROM QuestionBlankAnswer a WHERE a.blank.blankId IN (SELECT b.blankId FROM QuestionBlank b WHERE b.question.questionId = :questionId)")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}

