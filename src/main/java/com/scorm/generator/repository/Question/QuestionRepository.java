package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q.questionId AS questionId, q.title AS title, q.instruction AS instruction, "
            + "q.promptHtml AS promptHtml, q.textHtml AS textHtml, q.questionType AS questionType, "
            + "q.themeOverride AS themeOverride, q.layoutMode AS layoutMode, q.layoutMeta AS layoutMeta, "
            + "q.templateData AS templateData, q.points AS points, q.shuffleOptions AS shuffleOptions, "
            + "q.caseSensitive AS caseSensitive, q.extraConfig AS extraConfig "
            + "FROM Question q WHERE q.questionId IN :ids")
    List<QuestionSummaryProjection> findSummariesByIds(@Param("ids") List<Long> ids);
}
