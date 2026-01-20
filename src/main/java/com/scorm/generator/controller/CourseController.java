package com.scorm.generator.controller;

import com.scorm.generator.dto.CourseCreateRequest;
import com.scorm.generator.dto.CourseResponse;
import com.scorm.generator.dto.CourseUpdateRequest;
import com.scorm.generator.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(@RequestBody CourseCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(courseService.create(request, authentication));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> listMine(Authentication authentication) {
        return ResponseEntity.ok(courseService.listMine(authentication));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<com.scorm.generator.dto.CourseDetailResponse> getById(
            @PathVariable Long courseId,
            Authentication authentication) {
        return ResponseEntity.ok(courseService.getById(courseId, authentication));
    }

    @PatchMapping("/{courseId}")
    public ResponseEntity<CourseResponse> update(
            @PathVariable Long courseId,
            @RequestBody CourseUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(courseService.update(courseId, request, authentication));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(@PathVariable Long courseId, Authentication authentication) {
        courseService.delete(courseId, authentication);
        return ResponseEntity.ok().build();
    }
}
