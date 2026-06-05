package com.scorm.generator.service.ai;

import com.scorm.generator.dto.ai.AiQuizResponse;
import com.scorm.generator.dto.ai.AiQuizResponse.AiQuestion;
import com.scorm.generator.dto.ai.AiQuizResponse.MatchingPair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Schema validator tests — pin the exact failure modes D3/D5 evaluation
 * surfaced (empty prompt on FILL/MCQ, missing options on MCQ, wrong
 * correctAnswer type per question type, etc.).
 */
class QuizSchemaValidatorTest {

    @Test
    void wellFormedResponse_returnsNoIssues() {
        AiQuizResponse ok = new AiQuizResponse(List.of(
                new AiQuestion("MCQ_SINGLE", "Câu hỏi?", List.of("A", "B"), "A",
                        "Theo tài liệu: ...", null, null),
                new AiQuestion("TRUE_FALSE", "Đúng hay sai?", null, true,
                        "Theo tài liệu: ...", null, null)));
        assertTrue(QuizSchemaValidator.validate(ok).isEmpty());
    }

    @Test
    void fillInBlank_withEmptyPrompt_isReported() {
        AiQuestion q = new AiQuestion("FILL_IN_THE_BLANK", "  ",
                null, List.of("answer"), "Theo tài liệu: ...",
                "<p>Sentence with ___</p>", null);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().anyMatch(s -> s.contains("prompt")),
                "Empty prompt should be flagged, got: " + issues);
    }

    @Test
    void mcqSingle_withEmptyPrompt_isReported() {
        AiQuestion q = new AiQuestion("MCQ_SINGLE", null,
                List.of("A", "B"), "A", "Theo tài liệu: ...", null, null);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("prompt")));
    }

    @Test
    void mcq_withOneOption_isReported() {
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A"), "A", "Theo tài liệu: ...", null, null);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("options")));
    }

    @Test
    void trueFalse_withStringCorrectAnswer_isReported() {
        AiQuestion q = new AiQuestion("TRUE_FALSE", "Đúng hay sai?",
                null, "True", "Theo tài liệu: ...", null, null);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("boolean")));
    }

    @Test
    void matching_withFewerThanTwoPairs_isReported() {
        AiQuestion q = new AiQuestion("MATCHING", "Ghép cặp",
                null, null, "Theo tài liệu: ...",
                null, List.of(new MatchingPair("A", "1")));
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("pairs")));
    }

    @Test
    void emptyResponse_reportsOneIssue() {
        assertEquals(1, QuizSchemaValidator.validate(new AiQuizResponse(List.of())).size());
        assertEquals(1, QuizSchemaValidator.validate(null).size());
    }
}
