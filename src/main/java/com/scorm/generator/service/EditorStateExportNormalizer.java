package com.scorm.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scorm.generator.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class EditorStateExportNormalizer {

    private final ObjectMapper objectMapper;

    public EditorStateExportNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode normalizeForScorm(JsonNode source, Course course) {
        ObjectNode out = objectMapper.createObjectNode();
        JsonNode safeSource = source == null || source.isNull() ? objectMapper.createObjectNode() : source;

        out.put("title", textOrDefault(safeSource.get("title"), defaultText(course != null ? course.getTitle() : null, "Untitled Course")));
        out.put("description", textOrDefault(safeSource.get("description"), course != null ? course.getDescription() : ""));
        out.put("coverImageUrl", textOrDefault(safeSource.get("coverImageUrl"), course != null ? course.getCoverImageUrl() : ""));
        out.put("passingScore", intOrDefault(safeSource.get("passingScore"), course != null && course.getPassingScore() != null ? course.getPassingScore().intValue() : 80));
        out.put("attemptLimit", intOrDefault(safeSource.get("attemptLimit"), course != null ? course.getAttemptLimit() : 0));
        out.put("durationMin", intOrDefault(safeSource.get("durationMin"), course != null ? course.getDurationMin() : 0));

        ArrayNode sections = objectMapper.createArrayNode();
        if (safeSource.has("sections") && safeSource.get("sections").isArray()) {
            safeSource.get("sections").forEach(sectionNode -> sections.add(normalizeSection(sectionNode, safeSource)));
        } else {
            JsonNode sectionOrder = safeSource.path("sectionOrder");
            JsonNode sectionsMap = safeSource.path("sections");
            if (sectionOrder.isArray() && sectionsMap.isObject()) {
                sectionOrder.forEach(sectionId -> {
                    JsonNode sectionNode = sectionsMap.path(sectionId.asText());
                    sections.add(normalizeSection(sectionNode, safeSource));
                });
            }
        }
        out.set("sections", sections);
        out.put("schemaVersion", "editor-state-v2");
        return out;
    }

    private ObjectNode normalizeSection(JsonNode sectionNode, JsonNode rootSource) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", textOrDefault(sectionNode != null ? sectionNode.get("id") : null, ""));
        section.put("title", textOrDefault(sectionNode != null ? sectionNode.get("title") : null, "Untitled section"));
        section.put("description", textOrDefault(sectionNode != null ? sectionNode.get("description") : null, ""));

        ArrayNode pages = objectMapper.createArrayNode();
        if (sectionNode != null && sectionNode.has("pages") && sectionNode.get("pages").isArray()) {
            sectionNode.get("pages").forEach(pageNode -> pages.add(normalizePage(pageNode, rootSource)));
        } else {
            String sectionId = textOrDefault(sectionNode != null ? sectionNode.get("id") : null, "");
            JsonNode pageOrder = rootSource.path("pageOrder").path(sectionId);
            JsonNode pagesMap = rootSource.path("pages");
            if (pageOrder.isArray() && pagesMap.isObject()) {
                pageOrder.forEach(pageId -> pages.add(normalizePage(pagesMap.path(pageId.asText()), rootSource)));
            }
        }
        section.set("pages", pages);
        return section;
    }

    private ObjectNode normalizePage(JsonNode pageNode, JsonNode rootSource) {
        ObjectNode page = objectMapper.createObjectNode();
        String rawPageType = textOrDefault(pageNode != null ? pageNode.get("pageType") : null,
                textOrDefault(pageNode != null ? pageNode.get("type") : null, "CONTENT"));
        String pageType = "quiz".equalsIgnoreCase(rawPageType) ? "QUIZ" : rawPageType.toUpperCase();

        page.put("id", textOrDefault(pageNode != null ? pageNode.get("id") : null, ""));
        page.put("title", textOrDefault(pageNode != null ? pageNode.get("title") : null, "Untitled page"));
        page.put("pageType", pageType);

        if ("QUIZ".equalsIgnoreCase(pageType)) {
            ObjectNode quizPage = objectMapper.createObjectNode();
            quizPage.put("passingScore", intOrDefault(pageNode != null ? pageNode.get("passingScore") : null, 80));
            quizPage.put("attemptAllowed", intOrDefault(pageNode != null ? pageNode.get("attemptAllowed") : null, 0));

            ArrayNode questions = objectMapper.createArrayNode();
            JsonNode embeddedQuestions = pageNode != null ? pageNode.path("quizPage").path("questions") : objectMapper.createArrayNode();
            if (embeddedQuestions.isArray() && embeddedQuestions.size() > 0) {
                embeddedQuestions.forEach(questions::add);
            } else {
                String pageId = textOrDefault(pageNode != null ? pageNode.get("id") : null, "");
                JsonNode questionOrder = rootSource.path("questionOrder").path(pageId);
                JsonNode questionMap = rootSource.path("questions");
                if (questionOrder.isArray() && questionMap.isObject()) {
                    questionOrder.forEach(qid -> questions.add(questionMap.path(qid.asText())));
                }
            }
            quizPage.set("questions", questions);
            page.set("quizPage", quizPage);
        } else {
            ObjectNode contentPage = objectMapper.createObjectNode();
            contentPage.put("layoutType", textOrDefault(pageNode != null ? pageNode.get("layoutType") : null, "SINGLE_COLUMN"));
            ArrayNode blocks = objectMapper.createArrayNode();
            JsonNode embeddedBlocks = pageNode != null ? pageNode.path("contentPage").path("blocks") : objectMapper.createArrayNode();
            if (embeddedBlocks.isArray() && embeddedBlocks.size() > 0) {
                embeddedBlocks.forEach(blocks::add);
            } else {
                String pageId = textOrDefault(pageNode != null ? pageNode.get("id") : null, "");
                JsonNode blockOrder = rootSource.path("blockOrder").path(pageId);
                JsonNode blockMap = rootSource.path("blocks");
                if (blockOrder.isArray() && blockMap.isObject()) {
                    blockOrder.forEach(blockId -> blocks.add(blockMap.path(blockId.asText())));
                }
            }
            contentPage.set("blocks", blocks);
            page.set("contentPage", contentPage);
        }
        return page;
    }

    private String textOrDefault(JsonNode node, String fallback) {
        if (node == null || node.isNull()) return defaultText(fallback, "");
        String value = node.asText();
        return value == null || value.isBlank() ? defaultText(fallback, "") : value;
    }

    private int intOrDefault(JsonNode node, int fallback) {
        return node != null && node.isNumber() ? node.asInt() : fallback;
    }

    private String defaultText(String value, String fallback) {
        return value == null ? fallback : value;
    }
}

