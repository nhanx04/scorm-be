package com.scorm.generator.dto.ai;

import lombok.Data;

@Data
public class GenerateCourseQuizRequest {
    private Long courseId; // Khóa học để nạp tài liệu gốc làm nguồn dữ kiện
    private String focusTopic; // Nội dung/chủ đề người dùng nhập muốn ra đề
    private String courseTitle;
    private String courseDescription;
    private String sectionTitle;
    private String pageTitle;
    private String sourceText; // Backend tự nạp từ tài liệu gốc của khóa học (không phải frontend gửi)
    private int numberOfQuestions = 6;
    private String language = "Vietnamese";
    private String difficulty = "Trung bình";
}
