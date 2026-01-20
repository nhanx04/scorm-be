package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q.questionId AS questionId, q.title AS title, q.questionType AS questionType "
            + "FROM Question q WHERE q.questionId IN :ids")
    List<QuestionSummaryProjection> findSummariesByIds(@Param("ids") List<Long> ids);
}
