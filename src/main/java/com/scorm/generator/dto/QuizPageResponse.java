package com.scorm.generator.dto;

import com.scorm.generator.dto.Question.QuestionSummaryDto;
import com.scorm.generator.entity.QuizPage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizPageResponse {
    private Long pageId;
    private BigDecimal passingScore;
    private Integer attemptAllowed;

    private List<QuestionSummaryDto> questions;

    public static QuizPageResponse fromEntity(QuizPage quizPage) {
        return QuizPageResponse.builder()
                .pageId(quizPage.getPageId())
                .passingScore(quizPage.getPassingScore())
                .attemptAllowed(quizPage.getAttemptAllowed())
                .questions(null)
                .build();
    }
}
