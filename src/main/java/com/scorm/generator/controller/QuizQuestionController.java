package com.scorm.generator.controller;

import com.scorm.generator.service.Question.QuizQuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quiz-pages")
public class QuizQuestionController {

    private final QuizQuestionService quizQuestionService;

    public QuizQuestionController(QuizQuestionService quizQuestionService) {
        this.quizQuestionService = quizQuestionService;
    }

    @PostMapping("/{pageId}/questions/{questionId}")
    public ResponseEntity<Void> attach(@PathVariable Long pageId, @PathVariable Long questionId,
            Authentication authentication) {
        quizQuestionService.attach(pageId, questionId, authentication);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{pageId}/questions/{questionId}")
    public ResponseEntity<Void> detach(@PathVariable Long pageId, @PathVariable Long questionId,
            Authentication authentication) {
        quizQuestionService.detach(pageId, questionId, authentication);
        return ResponseEntity.ok().build();
    }
}

