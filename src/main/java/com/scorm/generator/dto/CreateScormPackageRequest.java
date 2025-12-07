package com.scorm.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateScormPackageRequest {
    private String welcomeVideoUrl;
    private String themeJson;
    private String title;
    private String description;
    private Integer passingScore;
    private Integer maxAttempts;
    private List<QuestionDTO> questions;
    private com.scorm.generator.model.ReviewMode reviewMode;

}
