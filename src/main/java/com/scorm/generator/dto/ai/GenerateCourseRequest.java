package com.scorm.generator.dto.ai;

import lombok.Data;

@Data
public class GenerateCourseRequest {
    // ==========================================
    // Phần 1: Describe your course
    // ==========================================

    // 1. Course Description: What is your course about?
    // Mô tả khóa học (VD: "Khóa học hướng dẫn về an toàn lao động trong nhà
    // máy...")
    private String courseDescription;

    // 2. Target Audience & Audience Proficiency Level
    // Đối tượng học viên (VD: "Nhân viên mới", "Sinh viên năm 1")
    private String targetAudience;
    // Trình độ học viên (VD: "Beginner", "Intermediate", "Advanced")
    private String audienceProficiencyLevel;

    // 3. Duration: What is your ideal duration for this course?
    // Thời lượng dự kiến (VD: "2 hours", "4 weeks")
    private String duration;

    // 4. Language
    // Ngôn ngữ mong muốn (VD: "Vietnamese", "English")
    private String language;

    // ==========================================
    // Phần 2: Set the context
    // ==========================================

    // 1. What do you want your learners to do after they finish this course?
    // Mục tiêu đầu ra (VD: "Nắm vững quy trình vận hành máy móc an toàn")
    private String learningOutcomes;

    // 2. What should your learners know to achieve that goal?
    // Yêu cầu đầu vào / Kiến thức nền tảng (VD: "Biết sử dụng máy tính cơ bản")
    private String prerequisites;

    // 3. What title will you give your course?
    // Tiêu đề khóa học (VD: "An toàn lao động cơ bản 101")
    private String courseTitle;

    // ==========================================
    // Yêu cầu thêm (Tùy chọn)
    // ==========================================
    // Mô tả thêm hoặc yêu cầu đặc biệt (Optional)
    private String additionalInstructions;
}