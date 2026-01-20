package com.scorm.generator.controller;

import com.scorm.generator.dto.Question.QuestionCreateRequest;
import com.scorm.generator.dto.Question.QuestionDetailsRequest;
import com.scorm.generator.dto.Question.QuestionResponse;
import com.scorm.generator.dto.Question.QuestionUpdateRequest;
import com.scorm.generator.service.Question.QuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public ResponseEntity<QuestionResponse> create(@RequestBody QuestionCreateRequest request) {
        return ResponseEntity.ok(questionService.create(request));
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionResponse> getById(@PathVariable Long questionId) {
        return ResponseEntity.ok(questionService.getById(questionId));
    }

    @PatchMapping("/{questionId}")
    public ResponseEntity<QuestionResponse> update(@PathVariable Long questionId,
            @RequestBody QuestionUpdateRequest request) {
        return ResponseEntity.ok(questionService.update(questionId, request));
    }

    @PutMapping("/{questionId}/details")
    public ResponseEntity<QuestionResponse> replaceDetails(@PathVariable Long questionId,
            @RequestBody QuestionDetailsRequest request) {
        return ResponseEntity.ok(questionService.replaceDetails(questionId, request));
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> delete(@PathVariable Long questionId) {
        questionService.delete(questionId);
        return ResponseEntity.ok().build();
    }
}

