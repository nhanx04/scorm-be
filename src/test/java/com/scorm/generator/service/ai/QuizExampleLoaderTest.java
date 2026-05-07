package com.scorm.generator.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorm.generator.dto.ai.AiQuizResponse;
import com.scorm.generator.dto.ai.AiQuizResponse.AiQuestion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for quiz few-shot examples.
 *
 * Mục tiêu chính: bảo vệ chống "example rot" — khi DTO {@link AiQuestion} đổi
 * hoặc khi ai đó sửa file JSON ví dụ, test sẽ fail nếu shape của các trường
 * không còn khớp với hợp đồng đã định trong prompt engineering.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuizExampleLoaderTest {

    private static final Set<String> EXPECTED_TYPES = Set.of(
            "MCQ_SINGLE",
            "MCQ_MULTIPLE",
            "TRUE_FALSE",
            "SHORT_ANSWER",
            "FILL_IN_THE_BLANK",
            "MATCHING");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private Map<String, JsonNode> rootByType;
    private Map<String, AiQuestion> questionByType;

    @BeforeAll
    void loadAll() throws Exception {
        rootByType = new HashMap<>();
        questionByType = new HashMap<>();

        Resource[] resources = resolver.getResources("classpath:prompts/quiz/examples-*.json");
        assertEquals(EXPECTED_TYPES.size(), resources.length,
                "Số lượng file ví dụ quiz phải khớp với số loại câu hỏi");

        for (Resource resource : resources) {
            JsonNode root;
            try (var in = resource.getInputStream()) {
                root = objectMapper.readTree(in);
            }
            String type = root.get("type").asText();
            JsonNode expected = root.get("expectedOutput");
            assertNotNull(expected, "Thiếu expectedOutput trong file: " + resource.getFilename());

            AiQuestion question = objectMapper.treeToValue(expected, AiQuestion.class);
            assertNotNull(question, "Không parse được expectedOutput thành AiQuestion: " + resource.getFilename());
            assertEquals(type, question.type(),
                    "Trường type trong expectedOutput phải khớp với type ngoài cùng (file: "
                            + resource.getFilename() + ")");

            rootByType.put(type, root);
            questionByType.put(type, question);
        }
    }

    @Test
    void allSixTypesArePresent() {
        assertEquals(EXPECTED_TYPES, questionByType.keySet(),
                "Các file ví dụ phải bao phủ đủ 6 loại câu hỏi");
    }

    @Test
    void everyExampleHasPromptAndExplanation() {
        for (Map.Entry<String, AiQuestion> entry : questionByType.entrySet()) {
            AiQuestion q = entry.getValue();
            assertFalse(q.prompt() == null || q.prompt().isBlank(),
                    "Trường prompt rỗng cho loại " + entry.getKey());
            assertFalse(q.explanation() == null || q.explanation().isBlank(),
                    "Trường explanation rỗng cho loại " + entry.getKey());
        }
    }

    @Test
    void mcqSingle_correctAnswerIsString() {
        AiQuestion q = requireType("MCQ_SINGLE");
        assertInstanceOf(String.class, q.correctAnswer(),
                "MCQ_SINGLE.correctAnswer phải là chuỗi đơn (String)");
        assertNotNull(q.options());
        assertTrue(q.options().size() >= 2, "MCQ_SINGLE phải có ít nhất 2 options");
        assertTrue(q.options().contains((String) q.correctAnswer()),
                "MCQ_SINGLE.correctAnswer phải nằm trong danh sách options");
    }

    @Test
    void mcqMultiple_correctAnswerIsListOfString() {
        AiQuestion q = requireType("MCQ_MULTIPLE");
        assertInstanceOf(List.class, q.correctAnswer(),
                "MCQ_MULTIPLE.correctAnswer phải là List<String>");
        @SuppressWarnings("unchecked")
        List<Object> answers = (List<Object>) q.correctAnswer();
        assertTrue(answers.size() >= 2,
                "MCQ_MULTIPLE phải có ít nhất 2 đáp án đúng để phân biệt với MCQ_SINGLE");
        for (Object a : answers) {
            assertInstanceOf(String.class, a, "Mỗi phần tử trong correctAnswer phải là String");
        }
    }

    @Test
    void trueFalse_correctAnswerIsBooleanPrimitive() {
        AiQuestion q = requireType("TRUE_FALSE");
        assertInstanceOf(Boolean.class, q.correctAnswer(),
                "TRUE_FALSE.correctAnswer phải là boolean nguyên thủy, không phải chuỗi");
    }

    @Test
    void shortAnswer_correctAnswerIsListOfString() {
        AiQuestion q = requireType("SHORT_ANSWER");
        assertInstanceOf(List.class, q.correctAnswer(),
                "SHORT_ANSWER.correctAnswer luôn là mảng (kể cả khi chỉ có 1 đáp án)");
        @SuppressWarnings("unchecked")
        List<Object> answers = (List<Object>) q.correctAnswer();
        assertFalse(answers.isEmpty(), "SHORT_ANSWER phải có ít nhất 1 đáp án");
        for (Object a : answers) {
            assertInstanceOf(String.class, a);
        }
    }

    @Test
    void fillBlank_hasSentenceHtmlAndOrderedAnswerArray() {
        AiQuestion q = requireType("FILL_IN_THE_BLANK");
        assertNotNull(q.sentenceHtml(), "FILL_IN_THE_BLANK phải có sentenceHtml");
        assertFalse(q.sentenceHtml().isBlank());
        assertTrue(q.sentenceHtml().contains("___"),
                "sentenceHtml nên chứa marker '___' cho mỗi chỗ trống");

        assertInstanceOf(List.class, q.correctAnswer(),
                "FILL_IN_THE_BLANK.correctAnswer là mảng đáp án theo thứ tự chỗ trống");

        long blankCount = countOccurrences(q.sentenceHtml(), "___");
        @SuppressWarnings("unchecked")
        List<Object> answers = (List<Object>) q.correctAnswer();
        assertEquals(blankCount, answers.size(),
                "Số phần tử correctAnswer phải khớp số chỗ trống '___' trong sentenceHtml");
    }

    @Test
    void matching_pairsArePopulatedAndCorrectAnswerNull() {
        AiQuestion q = requireType("MATCHING");
        assertNull(q.correctAnswer(),
                "MATCHING.correctAnswer phải là null (đáp án nằm trong pairs)");
        assertNotNull(q.pairs(), "MATCHING phải có trường pairs");
        assertTrue(q.pairs().size() >= 3, "MATCHING nên có ít nhất 3 cặp để đủ thử thách");
        for (AiQuizResponse.MatchingPair p : q.pairs()) {
            assertFalse(p.left() == null || p.left().isBlank(), "left không được rỗng");
            assertFalse(p.right() == null || p.right().isBlank(), "right không được rỗng");
        }
    }

    @Test
    void loader_loadsAndFormatsAllExamples() throws Exception {
        QuizExampleLoader loader = new QuizExampleLoader(new ObjectMapper());
        loader.loadExamples();

        String formatted = loader.formatAllExamples();
        assertNotNull(formatted);
        assertFalse(formatted.isBlank(), "formatAllExamples() không được trả chuỗi rỗng");

        for (String type : EXPECTED_TYPES) {
            assertTrue(formatted.contains(type),
                    "Chuỗi few-shot ghép phải chứa block cho loại: " + type);
        }
        assertEquals(EXPECTED_TYPES, loader.getFormattedByType().keySet());
    }

    private AiQuestion requireType(String type) {
        AiQuestion q = questionByType.get(type);
        assertNotNull(q, "Thiếu file ví dụ cho loại: " + type);
        return q;
    }

    private static long countOccurrences(String haystack, String needle) {
        long count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
