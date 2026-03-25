package com.scorm.generator.dto.ai;

public record AiPageContentResponse(
                String pageTitle, // Tên bài học (AI có thể tinh chỉnh lại cho hay hơn)
                String htmlContent, // Nội dung bài học dưới dạng mã HTML
                int estimatedReadingTime, // Thời gian đọc dự kiến (phút) do AI đánh giá
                String shortSummary // Tóm tắt nội dung (có thể dùng làm mô tả meta)
) {
}