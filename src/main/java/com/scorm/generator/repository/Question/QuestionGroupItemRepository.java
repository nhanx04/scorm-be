package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionGroupItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionGroupItemRepository extends JpaRepository<QuestionGroupItem, Long> {
    List<QuestionGroupItem> findByQuestion_QuestionIdOrderByOrderIndexAsc(Long questionId);

    @Modifying
    @Query("DELETE FROM QuestionGroupItem i WHERE i.question.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}

