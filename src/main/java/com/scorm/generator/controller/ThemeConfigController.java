package com.scorm.generator.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.scorm.generator.dto.ThemeConfigUpsertRequest;
import com.scorm.generator.service.ThemeConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ThemeConfigController {

    private final ThemeConfigService themeConfigService;

    public ThemeConfigController(ThemeConfigService themeConfigService) {
        this.themeConfigService = themeConfigService;
    }

    @PutMapping("/courses/{courseId}/scorm-export-config/theme")
    public ResponseEntity<JsonNode> upsertCourseTheme(
            @PathVariable Long courseId,
            @RequestBody ThemeConfigUpsertRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(themeConfigService.upsertCourseTheme(courseId, request.getThemeConfig(), authentication));
    }

    @PutMapping("/sections/{sectionId}/theme")
    public ResponseEntity<JsonNode> upsertSectionTheme(
            @PathVariable Long sectionId,
            @RequestBody ThemeConfigUpsertRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(themeConfigService.upsertSectionTheme(sectionId, request.getThemeConfig(), authentication));
    }

    @PutMapping("/pages/{pageId}/theme")
    public ResponseEntity<JsonNode> upsertPageTheme(
            @PathVariable Long pageId,
            @RequestBody ThemeConfigUpsertRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(themeConfigService.upsertPageTheme(pageId, request.getThemeConfig(), authentication));
    }
}

