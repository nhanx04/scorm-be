package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionGroupItemAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionGroupItemAnswerRepository extends JpaRepository<QuestionGroupItemAnswer, Long> {
    List<QuestionGroupItemAnswer> findByItemIdIn(List<Long> itemIds);

    @Modifying
    @Query("DELETE FROM QuestionGroupItemAnswer a WHERE a.itemId IN (SELECT i.itemId FROM QuestionGroupItem i WHERE i.question.questionId = :questionId)")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}

