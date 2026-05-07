package com.scorm.generator.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorm.generator.dto.ai.AiPageContentResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cho ví dụ few-shot của Page Content.
 *
 * Đảm bảo:
 *  - example-positive.json parse được thành {@link AiPageContentResponse}
 *  - htmlContent của ví dụ "đúng" KHÔNG chứa các thẻ trong blacklist
 *  - example-negative.json có ghi đúng các vi phạm để dạy model
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PageContentExampleLoaderTest {

    private static final List<String> HTML_BLACKLIST = List.of(
            "<html", "<head", "<body", "<script", "<style",
            "<div", "<span", "<h1", "<pre", "<code", "<img", "<a ");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode positiveRoot;
    private JsonNode negativeRoot;
    private AiPageContentResponse positiveExpected;

    @BeforeAll
    void load() throws Exception {
        try (var in = new ClassPathResource("prompts/page-content/example-positive.json").getInputStream()) {
            positiveRoot = objectMapper.readTree(in);
        }
        try (var in = new ClassPathResource("prompts/page-content/example-negative.json").getInputStream()) {
            negativeRoot = objectMapper.readTree(in);
        }
        positiveExpected = objectMapper.treeToValue(positiveRoot.get("expectedOutput"), AiPageContentResponse.class);
    }

    @Test
    void positiveExample_parsesAsAiPageContentResponse() {
        assertNotNull(positiveExpected);
        assertNotNull(positiveExpected.pageTitle());
        assertNotNull(positiveExpected.htmlContent());
        assertNotNull(positiveExpected.shortSummary());
        assertTrue(positiveExpected.estimatedReadingTime() > 0,
                "estimatedReadingTime phải > 0");
    }

    @Test
    void positiveExample_htmlOnlyUsesWhitelistedTags() {
        String html = positiveExpected.htmlContent().toLowerCase();
        for (String forbidden : HTML_BLACKLIST) {
            assertFalse(html.contains(forbidden),
                    "Ví dụ DƯƠNG không được chứa thẻ blacklisted: " + forbidden);
        }
    }

    @Test
    void positiveExample_startsWithH2NotH1() {
        String html = positiveExpected.htmlContent().trim().toLowerCase();
        assertTrue(html.startsWith("<h2"),
                "HTML bài học phải bắt đầu bằng <h2> (không dùng <h1>)");
    }

    @Test
    void positiveExample_hasPedagogicalStructure() {
        String html = positiveExpected.htmlContent().toLowerCase();
        // Cần có ít nhất các thành phần: heading + paragraph + list (ul/ol) + blockquote
        assertTrue(Pattern.compile("<h[23]").matcher(html).find(),
                "Phải có heading <h2> hoặc <h3>");
        assertTrue(html.contains("<p>"), "Phải có ít nhất 1 đoạn <p>");
        assertTrue(html.contains("<ul>") || html.contains("<ol>"),
                "Phải có danh sách <ul> hoặc <ol>");
    }

    @Test
    void negativeExample_listsViolations() {
        JsonNode violations = negativeRoot.get("violations");
        assertNotNull(violations, "Ví dụ ÂM phải có trường violations để giải thích lỗi cho model");
        assertTrue(violations.isArray() && violations.size() >= 3,
                "Cần liệt kê ít nhất 3 vi phạm để đủ contrastive");
    }

    @Test
    void negativeExample_wrongOutputDemonstratesAtLeastOneBlacklistedTag() {
        JsonNode wrong = negativeRoot.get("wrongOutput");
        assertNotNull(wrong, "Ví dụ ÂM phải có trường wrongOutput");
        String wrongHtml = wrong.path("htmlContent").asText("").toLowerCase();
        long matchedBlacklisted = HTML_BLACKLIST.stream()
                .filter(wrongHtml::contains)
                .count();
        assertTrue(matchedBlacklisted >= 3,
                "wrongOutput.htmlContent nên minh họa nhiều anti-pattern (>=3 thẻ blacklisted)");
    }
}
