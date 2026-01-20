package com.scorm.generator.dto.Question;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChoiceOptionDto {
    private Long optionId;
    private Integer orderIndex;
    private String contentHtml;
    private Boolean isCorrect;
    private BigDecimal scoreFraction;
    private List<Long> imageMediaIds;
}

