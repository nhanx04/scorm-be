package com.scorm.generator.dto.ai;

import java.util.List;

public record AiQuizResponse(
                List<AiQuestion> questions) {
        public record AiQuestion(
                        String type,
                        String prompt,
                        List<String> options,
                        Object correctAnswer,
                        String explanation,
                        String sentenceHtml,
                        List<MatchingPair> pairs) {
        }

        public record MatchingPair(
                        String left,
                        String right) {
        }
}