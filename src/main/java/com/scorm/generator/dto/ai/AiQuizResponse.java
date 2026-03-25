package com.scorm.generator.dto.ai;

import java.util.List;

public record AiQuizResponse(
                List<AiQuestion> questions) {

        public record AiQuestion(
                        String type, // Loại câu hỏi (VD: Multiple Choice, Matching, Fill Blank...)
                        String prompt, // Nội dung câu hỏi (tương đương questionText ở nhánh cũ)
                        List<String> options, // Danh sách các lựa chọn đáp án
                        Object correctAnswer, // Đáp án đúng (dùng Object để linh hoạt kiểu dữ liệu)
                        String explanation, // Giải thích đáp án
                        String sentenceHtml, // Dành cho câu hỏi điền khuyết
                        List<MatchingPair> pairs // Dành cho câu hỏi nối chéo (Matching)
        ) {
        }

        public record MatchingPair(
                        String left,
                        String right) {
        }
}