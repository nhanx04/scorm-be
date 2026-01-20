package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionMatchingRight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionMatchingRightRepository extends JpaRepository<QuestionMatchingRight, Long> {
    List<QuestionMatchingRight> findByQuestion_QuestionIdOrderByOrderIndexAsc(Long questionId);

    @Modifying
    @Query("DELETE FROM QuestionMatchingRight r WHERE r.question.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}

