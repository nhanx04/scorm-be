package com.scorm.generator.controller;

import com.scorm.generator.dto.PageCreateRequest;
import com.scorm.generator.dto.PageResponse;
import com.scorm.generator.dto.PageUpdateRequest;
import com.scorm.generator.service.PageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class PageController {

    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    @PostMapping("/sections/{sectionId}/pages")
    public ResponseEntity<PageResponse> create(
            @PathVariable Long sectionId,
            @RequestBody PageCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(pageService.create(sectionId, request, authentication));
    }

    @PatchMapping("/pages/{pageId}")
    public ResponseEntity<PageResponse> update(
            @PathVariable Long pageId,
            @RequestBody PageUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(pageService.update(pageId, request, authentication));
    }

    @DeleteMapping("/pages/{pageId}")
    public ResponseEntity<Void> delete(@PathVariable Long pageId, Authentication authentication) {
        pageService.delete(pageId, authentication);
        return ResponseEntity.ok().build();
    }
}

