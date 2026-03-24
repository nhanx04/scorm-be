package com.scorm.generator.dto.AI;

import java.util.List;

public record AiQuizResponse(
        List<AiQuestion> questions) {
    public record AiQuestion(
            String questionText, // Nội dung câu hỏi
            List<String> options, // Danh sách 4 đáp án
            String correctAnswer, // Đáp án đúng (phải khớp exatcly với 1 item trong options)
            String explanation // Giải thích vì sao đáp án này đúng
    ) {
    }
}