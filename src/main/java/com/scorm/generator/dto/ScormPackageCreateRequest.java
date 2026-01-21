package com.scorm.generator.dto;

import lombok.Data;

@Data
public class ScormPackageCreateRequest {
    private String packageName;
    private String packageType;
}

