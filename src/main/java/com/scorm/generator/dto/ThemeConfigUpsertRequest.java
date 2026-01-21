package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ThemeConfigUpsertRequest {
    private JsonNode themeConfig;
}

