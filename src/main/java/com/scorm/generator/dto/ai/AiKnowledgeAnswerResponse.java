package com.scorm.generator.dto.ai;

public record AiKnowledgeAnswerResponse(
                String answer,
                boolean groundedInCourse,
                String sourceScope) {
}
