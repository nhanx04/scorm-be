package com.scorm.generator.dto;

import com.scorm.generator.entity.QuizPage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizPageResponse {
    private Long pageId;
    private BigDecimal passingScore;
    private Integer attemptAllowed;

    public static QuizPageResponse fromEntity(QuizPage quizPage) {
        return QuizPageResponse.builder()
                .pageId(quizPage.getPageId())
                .passingScore(quizPage.getPassingScore())
                .attemptAllowed(quizPage.getAttemptAllowed())
                .build();
    }
}

