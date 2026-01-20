package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionShortAnswerExpected;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionShortAnswerExpectedRepository extends JpaRepository<QuestionShortAnswerExpected, Long> {
    List<QuestionShortAnswerExpected> findByQuestion_QuestionId(Long questionId);

    @Modifying
    @Query("DELETE FROM QuestionShortAnswerExpected e WHERE e.question.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}

