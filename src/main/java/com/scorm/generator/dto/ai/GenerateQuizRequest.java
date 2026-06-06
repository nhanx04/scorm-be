package com.scorm.generator.dto.ai;

import lombok.Data;

@Data
public class GenerateQuizRequest {
    private String sourceText; // Đoạn văn bản tài liệu người dùng bôi đen hoặc cung cấp
    private int numberOfQuestions = 3; // Số lượng câu hỏi muốn tạo
    private String language = "auto"; // "auto" = theo ngôn ngữ của nguồn (tài liệu/nội dung nhập)
    private String difficulty; // Độ khó: Dễ, Trung bình, Khó
}
