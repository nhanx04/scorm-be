package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionMatchingLeft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionMatchingLeftRepository extends JpaRepository<QuestionMatchingLeft, Long> {
    List<QuestionMatchingLeft> findByQuestion_QuestionIdOrderByOrderIndexAsc(Long questionId);

    @Modifying
    @Query("DELETE FROM QuestionMatchingLeft l WHERE l.question.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}

