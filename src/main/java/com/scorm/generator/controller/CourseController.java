package com.scorm.generator.controller;

import com.scorm.generator.dto.CourseCreateRequest;
import com.scorm.generator.dto.CourseResponse;
import com.scorm.generator.dto.CourseUpdateRequest;
import com.scorm.generator.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(courseService.create(request, authentication));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> listMine(Authentication authentication) {
        return ResponseEntity.ok(courseService.listMine(authentication));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<CourseResponse>> getRecent(
            @RequestParam(defaultValue = "6") int limit,
            Authentication authentication) {
        return ResponseEntity.ok(courseService.getRecentCourses(authentication, limit));
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
            @Valid @RequestBody CourseUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(courseService.update(courseId, request, authentication));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(@PathVariable Long courseId, Authentication authentication) {
        courseService.delete(courseId, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/import-scorm", consumes = "multipart/form-data")
    public ResponseEntity<CourseResponse> importScorm(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.ok(courseService.importScormPackage(file, authentication));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<CourseResponse>> getRecent(
            @RequestParam(defaultValue = "6") int limit,
            Authentication authentication) {
        return ResponseEntity.ok(courseService.getRecentCourses(authentication, limit));
    }
}
