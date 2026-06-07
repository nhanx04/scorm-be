package com.scorm.generator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorm.generator.dto.CourseResponse;
import com.scorm.generator.dto.ai.AiCourseOutline;
import com.scorm.generator.dto.ai.AiPageContentResponse;
import com.scorm.generator.dto.ai.AiQuizResponse;
import com.scorm.generator.dto.ai.GenerateCourseQuizRequest;
import com.scorm.generator.dto.ai.GenerateCourseRequest;
import com.scorm.generator.dto.ai.GeneratePageContentRequest;
import com.scorm.generator.dto.ai.GenerateQuizRequest;
import com.scorm.generator.service.AiGeneratorService;
import com.scorm.generator.service.CourseService;
import com.scorm.generator.service.rag.CourseRagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
// @CrossOrigin(origins = "*") // Bật dòng này nếu bạn gặp lỗi CORS khi test từ
// localhost:3000
public class AiFeatureController {

    private final AiGeneratorService aiGeneratorService;
    private final CourseService courseService;
    private final CourseRagService courseRagService;
    private final ObjectMapper objectMapper; // <-- Inject ObjectMapper để parse JSON từ form-data

    /**
     * Chọn nguồn ngữ cảnh (grounding) cho khóa học: ưu tiên RAG (top-k đoạn liên
     * quan tới {@code query}) khi khóa học đã được index; nếu không có chỉ mục
     * hoặc truy xuất rỗng thì fallback về toàn bộ tài liệu gốc.
     *
     * <p>Luôn gọi {@link CourseService#getCourseSourceDocument} trước để vừa kiểm
     * tra quyền sở hữu khóa học vừa có sẵn nội dung fallback.
     */
    private String resolveGrounding(Long courseId, String query, Authentication authentication) {
        String fullDocument = courseService.getCourseSourceDocument(courseId, authentication);
        if (query != null && !query.isBlank() && courseRagService.hasIndex(courseId)) {
            String ragContext = courseRagService.retrieveContext(courseId, query, null);
            if (!ragContext.isBlank()) {
                return ragContext;
            }
        }
        return fullDocument;
    }

    /** Ghép tiêu đề chương + chủ đề bài học thành truy vấn truy xuất RAG. */
    private static String buildPageQuery(String sectionTitle, String pageTopic) {
        StringBuilder sb = new StringBuilder();
        if (sectionTitle != null && !sectionTitle.isBlank()) {
            sb.append(sectionTitle.strip());
        }
        if (pageTopic != null && !pageTopic.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" — ");
            }
            sb.append(pageTopic.strip());
        }
        return sb.toString();
    }

    /**
     * API tạo dàn ý khóa học từ file (PDF, DOCX)
     * Method: POST
     * URL: /ai/generate-outline-from-file
     */
    @PostMapping(value = "/generate-outline-from-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiCourseOutline> generateOutlineFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "request", required = false) String requestJsonString) {

        try {
            // 1. Nếu user không gửi kèm cấu hình (chỉ upload file không thôi), tạo một
            // request rỗng
            GenerateCourseRequest requestConfig = new GenerateCourseRequest();

            // 2. Nếu user có gửi kèm cấu hình (JSON string), parse nó ra Object
            if (requestJsonString != null && !requestJsonString.trim().isEmpty()) {
                requestConfig = objectMapper.readValue(requestJsonString, GenerateCourseRequest.class);
            }

            // 3. Gọi service để trích xuất text từ file và gọi AI
            AiCourseOutline outline = aiGeneratorService.generateCourseOutlineFromFile(file, requestConfig);

            return ResponseEntity.ok(outline);

        } catch (Exception e) {
            e.printStackTrace();
            // Tạm thời trả về 500 kèm message lỗi để dễ debug
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API tạo dàn ý khóa học tự động
     * Method: POST
     * URL: /ai/generate-outline
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
     * URL: /ai/save-outline
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
     * URL: /ai/generate-page-content
     */
    @PostMapping("/generate-page-content")
    public ResponseEntity<AiPageContentResponse> generatePageContent(
            @RequestBody GeneratePageContentRequest request,
            Authentication authentication) {
        // Nạp ngữ cảnh bám sát tài liệu (RAG theo chủ đề bài học, fallback toàn bộ tài liệu).
        if (request.getCourseId() != null) {
            String query = buildPageQuery(request.getSectionTitle(), request.getPageTopic());
            request.setSourceDocumentText(
                    resolveGrounding(request.getCourseId(), query, authentication));
        }
        AiPageContentResponse content = aiGeneratorService.generatePageContent(request);
        return ResponseEntity.ok(content);
    }

    /**
     * API tạo bộ câu hỏi trắc nghiệm
     * Method: POST
     * URL: /ai/generate-quiz
     */
    @PostMapping("/generate-quiz")
    public ResponseEntity<AiQuizResponse> generateQuiz(@RequestBody GenerateQuizRequest request) {
        AiQuizResponse quizResponse = aiGeneratorService.generateQuizFromText(request);
        return ResponseEntity.ok(quizResponse);
    }

    @PostMapping("/generate-course-quiz")
    public ResponseEntity<AiQuizResponse> generateCourseQuiz(
            @RequestBody GenerateCourseQuizRequest request,
            Authentication authentication) {
        // Nạp nguồn dữ kiện (ground). Người dùng chỉ nhập focusTopic; sourceText do
        // backend tự lấy từ DB. Có chủ đề trọng tâm → truy xuất RAG theo focusTopic;
        // không có → ra đề tổng quát nên dùng toàn bộ tài liệu để bao quát.
        if (request.getCourseId() != null) {
            String focusTopic = request.getFocusTopic();
            request.setSourceText(
                    resolveGrounding(request.getCourseId(), focusTopic, authentication));
        }
        AiQuizResponse quizResponse = aiGeneratorService.generateCourseAwareQuiz(request);
        return ResponseEntity.ok(quizResponse);
    }
}