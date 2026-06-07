package com.scorm.generator.dto.ai;

import java.util.List;

public record AiQuizResponse(
                List<AiQuestion> questions) {

        public record AiQuestion(
                        String type, // Loại câu hỏi (VD: Multiple Choice, Matching, Fill Blank...)
                        String prompt, // Nội dung câu hỏi (tương đương questionText ở nhánh cũ)
                        List<String> options, // Danh sách các lựa chọn đáp án
                        Object correctAnswer, // Đáp án đúng (dùng Object để linh hoạt kiểu dữ liệu)
                        Citation citation, // Bằng chứng từ tài liệu — server có thể verify substring
                        String sentenceHtml, // Dành cho câu hỏi điền khuyết
                        List<MatchingPair> pairs, // Dành cho câu hỏi nối chéo (Matching)
                        Integer bloomLevel // Mức Bloom Taxonomy 1-6 (đo/kiểm soát độ khó câu hỏi)
        ) {
        }

        /**
         * Structured citation evidence backing an AI-generated quiz answer.
         *
         * <p>The model MUST fill {@code verbatimQuote} with text that exists
         * character-for-character in the source document. The server validates
         * this with a substring check — a deterministic, no-LLM anti-hallucination
         * gate that replaces the earlier free-form {@code explanation} string.
         */
        public record Citation(
                        String sourceLocation, // ví dụ "Slide 8" hoặc "Đoạn 3"
                        String verbatimQuote, // Trích nguyên văn từ nguồn, ≤ 50 từ
                        String reasoning // 1 câu ≤ 20 từ giải thích vì sao quote này hỗ trợ đáp án đúng
        ) {
        }

        public record MatchingPair(
                        String left,
                        String right) {
        }
}
