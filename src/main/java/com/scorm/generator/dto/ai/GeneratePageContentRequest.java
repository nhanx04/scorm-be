package com.scorm.generator.dto.AI;

import lombok.Data;

@Data
public class GeneratePageContentRequest {
    private String courseTopic; // Chủ đề của cả khóa học (để AI hiểu ngữ cảnh)
    private String sectionTitle; // Tên chương chứa bài học này
    private String pageTopic; // Chủ đề chính của bài học (Page) cần viết
    private String language = "Vietnamese"; // Mặc định là tiếng Việt
    private String additionalInstructions; // VD: "Viết ngắn gọn", "Thêm nhiều ví dụ thực tế"
}
