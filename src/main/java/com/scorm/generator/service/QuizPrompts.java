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
 *
 * <p>Both templates share three blocks (anti-hallucination, schema
 * conventions, IWF-aware item-writing rules) defined as private constants
 * and interpolated via {@link String#formatted}. Edit a rule in one place
 * and both prompts pick it up.
 */
public final class QuizPrompts {

    private QuizPrompts() {
    }

    // -----------------------------------------------------------------
    // Shared rule blocks. Each block is a self-contained fragment; the
    // {sourceLabel} placeholder is filled in per-template.
    // -----------------------------------------------------------------

    private static final String ANTI_HALLUCINATION = """
            RÀNG BUỘC NGHIÊM NGẶT VỀ NGUỒN (anti-hallucination):
            1. MỌI câu hỏi PHẢI có thể trả lời được CHỈ bằng nội dung trong "%s" ở trên.
            2. TUYỆT ĐỐI KHÔNG sử dụng kiến thức chung từ training data nằm ngoài tài liệu, kể cả khi kiến thức đó là chính xác.
            3. Trước khi tạo mỗi câu, hãy tự kiểm tra: "Đáp án đúng có trích dẫn được TRỰC TIẾP từ nguồn không?" Nếu không → loại bỏ câu hỏi đó, viết câu khác.
            4. Nếu nguồn quá ngắn để có đủ {numberOfQuestions} câu hỏi chất lượng, hãy tạo ít câu hơn thay vì bịa.
            """;

    private static final String SCHEMA_CONVENTIONS = """
            QUY ƯỚC DỮ LIỆU (áp dụng cho TẤT CẢ loại câu hỏi):
            - `type` và `prompt` BẮT BUỘC không rỗng cho MỌI loại — kể cả khi đã có sentenceHtml/options/pairs.
              `prompt` là chỉ dẫn hiển thị cho học viên (ví dụ "Chọn đáp án đúng:", "Đúng hay sai:", "Điền vào chỗ trống:").
            - `citation` BẮT BUỘC có đủ 3 trường:
                + sourceLocation: "Slide <N>" nếu nguồn chia slide, hoặc "Đoạn <N>" cho văn bản liên tục.
                + verbatimQuote: chuỗi TRÍCH NGUYÊN VĂN từ nguồn — NGẮN (5-20 từ), LIỀN MẠCH trong 1 câu duy nhất.
                  TUYỆT ĐỐI KHÔNG:
                    × dùng dấu "..." để nối nhiều đoạn không liền mạch;
                    × ghép nhiều bullet (• -) thành 1 quote;
                    × paraphrase, dịch, thêm/xoá dấu câu.
                  Server SẼ tự kiểm tra bằng substring match — quote không tìm thấy chữ-cho-chữ sẽ bị reject.
                + reasoning: 1 câu (≤ 20 từ) giải thích vì sao quote này hỗ trợ đáp án đúng.
                  Reasoning PHẢI cùng ngôn ngữ với {language} — KHÔNG mix.

            QUY ƯỚC PER-TYPE:
            - MCQ_SINGLE/MCQ_MULTIPLE: có options và correctAnswer tương ứng (string hoặc mảng string).
            - TRUE_FALSE: correctAnswer là boolean.
            - SHORT_ANSWER: correctAnswer là mảng câu trả lời ngắn chấp nhận được.
            - FILL_IN_THE_BLANK: ngoài `prompt`, còn cần sentenceHtml (câu chứa <input> hoặc ___(N)___) và correctAnswer là mảng đáp án theo thứ tự chỗ trống.
            - MATCHING: có pairs (left-right), correctAnswer có thể để null.
            """;

    private static final String ITEM_WRITING_STANDARDS = """
            QUY TẮC CHẤT LƯỢNG ĐÁP ÁN (Item Writing Standards, dựa trên Haladyna et al. 2002):
            - Distractor (đáp án sai trong MCQ) phải có độ dài tương đương đáp án đúng (chênh lệch ≤ 30%% số từ).
            - KHÔNG dùng từ tuyệt đối ("luôn luôn", "không bao giờ", "tất cả", "duy nhất", "always", "never", "only")
              trong distractor — học viên sẽ loại được mà không cần kiến thức.
            - KHÔNG dùng các option dạng "All of the above" / "None of the above" / "Tất cả đều đúng".
            - Stem (trường `prompt`) ≤ 25 từ, không chứa thông tin thừa ngoài câu hỏi.
            - Câu hỏi phủ định BẮT BUỘC viết hoa từ phủ định ("NOT", "KHÔNG", "EXCEPT") để học viên không bỏ sót.
            """;

    private static final String DIFFICULTY_RUBRIC = """
            QUY ĐỊNH ĐỘ KHÓ (rubric, mapped sang Bloom Taxonomy):
            - Dễ:        Bloom L1-L2 (Remember / Understand) — nhận biết hoặc nhớ định nghĩa trực tiếp từ nguồn.
            - Trung bình: Bloom L3 (Apply) — áp dụng khái niệm vào tình huống cụ thể, ví dụ minh họa, so sánh nhẹ.
            - Khó:       Bloom L4-L5 (Analyze / Evaluate) — phân tích thành phần, đánh giá đa khái niệm, suy luận.

            PHÂN BỔ ĐỘ KHÓ (khi {difficulty} là "Mixed" hoặc khi {numberOfQuestions} >= 5):
            Mặc định 1 câu Dễ + 3 câu Trung bình + 1 câu Khó cho mỗi 5 câu. Điều chỉnh tỉ lệ tương ứng cho N khác.
            Nếu {difficulty} chỉ định một mức (Dễ/Trung bình/Khó), tất cả câu cùng mức đó.
            """;

    private static final String CONCEPT_DIVERSITY = """
            QUY TRÌNH SINH CÂU HỎI (làm theo đúng thứ tự):
            Bước 1: Liệt kê (trong suy nghĩ) {numberOfQuestions} KHÁI NIỆM CỐT LÕI khác nhau xuất hiện trong nguồn —
                    mỗi khái niệm phải có ít nhất 1 câu/đoạn trong nguồn trực tiếp đề cập.
            Bước 2: Với mỗi khái niệm, tạo đúng 1 câu hỏi target khái niệm đó. KHÔNG tạo 2 câu cùng khái niệm.
            Bước 3: Phân bổ N câu vào 6 loại (MCQ_SINGLE/MCQ_MULTIPLE/TRUE_FALSE/SHORT_ANSWER/FILL_IN_THE_BLANK/MATCHING)
                    sao cho cân đối — không lặp loại quá 2 lần nếu N <= 6.
            Bước 4: Với mỗi câu, chọn quote nguyên văn ngắn (≤ 50 từ) làm `citation.verbatimQuote`.
            """;

    // Spring AI's prompt template parser treats { and } as placeholder
    // delimiters — anti-examples cannot use literal JSON braces. We describe
    // the bad outputs in prose instead.
    private static final String NEGATIVE_EXAMPLES = """
            ❌ TRÁNH CÁC LỖI SAU (negative examples — dạng KHÔNG được làm):

            ❌ BAD #1 — verbatimQuote bị paraphrase hoặc dùng dấu "..." nối nhiều đoạn:
              VÍ DỤ SAI: citation.verbatimQuote = "Binary Search ... Time complexity: O(log n)"
              LÝ DO SAI: server check substring sẽ fail vì cụm "..." không có trong nguồn.
              ĐÚNG: chọn 1 đoạn LIỀN MẠCH trong nguồn, ví dụ "Binary Search has time complexity O(log n)".

            ❌ BAD #2 — FILL_IN_THE_BLANK không có trường prompt (chỉ có sentenceHtml):
              VÍ DỤ SAI: type = "FILL_IN_THE_BLANK", prompt = "", sentenceHtml = "<p>Tầng 2 OSI là ___</p>"
              LÝ DO SAI: prompt BẮT BUỘC không rỗng cho MỌI loại.
              ĐÚNG: prompt = "Điền vào chỗ trống tên tầng OSI." + sentenceHtml như cũ.

            ❌ BAD #3 — distractor MCQ chứa từ tuyệt đối ("luôn", "không bao giờ"):
              VÍ DỤ SAI: options = ["Đáp án đúng", "Không bao giờ xảy ra", "Luôn luôn đúng", "Tất cả"]
              LÝ DO SAI: học viên sẽ loại 3 distractor mà không cần kiến thức (testwiseness flaw TW-5).
              ĐÚNG: cả 4 option viết theo dạng claim trung lập, không chứa từ tuyệt đối.
            """;

    // -----------------------------------------------------------------
    // Public templates
    // -----------------------------------------------------------------

    /** Quiz generated from raw source text (no course context). */
    public static final String FROM_TEXT = """
            Bạn là chuyên gia thiết kế kiểm tra đánh giá e-learning.
            Dựa hoàn toàn vào tài liệu học tập dưới đây, hãy tạo đúng {numberOfQuestions} câu hỏi với độ khó {difficulty}.

            TÀI LIỆU GỐC:
            "{sourceText}"

            %s
            %s
            BẮT BUỘC:
            1. Sử dụng đầy đủ 6 loại câu hỏi: MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE, SHORT_ANSWER, FILL_IN_THE_BLANK, MATCHING.
            2. Nếu {numberOfQuestions} >= 6, phải có ít nhất 1 câu cho mỗi loại.
            3. Ngôn ngữ đầu ra: {language}.

            %s
            %s
            %s
            %s
            {fewShotExamples}

            CHỈ trả về JSON đúng schema sau:
            {formatInstructions}
            """.formatted(
                    ANTI_HALLUCINATION.formatted("TÀI LIỆU GỐC"),
                    CONCEPT_DIVERSITY,
                    SCHEMA_CONVENTIONS,
                    ITEM_WRITING_STANDARDS,
                    DIFFICULTY_RUBRIC,
                    NEGATIVE_EXAMPLES);

    /**
     * Quiz grounded on the course's source document, focused on a user-supplied
     * topic. The document is the ONLY source of facts (anti-hallucination still
     * applies); {focusTopic} only narrows WHICH parts of the document to test.
     */
    public static final String FROM_DOCUMENT_FOCUSED = """
            Bạn là chuyên gia thiết kế kiểm tra đánh giá e-learning.
            Người học muốn ra đề xoay quanh CHỦ ĐỀ TRỌNG TÂM dưới đây, nhưng MỌI câu hỏi và đáp án PHẢI lấy dữ kiện từ TÀI LIỆU GỐC.

            CHỦ ĐỀ TRỌNG TÂM (do người dùng nhập):
            "{focusTopic}"

            TÀI LIỆU GỐC (nguồn dữ kiện DUY NHẤT):
            "{sourceText}"

            Hãy tạo đúng {numberOfQuestions} câu hỏi với độ khó {difficulty}, tập trung vào CHỦ ĐỀ TRỌNG TÂM.

            %s
            QUY TẮC TRỌNG TÂM:
            1. Chỉ chọn những phần của TÀI LIỆU GỐC có liên quan tới CHỦ ĐỀ TRỌNG TÂM để ra đề.
            2. Nếu TÀI LIỆU GỐC không đề cập (hoặc đề cập rất ít) tới CHỦ ĐỀ TRỌNG TÂM,
               hãy tạo ÍT câu hơn — TUYỆT ĐỐI KHÔNG bịa kiến thức ngoài tài liệu để lấp đầy.
            %s
            BẮT BUỘC:
            1. Sử dụng đa dạng 6 loại câu hỏi: MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE, SHORT_ANSWER, FILL_IN_THE_BLANK, MATCHING.
            2. Nếu {numberOfQuestions} >= 6, phải có ít nhất 1 câu cho mỗi loại (trừ khi nguồn không đủ dữ kiện).
            3. Ngôn ngữ đầu ra: {language}.

            %s
            %s
            %s
            %s
            {fewShotExamples}

            CHỈ trả về JSON đúng schema sau:
            {formatInstructions}
            """.formatted(
                    ANTI_HALLUCINATION.formatted("TÀI LIỆU GỐC"),
                    CONCEPT_DIVERSITY,
                    SCHEMA_CONVENTIONS,
                    ITEM_WRITING_STANDARDS,
                    DIFFICULTY_RUBRIC,
                    NEGATIVE_EXAMPLES);

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

            %s
            %s
            YÊU CẦU:
            1. Tạo đúng {numberOfQuestions} câu, độ khó {difficulty}, ngôn ngữ {language}.
            2. Bao phủ đầy đủ 6 loại câu hỏi: MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE, SHORT_ANSWER, FILL_IN_THE_BLANK, MATCHING.

            %s
            %s
            %s
            %s
            {fewShotExamples}

            Chỉ trả về JSON hợp lệ theo schema:
            {formatInstructions}
            """.formatted(
                    ANTI_HALLUCINATION.formatted("NGUỒN NỘI DUNG BÀI HỌC"),
                    CONCEPT_DIVERSITY,
                    SCHEMA_CONVENTIONS,
                    ITEM_WRITING_STANDARDS,
                    DIFFICULTY_RUBRIC,
                    NEGATIVE_EXAMPLES);
}
