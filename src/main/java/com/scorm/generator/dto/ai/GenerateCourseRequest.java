package com.scorm.generator.dto.ai;

import lombok.Data;

@Data
public class GenerateCourseRequest {
    // Chủ đề khóa học (VD: "Python cơ bản", "An toàn lao động")
    private String topic;

    // Đối tượng học viên (VD: "Sinh viên năm 1", "Nhân viên mới")
    private String targetAudience;

    // Ngôn ngữ mong muốn (VD: "Vietnamese", "English")
    private String language;

    // Số lượng chương/phần mong muốn (để kiểm soát độ dài AI gen ra)
    private int numberOfSections = 5; // Mặc định là 5 nếu user không chọn

    // Mô tả thêm hoặc yêu cầu đặc biệt (Optional)
    private String additionalInstructions;
}