package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionMatchingPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionMatchingPairRepository extends JpaRepository<QuestionMatchingPair, Long> {
    List<QuestionMatchingPair> findByQuestion_QuestionId(Long questionId);

    @Modifying
    @Query("DELETE FROM QuestionMatchingPair p WHERE p.question.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}

