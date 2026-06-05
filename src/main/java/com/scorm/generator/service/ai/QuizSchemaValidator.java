package com.scorm.generator.service.ai;

import com.scorm.generator.dto.ai.AiQuizResponse;
import com.scorm.generator.dto.ai.AiQuizResponse.AiQuestion;
import com.scorm.generator.dto.ai.AiQuizResponse.Citation;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-parse schema validator for AI-generated quiz responses.
 *
 * <p>Two-layer check:
 * <ol>
 *   <li><b>Schema check:</b> required fields present, correctAnswer type
 *       matches question type, etc. Catches issues like the FILL_IN_THE_BLANK
 *       empty-prompt bug surfaced in D3+D5+D6 evaluation.</li>
 *   <li><b>Deterministic citation verify:</b> when {@code sourceText} is
 *       supplied, every question's {@code citation.verbatimQuote} must
 *       appear character-for-character (after whitespace normalisation)
 *       in the source document. This replaces probabilistic LLM-as-judge
 *       hallucination detection with a no-AI substring match — same input
 *       always yields the same answer, no sparse-GT bias.</li>
 * </ol>
 *
 * <p>Returns a list of human-readable issue descriptions — empty list
 * means the response is well-formed and grounded.
 */
public final class QuizSchemaValidator {

    /** Minimum length of a verbatim quote before substring-matching becomes meaningful. */
    private static final int MIN_QUOTE_LENGTH = 6;

    private QuizSchemaValidator() {
    }

    /** Schema-only validation (no citation grounding check). */
    public static List<String> validate(AiQuizResponse response) {
        return validate(response, null);
    }

    /**
     * Full validation: schema + (if {@code sourceText} non-null) deterministic
     * citation grounding via substring match.
     */
    public static List<String> validate(AiQuizResponse response, String sourceText) {
        List<String> issues = new ArrayList<>();
        if (response == null || response.questions() == null || response.questions().isEmpty()) {
            issues.add("response.questions trống hoặc null");
            return issues;
        }
        String normalisedSource = sourceText == null ? null : normalise(sourceText);
        for (int i = 0; i < response.questions().size(); i++) {
            AiQuestion q = response.questions().get(i);
            String prefix = "Q" + (i + 1) + " (" + safeType(q) + "): ";
            validateSchema(q, prefix, issues);
            if (normalisedSource != null) {
                validateCitation(q, normalisedSource, prefix, issues);
            }
        }
        return issues;
    }

    private static void validateSchema(AiQuestion q, String prefix, List<String> issues) {
        if (isBlank(q.type())) {
            issues.add(prefix + "thiếu trường `type`");
        }
        if (isBlank(q.prompt())) {
            issues.add(prefix + "thiếu trường `prompt` (BẮT BUỘC cho mọi loại câu hỏi)");
        }
        Citation c = q.citation();
        if (c == null) {
            issues.add(prefix + "thiếu trường `citation`");
        } else {
            if (isBlank(c.sourceLocation())) {
                issues.add(prefix + "citation.sourceLocation rỗng");
            }
            if (isBlank(c.verbatimQuote())) {
                issues.add(prefix + "citation.verbatimQuote rỗng");
            }
            if (isBlank(c.reasoning())) {
                issues.add(prefix + "citation.reasoning rỗng");
            }
        }

        String type = q.type() == null ? "" : q.type().toUpperCase();
        switch (type) {
            case "MCQ_SINGLE", "MCQ_MULTIPLE" -> {
                if (q.options() == null || q.options().size() < 2) {
                    issues.add(prefix + "cần ≥ 2 options cho MCQ");
                }
                if (q.correctAnswer() == null) {
                    issues.add(prefix + "thiếu correctAnswer cho MCQ");
                }
            }
            case "TRUE_FALSE" -> {
                if (!(q.correctAnswer() instanceof Boolean)) {
                    issues.add(prefix + "TRUE_FALSE.correctAnswer phải là boolean");
                }
            }
            case "SHORT_ANSWER" -> {
                if (!(q.correctAnswer() instanceof List<?> list) || list.isEmpty()) {
                    issues.add(prefix + "SHORT_ANSWER.correctAnswer phải là mảng đáp án không rỗng");
                }
            }
            case "FILL_IN_THE_BLANK" -> {
                if (isBlank(q.sentenceHtml())) {
                    issues.add(prefix + "FILL_IN_THE_BLANK thiếu sentenceHtml");
                }
                if (!(q.correctAnswer() instanceof List<?> list) || list.isEmpty()) {
                    issues.add(prefix + "FILL_IN_THE_BLANK.correctAnswer phải là mảng theo thứ tự chỗ trống");
                }
            }
            case "MATCHING" -> {
                if (q.pairs() == null || q.pairs().size() < 2) {
                    issues.add(prefix + "MATCHING cần ≥ 2 pairs");
                }
            }
            case "" -> { /* missing type already reported */ }
            default -> issues.add(prefix + "loại câu hỏi không nhận dạng: " + q.type());
        }
    }

    /**
     * Deterministic grounding check: the model-supplied verbatim quote MUST
     * appear (after whitespace normalisation) inside the source text.
     * If it doesn't, the model fabricated the citation — flag it.
     */
    private static void validateCitation(AiQuestion q, String normalisedSource,
                                         String prefix, List<String> issues) {
        if (q.citation() == null || isBlank(q.citation().verbatimQuote())) {
            return; // schema check above will have flagged it
        }
        String raw = q.citation().verbatimQuote();
        // Pre-check: model frequently joins multiple non-contiguous spans
        // with "..." or bullet markers. Those quotes can never substring-match
        // the source, so flag them up front with a clearer reason.
        if (raw.contains("...") || raw.contains("…")) {
            issues.add(prefix + "citation.verbatimQuote chứa dấu '...' — quote phải LIỀN MẠCH, "
                    + "không nối nhiều đoạn: \"" + truncate(raw, 80) + "\"");
            return;
        }
        if (raw.contains("•") || raw.contains(" ● ")) {
            issues.add(prefix + "citation.verbatimQuote chứa bullet marker — chọn 1 câu liền mạch thay vì ghép nhiều bullet");
            return;
        }
        String quote = normalise(raw);
        if (quote.length() < MIN_QUOTE_LENGTH) {
            issues.add(prefix + "citation.verbatimQuote quá ngắn (< "
                    + MIN_QUOTE_LENGTH + " ký tự sau chuẩn hóa)");
            return;
        }
        if (!normalisedSource.contains(quote)) {
            issues.add(prefix + "citation.verbatimQuote KHÔNG tìm thấy trong nguồn — "
                    + "có thể model bịa: \"" + truncate(raw, 80) + "\"");
        }
    }

    /** Lowercase + collapse all whitespace to single spaces + drop curly quotes. */
    private static String normalise(String s) {
        return s.toLowerCase()
                .replace('“', '"').replace('”', '"')
                .replace('‘', '\'').replace('’', '\'')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String safeType(AiQuestion q) {
        return q == null || q.type() == null ? "?" : q.type();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
