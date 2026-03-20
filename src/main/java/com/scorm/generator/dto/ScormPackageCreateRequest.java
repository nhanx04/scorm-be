package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ScormPackageCreateRequest {
    private String packageName;
    private String packageType;
    private JsonNode editorStateSnapshot;
    private JsonNode interfaceSnapshot;
}
