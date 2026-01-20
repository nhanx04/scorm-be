package com.scorm.generator.dto.Question;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortAnswerDetailsDto {
    private Integer minLength;
    private Integer maxLength;
    private List<ShortAnswerExpectedDto> expectedAnswers;
}

