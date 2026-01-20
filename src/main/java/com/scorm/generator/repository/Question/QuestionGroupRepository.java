package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, Long> {
    List<QuestionGroup> findByQuestion_QuestionIdOrderByOrderIndexAsc(Long questionId);

    @Modifying
    @Query("DELETE FROM QuestionGroup g WHERE g.question.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}

