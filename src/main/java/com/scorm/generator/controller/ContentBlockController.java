package com.scorm.generator.controller;

import com.scorm.generator.dto.ContentBlockCreateRequest;
import com.scorm.generator.dto.ContentBlockResponse;
import com.scorm.generator.dto.ContentBlockUpdateRequest;
import com.scorm.generator.service.ContentBlockService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ContentBlockController {

    private final ContentBlockService contentBlockService;

    public ContentBlockController(ContentBlockService contentBlockService) {
        this.contentBlockService = contentBlockService;
    }

    @PostMapping("/content-pages/{pageId}/blocks")
    public ResponseEntity<ContentBlockResponse> create(
            @PathVariable Long pageId,
            @RequestBody ContentBlockCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(contentBlockService.create(pageId, request, authentication));
    }

    @GetMapping("/content-pages/{pageId}/blocks")
    public ResponseEntity<List<ContentBlockResponse>> listByContentPage(
            @PathVariable Long pageId,
            Authentication authentication) {
        return ResponseEntity.ok(contentBlockService.getByContentPageId(pageId, authentication));
    }

    @GetMapping("/content-blocks/{blockId}")
    public ResponseEntity<ContentBlockResponse> getById(
            @PathVariable Long blockId,
            Authentication authentication) {
        return ResponseEntity.ok(contentBlockService.getById(blockId, authentication));
    }

    @PatchMapping("/content-blocks/{blockId}")
    public ResponseEntity<ContentBlockResponse> update(
            @PathVariable Long blockId,
            @RequestBody ContentBlockUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(contentBlockService.update(blockId, request, authentication));
    }

    @DeleteMapping("/content-blocks/{blockId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long blockId,
            Authentication authentication) {
        contentBlockService.delete(blockId, authentication);
        return ResponseEntity.noContent().build();
    }
}
