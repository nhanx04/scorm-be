package com.scorm.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryListItemResponse {
    private Long libraryId;
    private String libraryName;
    private String description;
    private String scopeType;
    private OffsetDateTime updatedAt;
}

