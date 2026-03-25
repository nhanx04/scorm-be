package com.scorm.generator.controller;

import com.scorm.generator.dto.AI.AiCourseOutline;
import com.scorm.generator.dto.AI.GenerateCourseRequest;
import com.scorm.generator.dto.AI.AiPageContentResponse;
import com.scorm.generator.dto.AI.GeneratePageContentRequest;
import com.scorm.generator.dto.AI.AiQuizResponse;
import com.scorm.generator.dto.AI.GenerateQuizRequest;
import com.scorm.generator.dto.CourseResponse; // <-- Bổ sung import này
import com.scorm.generator.service.AiGeneratorService;
import com.scorm.generator.service.CourseService; // <-- Bổ sung import này
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // <-- Bổ sung import này
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
// @CrossOrigin(origins = "*") // Bật dòng này nếu bạn gặp lỗi CORS khi test từ
// localhost:3000
public class AiFeatureController {

    private final AiGeneratorService aiGeneratorService;
    private final CourseService courseService; // <-- Inject thêm CourseService vào Controller

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

    /**
     * API lưu dàn ý khóa học (do AI sinh ra) vào Database
     * Method: POST
     * URL: /api/ai/save-outline
     */
    @PostMapping("/save-outline")
    public ResponseEntity<CourseResponse> saveOutlineToDatabase(
            @RequestBody AiCourseOutline outline,
            Authentication authentication) {

        // Gọi Service để lưu Outline và trả về thông tin Course vừa tạo
        CourseResponse savedCourse = courseService.saveAiCourseOutline(outline, authentication);

        return ResponseEntity.ok(savedCourse);
    }

    /**
     * API tạo nội dung chi tiết cho một Page
     * Method: POST
     * URL: /api/ai/generate-page-content
     */
    @PostMapping("/generate-page-content")
    public ResponseEntity<AiPageContentResponse> generatePageContent(@RequestBody GeneratePageContentRequest request) {
        AiPageContentResponse content = aiGeneratorService.generatePageContent(request);
        return ResponseEntity.ok(content);
    }

    @PostMapping("/generate-quiz")
    public ResponseEntity<AiQuizResponse> generateQuiz(@RequestBody GenerateQuizRequest request) {
        AiQuizResponse quizResponse = aiGeneratorService.generateQuizFromText(request);
        return ResponseEntity.ok(quizResponse);
    }
}