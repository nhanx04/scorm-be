package com.scorm.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import com.scorm.generator.model.QuestionType;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDTO {
    private Long id;
    private String text;
    private QuestionType questionType;
    private String imageUrl;
    private Integer questionOrder;
    private List<AnswerDTO> answers;
}
