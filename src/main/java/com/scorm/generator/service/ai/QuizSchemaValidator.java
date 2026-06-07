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
            validateBloom(q, prefix, issues);
            validateItemWritingFlaws(q, prefix, issues);
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

    /**
     * Bloom Taxonomy level must be present and in [1,6]. Makes question
     * difficulty measurable (đo độ khó qua thang Bloom) — the prompt maps the
     * requested difficulty onto these levels, this just enforces the model
     * actually tags every question.
     */
    private static void validateBloom(AiQuestion q, String prefix, List<String> issues) {
        Integer bloom = q.bloomLevel();
        if (bloom == null) {
            issues.add(prefix + "thiếu bloomLevel (mức Bloom 1-6) để xác định độ khó");
        } else if (bloom < 1 || bloom > 6) {
            issues.add(prefix + "bloomLevel ngoài khoảng [1,6]: " + bloom);
        }
    }

    // --- Item Writing Flaws: high-precision subset enforced at generation time ---
    // (TW-1 length cue, TW-5 absolute terms in distractor, ID-2 All/None of the
    // above). Lexicons kept in lockstep with QuizPrompts.ITEM_WRITING_STANDARDS.
    private static final List<String> ABSOLUTE_TERMS = List.of(
            "always", "never", "only",
            "luôn luôn", "không bao giờ", "tất cả", "duy nhất");
    private static final List<String> AOTA_PATTERNS = List.of(
            "all of the above", "none of the above",
            "tất cả đều đúng", "tất cả các đáp án trên",
            "không đáp án nào", "không có đáp án nào");
    /** Below this word count an option is too short for the length-cue ratio to be meaningful. */
    private static final int LENGTH_CUE_MIN_WORDS = 6;
    private static final double LENGTH_CUE_RATIO = 1.5;

    /**
     * Mechanically detectable Item Writing Flaws (Haladyna et al. 2002). Only
     * the high-precision subset is gated so the retry loop fixes clear flaws
     * without churning on subjective ones (those stay in the prompt + offline
     * rubric). Applies to choice-based questions only.
     */
    private static void validateItemWritingFlaws(AiQuestion q, String prefix, List<String> issues) {
        String type = q.type() == null ? "" : q.type().toUpperCase();
        boolean isMcq = type.equals("MCQ_SINGLE") || type.equals("MCQ_MULTIPLE");
        if (!isMcq || q.options() == null || q.options().size() < 2) {
            return;
        }
        List<String> options = q.options();

        // ID-2: "All/None of the above" style options.
        for (String opt : options) {
            String low = opt == null ? "" : opt.toLowerCase();
            if (AOTA_PATTERNS.stream().anyMatch(low::contains)) {
                issues.add(prefix + "IWF ID-2: tránh option dạng \"All/None of the above\": \""
                        + truncate(opt, 60) + "\"");
                break;
            }
        }

        // TW-5: absolute terms inside a distractor (not the correct answer).
        java.util.Set<String> correct = correctAnswerStrings(q);
        for (String opt : options) {
            if (opt == null || correct.contains(opt.trim().toLowerCase())) {
                continue; // skip the correct answer(s)
            }
            String low = opt.toLowerCase();
            String hit = ABSOLUTE_TERMS.stream().filter(low::contains).findFirst().orElse(null);
            if (hit != null) {
                issues.add(prefix + "IWF TW-5: distractor chứa từ tuyệt đối \"" + hit
                        + "\" (dễ bị loại không cần kiến thức): \"" + truncate(opt, 60) + "\"");
                break;
            }
        }

        // TW-1: length cue — correct answer markedly longer than the distractors.
        validateLengthCue(q, correct, prefix, issues);
    }

    private static void validateLengthCue(AiQuestion q, java.util.Set<String> correct,
                                          String prefix, List<String> issues) {
        int correctWords = 0;
        int distractorTotal = 0;
        int distractorCount = 0;
        for (String opt : q.options()) {
            if (opt == null || opt.isBlank()) {
                continue;
            }
            int w = opt.trim().split("\\s+").length;
            if (correct.contains(opt.trim().toLowerCase())) {
                correctWords = Math.max(correctWords, w);
            } else {
                distractorTotal += w;
                distractorCount++;
            }
        }
        if (distractorCount == 0 || correctWords < LENGTH_CUE_MIN_WORDS) {
            return; // too short to be a meaningful cue
        }
        double avgDistractor = (double) distractorTotal / distractorCount;
        if (avgDistractor > 0 && correctWords >= LENGTH_CUE_RATIO * avgDistractor) {
            issues.add(prefix + "IWF TW-1: đáp án đúng dài bất thường (" + correctWords
                    + " từ vs trung bình distractor " + String.format("%.1f", avgDistractor)
                    + " từ) — rút gọn để tránh length cue");
        }
    }

    /** Lowercased set of the correct answer option text(s), for distractor exclusion. */
    private static java.util.Set<String> correctAnswerStrings(AiQuestion q) {
        java.util.Set<String> out = new java.util.HashSet<>();
        Object ca = q.correctAnswer();
        if (ca instanceof String s) {
            out.add(s.trim().toLowerCase());
        } else if (ca instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    out.add(o.toString().trim().toLowerCase());
                }
            }
        }
        return out;
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
