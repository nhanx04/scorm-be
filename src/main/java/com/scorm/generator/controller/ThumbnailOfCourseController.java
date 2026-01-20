package com.scorm.generator.controller;

import com.scorm.generator.dto.ThumbnailOfCourseResponse;
import com.scorm.generator.dto.ThumbnailOfCourseUpsertRequest;
import com.scorm.generator.service.ThumbnailOfCourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ThumbnailOfCourseController {

    private final ThumbnailOfCourseService thumbnailOfCourseService;

    public ThumbnailOfCourseController(ThumbnailOfCourseService thumbnailOfCourseService) {
        this.thumbnailOfCourseService = thumbnailOfCourseService;
    }

    @PutMapping("/courses/{courseId}/thumbnail")
    public ResponseEntity<ThumbnailOfCourseResponse> upsert(
            @PathVariable Long courseId,
            @RequestBody ThumbnailOfCourseUpsertRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(thumbnailOfCourseService.upsert(courseId, request, authentication));
    }

    @GetMapping("/courses/{courseId}/thumbnail")
    public ResponseEntity<ThumbnailOfCourseResponse> getByCourseId(
            @PathVariable Long courseId,
            Authentication authentication) {
        return ResponseEntity.ok(thumbnailOfCourseService.getByCourseId(courseId, authentication));
    }

    @DeleteMapping("/courses/{courseId}/thumbnail")
    public ResponseEntity<Void> delete(
            @PathVariable Long courseId,
            Authentication authentication) {
        thumbnailOfCourseService.delete(courseId, authentication);
        return ResponseEntity.noContent().build();
    }
}
