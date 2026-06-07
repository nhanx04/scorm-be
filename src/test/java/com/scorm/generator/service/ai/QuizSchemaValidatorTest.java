package com.scorm.generator.service.ai;

import com.scorm.generator.dto.ai.AiQuizResponse;
import com.scorm.generator.dto.ai.AiQuizResponse.AiQuestion;
import com.scorm.generator.dto.ai.AiQuizResponse.Citation;
import com.scorm.generator.dto.ai.AiQuizResponse.MatchingPair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Schema validator tests — pin the exact failure modes D3/D5/D6 evaluation
 * surfaced (empty prompt on FILL/MCQ, wrong correctAnswer type per question
 * type, etc.), the D7 deterministic citation grounding check, plus the
 * runtime Bloom + Item-Writing-Flaw gates.
 */
class QuizSchemaValidatorTest {

    /** Citation that should always pass given a matching source text. */
    private static final Citation OK_CITATION = new Citation(
            "Slide 3", "neutron is the neutral particle",
            "Quote khẳng định trực tiếp đáp án.");

    private static final String OK_SOURCE = "Slide 3: in the atom, the neutron is the neutral particle.";

    /** Valid Bloom level so fixtures focused on other checks don't trip the Bloom gate. */
    private static final int OK_BLOOM = 2;

    @Test
    void wellFormedResponse_returnsNoIssues() {
        AiQuizResponse ok = new AiQuizResponse(List.of(
                new AiQuestion("MCQ_SINGLE", "Câu hỏi?", List.of("A", "B"), "A",
                        OK_CITATION, null, null, OK_BLOOM),
                new AiQuestion("TRUE_FALSE", "Đúng hay sai?", null, true,
                        OK_CITATION, null, null, OK_BLOOM)));
        // Schema-only check — no source provided
        assertTrue(QuizSchemaValidator.validate(ok).isEmpty());
        // Full check including substring grounding
        assertTrue(QuizSchemaValidator.validate(ok, OK_SOURCE).isEmpty());
    }

    @Test
    void fillInBlank_withEmptyPrompt_isReported() {
        AiQuestion q = new AiQuestion("FILL_IN_THE_BLANK", "  ",
                null, List.of("answer"), OK_CITATION,
                "<p>Sentence with ___</p>", null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().anyMatch(s -> s.contains("prompt")),
                "Empty prompt should be flagged, got: " + issues);
    }

    @Test
    void mcqSingle_withEmptyPrompt_isReported() {
        AiQuestion q = new AiQuestion("MCQ_SINGLE", null,
                List.of("A", "B"), "A", OK_CITATION, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("prompt")));
    }

    @Test
    void mcq_withOneOption_isReported() {
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A"), "A", OK_CITATION, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("options")));
    }

    @Test
    void trueFalse_withStringCorrectAnswer_isReported() {
        AiQuestion q = new AiQuestion("TRUE_FALSE", "Đúng hay sai?",
                null, "True", OK_CITATION, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("boolean")));
    }

    @Test
    void matching_withFewerThanTwoPairs_isReported() {
        AiQuestion q = new AiQuestion("MATCHING", "Ghép cặp",
                null, null, OK_CITATION,
                null, List.of(new MatchingPair("A", "1")), OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("pairs")));
    }

    @Test
    void emptyResponse_reportsOneIssue() {
        assertEquals(1, QuizSchemaValidator.validate(new AiQuizResponse(List.of())).size());
        assertEquals(1, QuizSchemaValidator.validate(null).size());
    }

    // ---------- Bloom gate ----------

    @Test
    void missingBloomLevel_isReported() {
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A", "B"), "A", OK_CITATION, null, null, null);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("bloomLevel")),
                "Missing Bloom level should be flagged, got: " + issues);
    }

    @Test
    void bloomLevelOutOfRange_isReported() {
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A", "B"), "A", OK_CITATION, null, null, 7);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("[1,6]")));
    }

    // ---------- Item Writing Flaw gate ----------

    @Test
    void aotaOption_isReported() {
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Which are valid?",
                List.of("Option A", "Option B", "All of the above"), "All of the above",
                OK_CITATION, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("ID-2")),
                "All-of-the-above should be flagged, got: " + issues);
    }

    @Test
    void absoluteTermInDistractor_isReported() {
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Pick the right statement",
                List.of("It depends on the input order", "This is never the case at all"),
                "It depends on the input order", OK_CITATION, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("TW-5")),
                "Absolute term in distractor should be flagged, got: " + issues);
    }

    @Test
    void absoluteTermInCorrectAnswer_isNotFlaggedAsTw5() {
        // The absolute term is in the CORRECT answer, not a distractor → not a TW-5 flaw.
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Pick the right statement",
                List.of("This always halts on sorted input", "A short distractor"),
                "This always halts on sorted input", OK_CITATION, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().noneMatch(s -> s.contains("TW-5")),
                "Absolute term in the correct answer must not be a TW-5 flaw, got: " + issues);
    }

    @Test
    void lengthCue_longCorrectAnswer_isReported() {
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Which best describes it?",
                List.of("Short one", "Brief", "It is the fully detailed correct explanation that runs much longer"),
                "It is the fully detailed correct explanation that runs much longer",
                OK_CITATION, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("TW-1")),
                "Length cue should be flagged, got: " + issues);
    }

    @Test
    void balancedMcq_passesIwfGate() {
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Which sorting case is O(n)?",
                List.of("Already sorted input array", "Reverse sorted input array",
                        "Randomly shuffled input array"),
                "Already sorted input array", OK_CITATION, null, null, 3);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.isEmpty(), "Balanced MCQ should pass the IWF gate, got: " + issues);
    }

    // ---------- Citation grounding tests (D7) ----------

    @Test
    void missingCitation_isReported() {
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A", "B"), "A", null, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("citation")));
    }

    @Test
    void citationWithEmptyFields_isReported() {
        Citation bad = new Citation("", "", "");
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A", "B"), "A", bad, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)));
        assertTrue(issues.stream().anyMatch(s -> s.contains("sourceLocation")));
        assertTrue(issues.stream().anyMatch(s -> s.contains("verbatimQuote")));
        assertTrue(issues.stream().anyMatch(s -> s.contains("reasoning")));
    }

    @Test
    void citationQuoteNotInSource_isReported() {
        Citation hallucinated = new Citation("Slide 3",
                "this exact phrase does not appear in the source document",
                "Bịa quote.");
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A", "B"), "A", hallucinated, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)),
                OK_SOURCE);
        assertTrue(issues.stream().anyMatch(s -> s.contains("KHÔNG tìm thấy")),
                "Expected hallucination flag, got: " + issues);
    }

    @Test
    void citationQuoteInSource_withWhitespaceVariation_isAccepted() {
        // Source has different whitespace than the quote — normaliser should handle it
        String spaced = "Slide   3:\n\tin the atom,  the neutron is\nthe neutral\tparticle.";
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A", "B"), "A", OK_CITATION, null, null, OK_BLOOM);
        assertTrue(QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)), spaced).isEmpty(),
                "Whitespace-different but substring-matching quote should pass");
    }

    @Test
    void citationQuoteWithCurlyQuotes_isNormalised() {
        // Model often emits “smart quotes”; source has plain quotes
        Citation curly = new Citation("Slide 3",
                "“neutron is the neutral particle”",
                "Quote.");
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A", "B"), "A", curly, null, null, OK_BLOOM);
        assertTrue(QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)),
                "Slide 3: \"neutron is the neutral particle\" is what we mean.").isEmpty(),
                "Curly quotes should be normalised to straight before substring match");
    }

    @Test
    void citationQuoteTooShort_isReported() {
        Citation tiny = new Citation("Slide 3", "abc", "Quote.");
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A", "B"), "A", tiny, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)), OK_SOURCE);
        assertTrue(issues.stream().anyMatch(s -> s.contains("quá ngắn")));
    }

    @Test
    void citationQuoteWithEllipsis_isReported() {
        Citation joined = new Citation("Slide 3",
                "Binary Search ... Time complexity O(log n)",
                "Bịa quote bằng cách nối '...'");
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A", "B"), "A", joined, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)), OK_SOURCE);
        assertTrue(issues.stream().anyMatch(s -> s.contains("'...'") || s.contains("LIỀN MẠCH")),
                "Quote containing ... should be rejected, got: " + issues);
    }

    @Test
    void citationQuoteWithBulletMarker_isReported() {
        Citation joined = new Citation("Slide 3",
                "Functional: Lisp • Logic: Prolog",
                "Bịa bằng ghép bullet.");
        AiQuestion q = new AiQuestion("MCQ_SINGLE", "Câu hỏi?",
                List.of("A", "B"), "A", joined, null, null, OK_BLOOM);
        List<String> issues = QuizSchemaValidator.validate(new AiQuizResponse(List.of(q)), OK_SOURCE);
        assertTrue(issues.stream().anyMatch(s -> s.contains("bullet")),
                "Quote containing bullet marker should be rejected, got: " + issues);
    }
}
