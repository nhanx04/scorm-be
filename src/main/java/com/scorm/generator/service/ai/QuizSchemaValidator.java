package com.scorm.generator.service.ai;

import com.scorm.generator.dto.ai.AiQuizResponse;
import com.scorm.generator.dto.ai.AiQuizResponse.AiQuestion;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-parse schema validator for AI-generated quiz responses.
 *
 * <p>The Gemini prompt already spells out what each question type requires,
 * but D3+D5 evaluation showed that the model still occasionally returns a
 * malformed payload (notably FILL_IN_THE_BLANK with empty {@code prompt},
 * and post-D5 MCQ_SINGLE with empty {@code prompt}). This validator
 * surfaces those issues so the caller can decide to retry with a
 * corrective prompt instead of returning broken data to the client.
 *
 * <p>Returns a list of human-readable issue descriptions — empty list
 * means the response is well-formed.
 */
public final class QuizSchemaValidator {

    private QuizSchemaValidator() {
    }

    public static List<String> validate(AiQuizResponse response) {
        List<String> issues = new ArrayList<>();
        if (response == null || response.questions() == null || response.questions().isEmpty()) {
            issues.add("response.questions trống hoặc null");
            return issues;
        }
        for (int i = 0; i < response.questions().size(); i++) {
            AiQuestion q = response.questions().get(i);
            String prefix = "Q" + (i + 1) + " (" + safeType(q) + "): ";
            validateOne(q, prefix, issues);
        }
        return issues;
    }

    private static void validateOne(AiQuestion q, String prefix, List<String> issues) {
        if (isBlank(q.type())) {
            issues.add(prefix + "thiếu trường `type`");
        }
        if (isBlank(q.prompt())) {
            issues.add(prefix + "thiếu trường `prompt` (BẮT BUỘC cho mọi loại câu hỏi)");
        }
        if (isBlank(q.explanation())) {
            issues.add(prefix + "thiếu trường `explanation`");
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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String safeType(AiQuestion q) {
        return q == null || q.type() == null ? "?" : q.type();
    }
}
