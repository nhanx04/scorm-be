package com.scorm.generator.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import com.scorm.generator.dto.AI.AiCourseOutline;
import com.scorm.generator.dto.AI.GenerateCourseRequest;

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
                                                .param("formatInstructions", formatInstructions)) // Truyền hướng dẫn
                                                                                                  // format JSON vào
                                                                                                  // prompt
                                .call()
                                .content(); // Lấy về String thay vì ép kiểu ngay

                // 4. Xử lý chuỗi JSON (Làm sạch Markdown nếu có)
                // Gemini thường trả về dạng: ```json { ... } ``` gây lỗi parser, cần xóa đi
                String jsonContent = rawResponse.replace("```json", "").replace("```", "").trim();

                // 5. Convert String sạch thành Object Java
                return converter.convert(jsonContent);
        }
}