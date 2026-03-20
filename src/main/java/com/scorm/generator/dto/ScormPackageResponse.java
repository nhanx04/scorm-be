package com.scorm.generator.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.scorm.generator.entity.ScormPackage;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScormPackageResponse {
    private Long scormPackageId;
    private String packageName;
    private String packageType;
    private String zipFilePath;
    private String cloudKey;
    private String cloudUrl;
    private JsonNode themeSnapshot;
    private Long packageCourseId;
    private Long packageConfigId;
    private Long packageUserId;

    public static ScormPackageResponse fromEntity(ScormPackage entity) {
        return ScormPackageResponse.builder()
                .scormPackageId(entity.getScormPackageId())
                .packageName(entity.getPackageName())
                .packageType(entity.getPackageType())
                .zipFilePath(entity.getZipFilePath())
                .cloudKey(entity.getCloudKey())
                .cloudUrl(entity.getCloudUrl())
                .themeSnapshot(entity.getThemeSnapshot())
                .packageCourseId(entity.getCourse() != null ? entity.getCourse().getCourseId() : null)
                .packageConfigId(entity.getConfig() != null ? entity.getConfig().getScormConfigId() : null)
                .packageUserId(entity.getUser() != null ? entity.getUser().getUserId() : null)
                .build();
    }
}
