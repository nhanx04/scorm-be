package com.scorm.generator.dto.AI;

import lombok.Data;

@Data
public class GenerateQuizRequest {
    private String sourceText; // Đoạn văn bản tài liệu người dùng bôi đen hoặc cung cấp
    private int numberOfQuestions = 3; // Số lượng câu hỏi muốn tạo
    private String language = "Vietnamese";
    private String difficulty; // Độ khó: Dễ, Trung bình, Khó
}
