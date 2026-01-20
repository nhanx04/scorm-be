package com.scorm.generator.controller;

import com.scorm.generator.dto.QuizPageCreateRequest;
import com.scorm.generator.dto.QuizPageResponse;
import com.scorm.generator.dto.QuizPageUpdateRequest;
import com.scorm.generator.service.QuizPageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class QuizPageController {

    private final QuizPageService quizPageService;

    public QuizPageController(QuizPageService quizPageService) {
        this.quizPageService = quizPageService;
    }

    @PostMapping("/pages/{pageId}/quiz-page")
    public ResponseEntity<QuizPageResponse> create(
            @PathVariable Long pageId,
            @RequestBody QuizPageCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(quizPageService.create(pageId, request, authentication));
    }

    @GetMapping("/quiz-pages/{pageId}")
    public ResponseEntity<QuizPageResponse> getByPageId(
            @PathVariable Long pageId,
            Authentication authentication) {
        return ResponseEntity.ok(quizPageService.getByPageId(pageId, authentication));
    }

    @PatchMapping("/quiz-pages/{pageId}")
    public ResponseEntity<QuizPageResponse> update(
            @PathVariable Long pageId,
            @RequestBody QuizPageUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(quizPageService.update(pageId, request, authentication));
    }
}
