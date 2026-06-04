package com.scorm.generator.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;

import com.scorm.generator.dto.ai.AiCourseOutline;
import com.scorm.generator.dto.ai.GenerateCourseRequest;
import com.scorm.generator.dto.ai.AiPageContentResponse;
import com.scorm.generator.dto.ai.GeneratePageContentRequest;
import com.scorm.generator.dto.ai.AiKnowledgeAnswerResponse;
import com.scorm.generator.dto.ai.AiQuizResponse;
import com.scorm.generator.dto.ai.AskKnowledgeRequest;
import com.scorm.generator.dto.ai.GenerateCourseQuizRequest;
import com.scorm.generator.dto.ai.GenerateQuizRequest;
import com.scorm.generator.service.ai.PageContentExampleLoader;
import com.scorm.generator.service.ai.QuizExampleLoader;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiGeneratorService {

        private final ChatClient chatClient;
        private final QuizExampleLoader quizExampleLoader;
        private final PageContentExampleLoader pageContentExampleLoader;
        private final boolean quizFewShotEnabled;
        private final boolean pageContentFewShotEnabled;

        public AiGeneratorService(
                        ChatClient.Builder chatClientBuilder,
                        QuizExampleLoader quizExampleLoader,
                        PageContentExampleLoader pageContentExampleLoader,
                        @Value("${app.ai.quiz.few-shot-enabled:true}") boolean quizFewShotEnabled,
                        @Value("${app.ai.page-content.few-shot-enabled:true}") boolean pageContentFewShotEnabled) {
                this.chatClient = chatClientBuilder
                                .defaultSystem(
                                                "Bạn là một chuyên gia thiết kế giáo trình e-learning (Instructional Designer) với 10 năm kinh nghiệm. "
                                                                + "Nhiệm vụ của bạn là xây dựng cấu trúc khóa học chi tiết, logic và hấp dẫn.")
                                .build();
                this.quizExampleLoader = quizExampleLoader;
                this.pageContentExampleLoader = pageContentExampleLoader;
                this.quizFewShotEnabled = quizFewShotEnabled;
                this.pageContentFewShotEnabled = pageContentFewShotEnabled;
        }

        private static String stripJsonFence(String raw) {
                return raw.replace("```json", "").replace("```", "").trim();
        }

        /**
         * Trích xuất văn bản từ File (PDF/Word) và tạo Dàn ý khóa học
         */
        public AiCourseOutline generateCourseOutlineFromFile(MultipartFile file, GenerateCourseRequest request)
                        throws Exception {
                // 1. GIAI ĐOẠN ĐỌC FILE VÀ TRÍCH XUẤT TEXT
                InputStream inputStream = file.getInputStream();
                InputStreamResource resource = new InputStreamResource(inputStream);

                // Khởi tạo TikaDocumentReader để đọc nội dung file
                TikaDocumentReader documentReader = new TikaDocumentReader(resource);
                List<Document> documents = documentReader.get();

                // Ghép tất cả văn bản lấy được thành một chuỗi context
                String documentContext = documents.stream()
                                .map(Document::getText)
                                .collect(Collectors.joining("\n"));

                // 2. Cấu hình Converter để hướng dẫn AI trả về đúng định dạng JSON
                BeanOutputConverter<AiCourseOutline> converter = new BeanOutputConverter<>(AiCourseOutline.class);
                String formatInstructions = converter.getFormat();

                // 3. Soạn Prompt dành riêng cho bài toán bám sát nội dung từ tài liệu
                String userPrompt = """
                                Hãy đọc, phân tích toàn bộ tài liệu chuyên môn dưới đây và biến nó thành một dàn ý khóa học e-learning chi tiết, có cấu trúc tốt.

                                NỘI DUNG TÀI LIỆU GỐC (Dùng làm nguồn kiến thức chính):
                                =================================
                                {documentContext}
                                =================================

                                THÔNG TIN CẤU HÌNH KHÓA HỌC:
                                - Tên khóa học mong muốn (Title): {courseTitle}
                                - Đối tượng học viên (Target Audience): {targetAudience}
                                - Trình độ hiện tại (Proficiency Level): {audienceProficiencyLevel}
                                - Thời lượng dự kiến (Duration): {duration}
                                - Ngôn ngữ đầu ra (Language): {language}
                                - Yêu cầu thêm: {additionalInstructions}

                                YÊU CẦU CHUYÊN MÔN:
                                1. Dựa trên nội dung tài liệu gốc, hãy tự động phân bổ số lượng chương (sections) và các bài học (topics) sao cho bao quát hết kiến thức, logic và khoa học.
                                2. Các ý chính, khái niệm quan trọng trong tài liệu phải được tách thành các bài học riêng biệt.
                                3. Đảm bảo luồng kiến thức đi từ cơ bản đến nâng cao.

                                YÊU CẦU ĐỊNH DẠNG:
                                1. Trả về kết quả CHÍNH XÁC theo định dạng JSON được yêu cầu.
                                2. KHÔNG thêm bất kỳ lời dẫn hay giải thích nào bên ngoài JSON.

                                {formatInstructions}
                                """;

                // 4. Gọi AI và truyền Data vào Prompt
                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("documentContext", documentContext)
                                                .param("courseTitle",
                                                                request.getCourseTitle() != null
                                                                                ? request.getCourseTitle()
                                                                                : "Tự động trích xuất từ tài liệu")
                                                .param("duration",
                                                                request.getDuration() != null ? request.getDuration()
                                                                                : "Tự động phân bổ")
                                                .param("language",
                                                                request.getLanguage() != null ? request.getLanguage()
                                                                                : "Vietnamese")
                                                .param("targetAudience",
                                                                request.getTargetAudience() != null
                                                                                ? request.getTargetAudience()
                                                                                : "Mọi đối tượng")
                                                .param("audienceProficiencyLevel",
                                                                request.getAudienceProficiencyLevel() != null
                                                                                ? request.getAudienceProficiencyLevel()
                                                                                : "Không xác định")
                                                .param("additionalInstructions",
                                                                request.getAdditionalInstructions() != null
                                                                                ? request.getAdditionalInstructions()
                                                                                : "Bám sát tài liệu gốc")
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                // 5. Xử lý chuỗi và Convert thành Object
                String jsonContent = stripJsonFence(rawResponse);
                return converter.convert(jsonContent);
        }

        /**
         * Gửi yêu cầu sang Gemini và nhận về Object Java (AiCourseOutline)
         */
        public AiCourseOutline generateCourseOutline(GenerateCourseRequest request) {
                // 1. Cấu hình Converter để hướng dẫn AI trả về đúng định dạng JSON của Class
                // Java
                BeanOutputConverter<AiCourseOutline> converter = new BeanOutputConverter<>(AiCourseOutline.class);
                String formatInstructions = converter.getFormat();

                // 2. Soạn Prompt (kịch bản nhắc) cập nhật theo cấu trúc mới
                String userPrompt = """
                                Hãy tạo một dàn ý khóa học chi tiết dựa trên các thông tin và ngữ cảnh sau:

                                THÔNG TIN KHÓA HỌC:
                                - Tên khóa học (Title): {courseTitle}
                                - Mô tả tổng quan (Description): {courseDescription}
                                - Thời lượng dự kiến (Duration): {duration}
                                - Ngôn ngữ đầu ra (Language): {language}

                                ĐỐI TƯỢNG & MỤC TIÊU:
                                - Đối tượng học viên (Target Audience): {targetAudience}
                                - Trình độ hiện tại (Proficiency Level): {audienceProficiencyLevel}
                                - Yêu cầu đầu vào (Prerequisites): {prerequisites}
                                - Mục tiêu đầu ra (Learning Outcomes): {learningOutcomes}

                                Yêu cầu thêm: {additionalInstructions}

                                YÊU CẦU CHUYÊN MÔN:
                                Dựa vào thời lượng dự kiến là "{duration}", hãy tự động phân bổ số lượng chương (sections) và nội dung bài học sao cho logic, khoa học và đáp ứng đúng mục tiêu đầu ra. Các chương cần phải liên kết chặt chẽ với nhau.

                                YÊU CẦU QUAN TRỌNG VỀ ĐỊNH DẠNG:
                                1. Trả về kết quả CHÍNH XÁC theo định dạng JSON được yêu cầu dưới đây.
                                2. KHÔNG thêm bất kỳ lời dẫn hay giải thích nào bên ngoài JSON.

                                {formatInstructions}
                                """;

                // 3. Gọi AI và lấy về chuỗi String thô (raw content)
                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("courseTitle",
                                                                request.getCourseTitle() != null
                                                                                ? request.getCourseTitle()
                                                                                : "Chưa xác định")
                                                .param("courseDescription",
                                                                request.getCourseDescription() != null
                                                                                ? request.getCourseDescription()
                                                                                : "Chưa xác định")
                                                .param("duration",
                                                                request.getDuration() != null ? request.getDuration()
                                                                                : "Tự động phân bổ")
                                                .param("language",
                                                                request.getLanguage() != null ? request.getLanguage()
                                                                                : "Vietnamese")
                                                .param("targetAudience",
                                                                request.getTargetAudience() != null
                                                                                ? request.getTargetAudience()
                                                                                : "Chưa xác định")
                                                .param("audienceProficiencyLevel",
                                                                request.getAudienceProficiencyLevel() != null
                                                                                ? request.getAudienceProficiencyLevel()
                                                                                : "Chưa xác định")
                                                .param("prerequisites",
                                                                request.getPrerequisites() != null
                                                                                ? request.getPrerequisites()
                                                                                : "Không có")
                                                .param("learningOutcomes",
                                                                request.getLearningOutcomes() != null
                                                                                ? request.getLearningOutcomes()
                                                                                : "Nắm vững kiến thức cơ bản")
                                                .param("additionalInstructions",
                                                                request.getAdditionalInstructions() != null
                                                                                ? request.getAdditionalInstructions()
                                                                                : "Không có")
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                // 4. Xử lý chuỗi JSON (Làm sạch Markdown nếu có)
                String jsonContent = stripJsonFence(rawResponse);

                // 5. Convert String sạch thành Object Java
                return converter.convert(jsonContent);
        }

        /**
         * API sinh nội dung chi tiết cho trang bài học (Page Content Generation)
         */
        public AiPageContentResponse generatePageContent(GeneratePageContentRequest request) {
                // 1. Cấu hình Converter
                BeanOutputConverter<AiPageContentResponse> converter = new BeanOutputConverter<>(
                                AiPageContentResponse.class);
                String formatInstructions = converter.getFormat();

                // 2. Soạn Prompt
                String userPrompt = """
                                Bạn là một chuyên gia thiết kế nội dung e-learning (Instructional Designer).
                                Hãy viết nội dung giảng dạy chi tiết cho một bài học (Page) dựa trên ngữ cảnh sau:
                                - Tên toàn bộ khóa học: {courseTopic}
                                - Thuộc chương (Section): {sectionTitle}
                                - Chủ đề bài học này (Topic): {pageTopic}
                                - Ngôn ngữ: {language}
                                - Yêu cầu thêm: {additionalInstructions}

                                YÊU CẦU VỀ NỘI DUNG (Trường htmlContent):
                                1. Trình bày bài học một cách sư phạm: Có đoạn mở đầu dẫn dắt, giải thích chi tiết các khái niệm, đưa ra ví dụ minh họa và tóm tắt ngắn ở cuối bài.
                                2. Giá trị của trường `htmlContent` PHẢI là chuỗi mã HTML hợp lệ để hiển thị trực tiếp trên trình duyệt.
                                3. CHỈ SỬ DỤNG các thẻ HTML cơ bản để định dạng: <h2>, <h3>, <p>, <ul>, <ol>, <li>, <strong>, <em>, blockquote.
                                4. TUYỆT ĐỐI KHÔNG sử dụng các thẻ <html>, <head>, <body>, <script>, <style>.

                                {fewShotExamples}

                                YÊU CẦU VỀ ĐỊNH DẠNG ĐẦU RA:
                                1. Trả về kết quả CHÍNH XÁC theo định dạng JSON được yêu cầu.
                                2. KHÔNG thêm bất kỳ lời dẫn hay giải thích nào bên ngoài block JSON.

                                {formatInstructions}
                                """;

                // 3. Gọi AI
                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("courseTopic",
                                                                request.getCourseTopic() != null
                                                                                ? request.getCourseTopic()
                                                                                : "Không xác định")
                                                .param("sectionTitle",
                                                                request.getSectionTitle() != null
                                                                                ? request.getSectionTitle()
                                                                                : "Không xác định")
                                                .param("pageTopic", request.getPageTopic())
                                                .param("language", request.getLanguage())
                                                .param("additionalInstructions",
                                                                request.getAdditionalInstructions() != null
                                                                                ? request.getAdditionalInstructions()
                                                                                : "Không có")
                                                .param("fewShotExamples", buildPageContentFewShotBlock())
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                // 4. Xử lý chuỗi và Convert
                String jsonContent = stripJsonFence(rawResponse);
                return converter.convert(jsonContent);
        }

        private String buildPageContentFewShotBlock() {
                if (!pageContentFewShotEnabled) {
                        return "";
                }
                String positive = pageContentExampleLoader.getPositiveExample();
                String negative = pageContentExampleLoader.getNegativeExample();
                if ((positive == null || positive.isBlank()) && (negative == null || negative.isBlank())) {
                        return "";
                }
                return """
                                VÍ DỤ THAM KHẢO (FEW-SHOT):
                                ----------------------------------------
                                %s
                                ----------------------------------------
                                %s
                                ----------------------------------------
                                """.formatted(positive == null ? "" : positive, negative == null ? "" : negative);
        }

        /**
         * API tạo bộ câu hỏi từ ngữ cảnh khóa học (đủ 6 loại câu hỏi)
         */
        public AiQuizResponse generateQuizFromText(GenerateQuizRequest request) {
                BeanOutputConverter<AiQuizResponse> converter = new BeanOutputConverter<>(AiQuizResponse.class);
                String formatInstructions = converter.getFormat();

                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(QuizPrompts.FROM_TEXT)
                                                .param("sourceText", request.getSourceText())
                                                .param("numberOfQuestions", request.getNumberOfQuestions())
                                                .param("difficulty",
                                                                request.getDifficulty() != null
                                                                                ? request.getDifficulty()
                                                                                : "Trung bình")
                                                .param("language",
                                                                request.getLanguage() != null ? request.getLanguage()
                                                                                : "Vietnamese")
                                                .param("fewShotExamples", buildQuizFewShotBlock())
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                String jsonContent = stripJsonFence(rawResponse);
                return converter.convert(jsonContent);
        }

        private String buildQuizFewShotBlock() {
                if (!quizFewShotEnabled) {
                        return "";
                }
                String all = quizExampleLoader.formatAllExamples();
                if (all == null || all.isBlank()) {
                        return "";
                }
                return """
                                DƯỚI ĐÂY LÀ CÁC VÍ DỤ MẪU CHO TỪNG LOẠI CÂU HỎI (FEW-SHOT). HÃY HỌC ĐÚNG SHAPE CỦA correctAnswer THEO TỪNG LOẠI:
                                ========================================================
                                %s
                                ========================================================
                                """.formatted(all);
        }

        public AiQuizResponse generateCourseAwareQuiz(GenerateCourseQuizRequest request) {
                BeanOutputConverter<AiQuizResponse> converter = new BeanOutputConverter<>(AiQuizResponse.class);
                String formatInstructions = converter.getFormat();

                String userPrompt = """
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
                                3. FILL_IN_THE_BLANK BẮT BUỘC có cả 3 trường: prompt (chỉ dẫn ngắn cho học viên, không được để trống), sentenceHtml (câu có chỗ trống), correctAnswer (mảng đáp án).
                                4. explanation BẮT BUỘC mở đầu bằng "Theo tài liệu:" hoặc "Slide X:" và trích ngữ cảnh từ nội dung bài học (không phải kiến thức chung).

                                {fewShotExamples}

                                Chỉ trả về JSON hợp lệ theo schema:
                                {formatInstructions}
                                """;

                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("courseTitle",
                                                                request.getCourseTitle() != null
                                                                                ? request.getCourseTitle()
                                                                                : "")
                                                .param("courseDescription",
                                                                request.getCourseDescription() != null
                                                                                ? request.getCourseDescription()
                                                                                : "")
                                                .param("sectionTitle",
                                                                request.getSectionTitle() != null
                                                                                ? request.getSectionTitle()
                                                                                : "")
                                                .param("pageTitle",
                                                                request.getPageTitle() != null ? request.getPageTitle()
                                                                                : "")
                                                .param("sourceText",
                                                                request.getSourceText() != null
                                                                                ? request.getSourceText()
                                                                                : "")
                                                .param("numberOfQuestions", request.getNumberOfQuestions())
                                                .param("difficulty",
                                                                request.getDifficulty() != null
                                                                                ? request.getDifficulty()
                                                                                : "Trung bình")
                                                .param("language",
                                                                request.getLanguage() != null ? request.getLanguage()
                                                                                : "Vietnamese")
                                                .param("fewShotExamples", buildQuizFewShotBlock())
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                String jsonContent = stripJsonFence(rawResponse);
                return converter.convert(jsonContent);
        }

        public AiKnowledgeAnswerResponse askCourseKnowledge(AskKnowledgeRequest request) {
                BeanOutputConverter<AiKnowledgeAnswerResponse> converter = new BeanOutputConverter<>(
                                AiKnowledgeAnswerResponse.class);
                String formatInstructions = converter.getFormat();

                String userPrompt = """
                                Bạn là trợ giảng AI cho khóa học. Chỉ được trả lời dựa trên ngữ cảnh khóa học và nội dung bài học cung cấp.

                                NGỮ CẢNH:
                                - courseTitle: {courseTitle}
                                - courseDescription: {courseDescription}
                                - sectionTitle: {sectionTitle}
                                - pageTitle: {pageTitle}
                                - pageContent: {pageContent}

                                CÂU HỎI NGƯỜI HỌC:
                                {question}

                                QUY TẮC:
                                1. Nếu đủ dữ liệu trong ngữ cảnh, trả lời ngắn gọn, rõ ràng, đúng trọng tâm.
                                2. Nếu không đủ dữ liệu, trả lời rằng chưa đủ thông tin trong nội dung khóa học hiện tại và gợi ý người dùng bổ sung nội dung.
                                3. Không bịa thông tin ngoài nội dung đã cung cấp.
                                4. Ngôn ngữ trả lời: {language}.

                                Trả về đúng JSON theo schema:
                                {formatInstructions}
                                """;

                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("courseTitle",
                                                                request.getCourseTitle() != null
                                                                                ? request.getCourseTitle()
                                                                                : "")
                                                .param("courseDescription",
                                                                request.getCourseDescription() != null
                                                                                ? request.getCourseDescription()
                                                                                : "")
                                                .param("sectionTitle",
                                                                request.getSectionTitle() != null
                                                                                ? request.getSectionTitle()
                                                                                : "")
                                                .param("pageTitle",
                                                                request.getPageTitle() != null ? request.getPageTitle()
                                                                                : "")
                                                .param("pageContent",
                                                                request.getPageContent() != null
                                                                                ? request.getPageContent()
                                                                                : "")
                                                .param("question",
                                                                request.getQuestion() != null ? request.getQuestion()
                                                                                : "")
                                                .param("language",
                                                                request.getLanguage() != null ? request.getLanguage()
                                                                                : "Vietnamese")
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                String jsonContent = stripJsonFence(rawResponse);
                return converter.convert(jsonContent);
        }
}