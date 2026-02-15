package com.scorm.generator.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.scorm.generator.dto.AI.AiCourseOutline;
import com.scorm.generator.dto.AI.GenerateCourseRequest;

@Service
public class AiGeneratorService {

        private final ChatClient chatClient;

        // Spring AI tự động cấu hình ChatClient.Builder dựa trên application.properties
        // bạn đã làm
        public AiGeneratorService(ChatClient.Builder chatClientBuilder) {
                this.chatClient = chatClientBuilder
                                .defaultSystem(
                                                "Bạn là một chuyên gia thiết kế giáo trình e-learning (Instructional Designer) với 10 năm kinh nghiệm. "
                                                                +
                                                                "Nhiệm vụ của bạn là xây dựng cấu trúc khóa học chi tiết, logic và hấp dẫn.")
                                .build();
        }

        /**
         * Gửi yêu cầu sang Gemini và nhận về Object Java (AiCourseOutline)
         */
        public AiCourseOutline generateCourseOutline(GenerateCourseRequest request) {

                // 1. Soạn Prompt (kịch bản nhắc)
                // Dùng text block (""") cho dễ nhìn
                String userPrompt = """
                                Hãy tạo một dàn ý khóa học chi tiết dựa trên các thông tin sau:
                                - Chủ đề: {topic}
                                - Đối tượng học viên: {targetAudience}
                                - Ngôn ngữ đầu ra: {language}
                                - Số lượng chương (sections) mong muốn: khoảng {numberOfSections}
                                - Yêu cầu thêm: {additionalInstructions}

                                Yêu cầu quan trọng về định dạng:
                                1. Trả về kết quả CHÍNH XÁC theo định dạng JSON tương ứng với cấu trúc dữ liệu.
                                2. KHÔNG thêm bất kỳ lời dẫn, markdown formatting (như ```json) hay giải thích nào bên ngoài JSON.
                                3. Nội dung phải mang tính giáo dục cao, phân chia bài học hợp lý.
                                """;

                // 2. Gọi AI và map kết quả
                return chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("topic", request.getTopic())
                                                .param("targetAudience", request.getTargetAudience())
                                                .param("language", request.getLanguage()) // Ví dụ: "Tiếng Việt"
                                                .param("numberOfSections", request.getNumberOfSections())
                                                .param("additionalInstructions",
                                                                request.getAdditionalInstructions() != null
                                                                                ? request.getAdditionalInstructions()
                                                                                : "Không có"))
                                .call()
                                // Đây là phép thuật: Tự động convert JSON của AI thành Record Java
                                .entity(AiCourseOutline.class);
        }
}