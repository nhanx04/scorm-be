package com.scorm.generator.controller;

import com.scorm.generator.dto.ContentPageCreateRequest;
import com.scorm.generator.dto.ContentPageResponse;
import com.scorm.generator.dto.ContentPageUpdateRequest;
import com.scorm.generator.service.ContentPageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ContentPageController {

    private final ContentPageService contentPageService;

    public ContentPageController(ContentPageService contentPageService) {
        this.contentPageService = contentPageService;
    }

    @PostMapping("/pages/{pageId}/content-page")
    public ResponseEntity<ContentPageResponse> create(
            @PathVariable Long pageId,
            @RequestBody ContentPageCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(contentPageService.create(pageId, request, authentication));
    }

    @GetMapping("/content-pages/{pageId}")
    public ResponseEntity<ContentPageResponse> getByPageId(
            @PathVariable Long pageId,
            Authentication authentication) {
        return ResponseEntity.ok(contentPageService.getByPageId(pageId, authentication));
    }

    @PatchMapping("/content-pages/{pageId}")
    public ResponseEntity<ContentPageResponse> update(
            @PathVariable Long pageId,
            @RequestBody ContentPageUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(contentPageService.update(pageId, request, authentication));
    }
}
