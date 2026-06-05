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

            QUY ƯỚC DỮ LIỆU (áp dụng cho TẤT CẢ loại câu hỏi):
            - `type` và `prompt` BẮT BUỘC không rỗng cho MỌI loại — kể cả khi đã có sentenceHtml/options/pairs.
              `prompt` là chỉ dẫn hiển thị cho học viên (ví dụ "Chọn đáp án đúng:", "Đúng hay sai:", "Điền vào chỗ trống:").
            - explanation BẮT BUỘC mở đầu bằng "Theo tài liệu:" hoặc "Slide X:" và trích dẫn ngữ cảnh cụ thể trong tài liệu gốc (không phải kiến thức chung).

            QUY ƯỚC PER-TYPE:
            - MCQ_SINGLE/MCQ_MULTIPLE: có options và correctAnswer tương ứng (string hoặc mảng string).
            - TRUE_FALSE: correctAnswer là boolean.
            - SHORT_ANSWER: correctAnswer là mảng câu trả lời ngắn chấp nhận được.
            - FILL_IN_THE_BLANK: ngoài `prompt`, còn cần sentenceHtml (câu chứa <input> hoặc ___(N)___) và correctAnswer là mảng đáp án theo thứ tự chỗ trống.
            - MATCHING: có pairs (left-right), correctAnswer có thể để null.

            {fewShotExamples}

            CHỈ trả về JSON đúng schema sau:
            {formatInstructions}
            """;

    /** Quiz generated with full course/section/page context (course-aware). */
    public static final String COURSE_AWARE = """
            Bạn là trợ lý học tập cho khóa học dưới đây. Hãy tạo câu hỏi kiểm tra chỉ dựa trên ngữ cảnh được cung cấp.

            THÔNG TIN KHÓA HỌC:
            - courseTitle: {courseTitle}
            - courseDescription: {courseDescription}
            - sectionTitle: {sectionTitle}
            - pageTitle: {pageTitle}

            NGUỒN NỘI DUNG BÀI HỌC:
            "{sourceText}"

            RÀNG BUỘC NGHIÊM NGẶT VỀ NGUỒN (anti-hallucination):
            1. MỌI câu hỏi PHẢI có thể trả lời được CHỈ bằng "NGUỒN NỘI DUNG BÀI HỌC" ở trên.
            2. TUYỆT ĐỐI KHÔNG sử dụng kiến thức chung từ training data nằm ngoài nội dung bài học.
            3. Trước khi tạo mỗi câu, tự kiểm tra: "Đáp án đúng có trích dẫn được TRỰC TIẾP từ nội dung bài học không?" Nếu không → loại bỏ.
            4. Nếu nội dung bài học quá ngắn để có đủ {numberOfQuestions} câu, tạo ít câu hơn thay vì bịa.

            YÊU CẦU:
            1. Tạo đúng {numberOfQuestions} câu, độ khó {difficulty}, ngôn ngữ {language}.
            2. Bao phủ đầy đủ 6 loại câu hỏi: MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE, SHORT_ANSWER, FILL_IN_THE_BLANK, MATCHING.

            QUY ƯỚC DỮ LIỆU (áp dụng cho TẤT CẢ loại câu hỏi):
            - `type` và `prompt` BẮT BUỘC không rỗng cho MỌI loại.
              `prompt` là chỉ dẫn hiển thị cho học viên (ví dụ "Chọn đáp án đúng:", "Đúng hay sai:", "Điền vào chỗ trống:").
            - explanation BẮT BUỘC mở đầu bằng "Theo tài liệu:" hoặc "Slide X:" và trích ngữ cảnh từ nội dung bài học (không phải kiến thức chung).

            QUY ƯỚC PER-TYPE:
            - MCQ_SINGLE/MCQ_MULTIPLE: có options và correctAnswer tương ứng.
            - TRUE_FALSE: correctAnswer là boolean.
            - SHORT_ANSWER: correctAnswer là mảng câu trả lời ngắn chấp nhận được.
            - FILL_IN_THE_BLANK: thêm sentenceHtml và correctAnswer là mảng đáp án theo thứ tự chỗ trống.
            - MATCHING: có pairs (left-right), correctAnswer có thể null.

            {fewShotExamples}

            Chỉ trả về JSON hợp lệ theo schema:
            {formatInstructions}
            """;
}
