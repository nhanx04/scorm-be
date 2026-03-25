package com.scorm.generator.dto.ai;

import lombok.Data;

@Data
public class GenerateCourseQuizRequest {
    private String courseTitle;
    private String courseDescription;
    private String sectionTitle;
    private String pageTitle;
    private String sourceText;
    private int numberOfQuestions = 6;
    private String language = "Vietnamese";
    private String difficulty = "Trung bình";
}
