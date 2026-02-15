package com.scorm.generator.controller;

import com.scorm.generator.dto.ai.AiCourseOutline;
import com.scorm.generator.dto.ai.GenerateCourseRequest;
import com.scorm.generator.service.AiGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
// @CrossOrigin(origins = "*") // Bật dòng này nếu bạn gặp lỗi CORS khi test từ
// localhost:3000
public class AiFeatureController {

    private final AiGeneratorService aiGeneratorService;

    /**
     * API tạo dàn ý khóa học tự động
     * Method: POST
     * URL: /api/ai/generate-outline
     */
    @PostMapping("/generate-outline")
    public ResponseEntity<AiCourseOutline> generateOutline(@RequestBody GenerateCourseRequest request) {
        // Gọi service để lấy dữ liệu từ Gemini
        AiCourseOutline outline = aiGeneratorService.generateCourseOutline(request);

        // Trả về JSON kết quả
        return ResponseEntity.ok(outline);
    }
}