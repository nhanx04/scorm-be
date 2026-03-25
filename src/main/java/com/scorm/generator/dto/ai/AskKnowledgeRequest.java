package com.scorm.generator.dto.ai;

import lombok.Data;

@Data
public class AskKnowledgeRequest {
    private String courseTitle;
    private String courseDescription;
    private String sectionTitle;
    private String pageTitle;
    private String pageContent;
    private String question;
    private String language = "Vietnamese";
}
