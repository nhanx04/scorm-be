package com.scorm.generator.dto.ai;

import lombok.Data;

@Data
public class GeneratePageContentRequest {
    private Long courseId; // Để backend nạp tài liệu gốc của khóa học làm nguồn dữ kiện (grounding)
    private String courseTopic; // Chủ đề của cả khóa học (để AI hiểu ngữ cảnh)
    private String sectionTitle; // Tên chương chứa bài học này
    private String pageTopic; // Chủ đề chính của bài học (Page) cần viết
    private String language = "auto"; // "auto" = theo ngôn ngữ của tài liệu/chủ đề nguồn
    private String additionalInstructions; // VD: "Viết ngắn gọn", "Thêm nhiều ví dụ thực tế"
    private String sourceDocumentText; // Backend tự nạp từ tài liệu gốc của khóa học (không phải frontend gửi)
}
