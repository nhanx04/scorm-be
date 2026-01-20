package com.scorm.generator.repository.Question;

import com.scorm.generator.entity.Question.QuestionBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionBlankRepository extends JpaRepository<QuestionBlank, Long> {
    List<QuestionBlank> findByQuestion_QuestionIdOrderByOrderIndexAsc(Long questionId);

    @Modifying
    @Query("DELETE FROM QuestionBlank b WHERE b.question.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}

