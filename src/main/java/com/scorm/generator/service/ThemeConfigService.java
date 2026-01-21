package com.scorm.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.core.Authentication;

public interface ThemeConfigService {
    JsonNode upsertCourseTheme(Long courseId, JsonNode themeConfig, Authentication authentication);

    JsonNode upsertSectionTheme(Long sectionId, JsonNode themeOverride, Authentication authentication);

    JsonNode upsertPageTheme(Long pageId, JsonNode themeOverride, Authentication authentication);
}

