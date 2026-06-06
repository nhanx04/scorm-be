package com.scorm.generator.dto.ai;

import java.util.List;

// Record dùng để hứng dữ liệu JSON từ AI một cách chính xác
public record AiCourseOutline(
                String title, // Tên khóa học do AI gợi ý
                String description, // Mô tả ngắn gọn về khóa học
                List<AiSection> sections, // Danh sách các chương
                String sourceDocumentText // Văn bản gốc trích từ file (nếu tạo từ file); round-trip để persist
) {
        // Record con đại diện cho một chương
        public record AiSection(
                        String title, // Tên chương
                        List<String> topics // Danh sách các ý chính (sẽ trở thành Page sau này)
        ) {
        }

        // Shape mà AI thực sự sinh ra. Tách riêng để sourceDocumentText KHÔNG lọt
        // vào JSON schema gửi cho LLM (tránh model echo lại toàn bộ tài liệu).
        public record Draft(
                        String title,
                        String description,
                        List<AiSection> sections) {
        }
}
