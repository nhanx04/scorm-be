package com.scorm.generator.service;

/**
 * Single source of truth for quiz generation prompt templates.
 *
 * <p>Kept separate so that the evaluation harness in
 * {@code src/test/java/.../eval} can reference the exact same template
 * the production endpoints use — without copy-pasting it (which is how
 * eval/prod drift gets introduced).
 *
 * <p>The templates use the standard Spring AI {@code {name}} placeholder
 * syntax; substitute via {@code .user(u -> u.text(TEMPLATE).param(...))}.
 */
public final class QuizPrompts {

    private QuizPrompts() {
    }

    /** Quiz generated from raw source text (no course context). */
    public static final String FROM_TEXT = """
            Bạn là chuyên gia thiết kế kiểm tra đánh giá e-learning.
            Dựa hoàn toàn vào tài liệu học tập dưới đây, hãy tạo đúng {numberOfQuestions} câu hỏi với độ khó {difficulty}.

            TÀI LIỆU GỐC:
            "{sourceText}"

            RÀNG BUỘC NGHIÊM NGẶT VỀ NGUỒN (anti-hallucination):
            1. MỌI câu hỏi PHẢI có thể trả lời được CHỈ bằng nội dung trong "TÀI LIỆU GỐC" ở trên.
            2. TUYỆT ĐỐI KHÔNG sử dụng kiến thức chung từ training data nằm ngoài tài liệu, kể cả khi kiến thức đó là chính xác.
            3. Trước khi tạo mỗi câu, hãy tự kiểm tra: "Đáp án đúng có trích dẫn được TRỰC TIẾP từ tài liệu gốc không?" Nếu không → loại bỏ câu hỏi đó, viết câu khác.
            4. Nếu tài liệu quá ngắn để có đủ {numberOfQuestions} câu hỏi chất lượng, hãy tạo ít câu hơn thay vì bịa.

            BẮT BUỘC:
            1. Sử dụng đầy đủ 6 loại câu hỏi: MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE, SHORT_ANSWER, FILL_IN_THE_BLANK, MATCHING.
            2. Nếu {numberOfQuestions} >= 6, phải có ít nhất 1 câu cho mỗi loại.
            3. Ngôn ngữ đầu ra: {language}.

            QUY ƯỚC DỮ LIỆU:
            - MCQ_SINGLE/MCQ_MULTIPLE: có options và correctAnswer tương ứng (string hoặc mảng string).
            - TRUE_FALSE: correctAnswer là boolean.
            - SHORT_ANSWER: correctAnswer là mảng câu trả lời ngắn chấp nhận được.
            - FILL_IN_THE_BLANK: BẮT BUỘC có đủ 3 trường:
                + prompt: chỉ dẫn ngắn cho học viên (ví dụ: "Điền vào chỗ trống dựa trên đoạn dưới đây:"). KHÔNG được để trống dù sentenceHtml đã rõ.
                + sentenceHtml: câu chứa các chỗ trống dạng <input> hoặc ___(N)___.
                + correctAnswer: mảng đáp án theo thứ tự các chỗ trống.
            - MATCHING: có pairs (left-right), correctAnswer có thể để null.
            - explanation BẮT BUỘC mở đầu bằng "Theo tài liệu:" hoặc "Slide X:" và trích dẫn ngữ cảnh cụ thể trong tài liệu gốc (không phải kiến thức chung).

            {fewShotExamples}

            CHỈ trả về JSON đúng schema sau:
            {formatInstructions}
            """;
}
