package com.scorm.generator.repository.Question;

public interface QuestionSummaryProjection {
    Long getQuestionId();

    String getTitle();

    String getInstruction();

    String getPromptHtml();

    String getTextHtml();

    String getQuestionType();

    com.fasterxml.jackson.databind.JsonNode getThemeOverride();

    String getLayoutMode();

    com.fasterxml.jackson.databind.JsonNode getLayoutMeta();

    com.fasterxml.jackson.databind.JsonNode getTemplateData();

    java.math.BigDecimal getPoints();

    Boolean getShuffleOptions();

    Boolean getCaseSensitive();

    com.fasterxml.jackson.databind.JsonNode getExtraConfig();
}
