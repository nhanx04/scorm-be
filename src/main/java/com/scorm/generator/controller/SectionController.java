package com.scorm.generator.controller;

import com.scorm.generator.dto.SectionCreateRequest;
import com.scorm.generator.dto.SectionResponse;
import com.scorm.generator.dto.SectionUpdateRequest;
import com.scorm.generator.service.SectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @PostMapping("/courses/{courseId}/sections")
    public ResponseEntity<SectionResponse> create(
            @PathVariable Long courseId,
            @RequestBody SectionCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(sectionService.create(courseId, request, authentication));
    }

    @PatchMapping("/sections/{sectionId}")
    public ResponseEntity<SectionResponse> update(
            @PathVariable Long sectionId,
            @RequestBody SectionUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(sectionService.update(sectionId, request, authentication));
    }

    @DeleteMapping("/sections/{sectionId}")
    public ResponseEntity<Void> delete(@PathVariable Long sectionId, Authentication authentication) {
        sectionService.delete(sectionId, authentication);
        return ResponseEntity.ok().build();
    }
}

