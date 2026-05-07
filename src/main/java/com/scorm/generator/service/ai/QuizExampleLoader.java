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
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class QuizExampleLoader {

    private static final Logger log = LoggerFactory.getLogger(QuizExampleLoader.class);

    private static final String EXAMPLES_PATTERN = "classpath:prompts/quiz/examples-*.json";

    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private final Map<String, String> formattedByType = new LinkedHashMap<>();
    private String allExamplesFormatted = "";

    public QuizExampleLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void loadExamples() throws IOException {
        Resource[] resources = resolver.getResources(EXAMPLES_PATTERN);
        if (resources.length == 0) {
            log.warn("No quiz example files found at {}", EXAMPLES_PATTERN);
            return;
        }

        StringBuilder all = new StringBuilder();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            JsonNode root;
            try (var in = resource.getInputStream()) {
                root = objectMapper.readTree(in);
            }

            String type = textOrEmpty(root, "type");
            JsonNode expectedOutput = root.get("expectedOutput");
            if (type.isEmpty() || expectedOutput == null) {
                log.warn("Skipping malformed quiz example file: {}", filename);
                continue;
            }

            String formatted = formatBlock(type,
                    textOrEmpty(root, "description"),
                    textOrEmpty(root, "sourceContext"),
                    objectMapper.writeValueAsString(expectedOutput));

            formattedByType.put(type, formatted);
            all.append(formatted).append("\n");
        }

        allExamplesFormatted = all.toString().trim();
        log.info("Loaded {} quiz few-shot examples: {}", formattedByType.size(), formattedByType.keySet());
    }

    public String formatAllExamples() {
        return allExamplesFormatted;
    }

    public Map<String, String> getFormattedByType() {
        return Map.copyOf(formattedByType);
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText();
    }

    private static String formatBlock(String type, String description, String sourceContext, String expectedJson) {
        return """
                === VÍ DỤ %s ===
                Ghi chú: %s

                Đoạn văn nguồn (sourceText giả định):
                "%s"

                JSON đầu ra mong đợi cho câu hỏi loại này:
                %s
                """.formatted(type, description, sourceContext, expectedJson);
    }
}
