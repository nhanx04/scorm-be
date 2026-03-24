package com.scorm.generator.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import com.scorm.generator.dto.AI.AiCourseOutline;
import com.scorm.generator.dto.AI.GenerateCourseRequest;
import com.scorm.generator.dto.AI.AiPageContentResponse;
import com.scorm.generator.dto.AI.GeneratePageContentRequest;
import com.scorm.generator.dto.AI.AiQuizResponse;
import com.scorm.generator.dto.AI.GenerateQuizRequest;

@Service
public class AiGeneratorService {

        private final ChatClient chatClient;

        public AiGeneratorService(ChatClient.Builder chatClientBuilder) {
                this.chatClient = chatClientBuilder
                                .defaultSystem(
                                                "Bạn là một chuyên gia thiết kế giáo trình e-learning (Instructional Designer) với 10 năm kinh nghiệm. "
                                                                + "Nhiệm vụ của bạn là xây dựng cấu trúc khóa học chi tiết, logic và hấp dẫn.")
                                .build();
        }

        /**
         * Gửi yêu cầu sang Gemini và nhận về Object Java (AiCourseOutline)
         */
        public AiCourseOutline generateCourseOutline(GenerateCourseRequest request) {
                // 1. Cấu hình Converter để hướng dẫn AI trả về đúng định dạng JSON của Class
                // Java
                BeanOutputConverter<AiCourseOutline> converter = new BeanOutputConverter<>(AiCourseOutline.class);
                String formatInstructions = converter.getFormat();

                // 2. Soạn Prompt (kịch bản nhắc)
                String userPrompt = """
                                Hãy tạo một dàn ý khóa học chi tiết dựa trên các thông tin sau:
                                - Chủ đề: {topic}
                                - Đối tượng học viên: {targetAudience}
                                - Ngôn ngữ đầu ra: {language}
                                - Số lượng chương (sections) mong muốn: khoảng {numberOfSections}
                                - Yêu cầu thêm: {additionalInstructions}

                                Yêu cầu quan trọng về định dạng:
                                1. Trả về kết quả CHÍNH XÁC theo định dạng JSON được yêu cầu dưới đây.
                                2. KHÔNG thêm bất kỳ lời dẫn hay giải thích nào bên ngoài JSON.

                                {formatInstructions}
                                """;

                // 3. Gọi AI và lấy về chuỗi String thô (raw content)
                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("topic", request.getTopic())
                                                .param("targetAudience", request.getTargetAudience())
                                                .param("language", request.getLanguage())
                                                .param("numberOfSections", request.getNumberOfSections())
                                                .param("additionalInstructions",
                                                                request.getAdditionalInstructions() != null
                                                                                ? request.getAdditionalInstructions()
                                                                                : "Không có")
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                // 4. Xử lý chuỗi JSON (Làm sạch Markdown nếu có)
                String jsonContent = rawResponse.replace("```json", "").replace("```", "").trim();

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
                                3. CHỈ SỬ DỤNG các thẻ HTML cơ bản để định dạng: <h2>, <h3>, <p>, <ul>, <ol>, <li>, <strong>, <em>, <blockquote>.
                                4. TUYỆT ĐỐI KHÔNG sử dụng các thẻ <html>, <head>, <body>, <script>, <style>.

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
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                // 4. Xử lý chuỗi và Convert
                String jsonContent = rawResponse.replace("```json", "").replace("```", "").trim();
                return converter.convert(jsonContent);
        }

        /**
         * API tạo bộ câu hỏi trắc nghiệm từ văn bản (Quiz Generation)
         */
        public AiQuizResponse generateQuizFromText(GenerateQuizRequest request) {
                // 1. Cấu hình Converter
                BeanOutputConverter<AiQuizResponse> converter = new BeanOutputConverter<>(AiQuizResponse.class);
                String formatInstructions = converter.getFormat();

                // 2. Soạn Prompt
                String userPrompt = """
                                Bạn là một chuyên gia giáo dục đánh giá năng lực học viên.
                                Dựa vào đoạn tài liệu học tập dưới đây, hãy tạo ra {numberOfQuestions} câu hỏi trắc nghiệm (Multiple Choice) ở mức độ {difficulty}.

                                TÀI LIỆU GỐC:
                                "{sourceText}"

                                YÊU CẦU:
                                1. Mỗi câu hỏi phải có chính xác 4 đáp án lựa chọn (options).
                                2. Chỉ có 1 đáp án đúng duy nhất (correctAnswer) và đáp án này phải nằm chính xác trong danh sách options.
                                3. Cung cấp lời giải thích ngắn gọn, dễ hiểu cho đáp án đúng (explanation) dựa vào tài liệu gốc.
                                4. Viết bằng ngôn ngữ: {language}.

                                ĐỊNH DẠNG TRẢ VỀ:
                                1. Trả về đúng cấu trúc JSON được yêu cầu.
                                2. Không giải thích gì thêm ngoài block JSON.

                                {formatInstructions}
                                """;

                // 3. Gọi AI
                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("sourceText", request.getSourceText())
                                                .param("numberOfQuestions", request.getNumberOfQuestions())
                                                .param("difficulty",
                                                                request.getDifficulty() != null
                                                                                ? request.getDifficulty()
                                                                                : "Trung bình")
                                                .param("language",
                                                                request.getLanguage() != null ? request.getLanguage()
                                                                                : "Vietnamese")
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                // 4. Xử lý chuỗi và Convert
                String jsonContent = rawResponse.replace("```json", "").replace("```", "").trim();
                return converter.convert(jsonContent);
        }
}