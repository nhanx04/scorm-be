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
public class BlankDto {
    private Long blankId;
    private String blankKey;
    private Integer orderIndex;
    private List<BlankAnswerDto> answers;
}

