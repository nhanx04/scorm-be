package com.scorm.generator.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PageContentExampleLoader {

    private static final Logger log = LoggerFactory.getLogger(PageContentExampleLoader.class);

    private static final String POSITIVE_PATH = "classpath:prompts/page-content/example-positive.json";
    private static final String NEGATIVE_PATH = "classpath:prompts/page-content/example-negative.json";

    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private String positiveBlock = "";
    private String negativeBlock = "";

    public PageContentExampleLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void loadExamples() throws IOException {
        positiveBlock = buildPositiveBlock(readJson(POSITIVE_PATH));
        negativeBlock = buildNegativeBlock(readJson(NEGATIVE_PATH));
        log.info("Loaded page-content few-shot examples (positive + negative)");
    }

    public String getPositiveExample() {
        return positiveBlock;
    }

    public String getNegativeExample() {
        return negativeBlock;
    }

    private JsonNode readJson(String classpathLocation) throws IOException {
        Resource resource = resolver.getResource(classpathLocation);
        if (!resource.exists()) {
            log.warn("Page content example not found: {}", classpathLocation);
            return objectMapper.createObjectNode();
        }
        try (var in = resource.getInputStream()) {
            return objectMapper.readTree(in);
        }
    }

    private String buildPositiveBlock(JsonNode root) throws IOException {
        if (root == null || root.isEmpty()) return "";
        JsonNode input = root.get("input");
        JsonNode expected = root.get("expectedOutput");
        if (expected == null) return "";

        String inputJson = input != null ? objectMapper.writeValueAsString(input) : "{}";
        String expectedJson = objectMapper.writeValueAsString(expected);

        return """
                Ngữ cảnh đầu vào của ví dụ:
                %s

                JSON đầu ra ĐÚNG (HÃY HỌC THEO ĐỊNH DẠNG NÀY):
                %s
                """.formatted(inputJson, expectedJson);
    }

    private String buildNegativeBlock(JsonNode root) throws IOException {
        if (root == null || root.isEmpty()) return "";
        JsonNode wrong = root.get("wrongOutput");
        JsonNode violations = root.get("violations");
        if (wrong == null) return "";

        String wrongJson = objectMapper.writeValueAsString(wrong);
        String violationsJson = violations != null ? objectMapper.writeValueAsString(violations) : "[]";

        return """
                JSON đầu ra SAI (TUYỆT ĐỐI KHÔNG ĐƯỢC TRẢ VỀ NHƯ NÀY):
                %s

                Các lỗi cụ thể trong ví dụ sai trên:
                %s
                """.formatted(wrongJson, violationsJson);
    }
}
