package com.scorm.generator.service;

import com.scorm.generator.dto.AnswerDTO;
import com.scorm.generator.dto.QuestionDTO;
import com.scorm.generator.dto.ScormPackageDTO;
import com.scorm.generator.entity.Answer;
import com.scorm.generator.entity.Question;
import com.scorm.generator.entity.ScormPackage;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class MapperService {

        public ScormPackageDTO toScormPackageDTO(ScormPackage scormPackage) {
                return ScormPackageDTO.builder()
                                .id(scormPackage.getId())
                                .title(scormPackage.getTitle())
                                .description(scormPackage.getDescription())
                                .passingScore(scormPackage.getPassingScore())
                                .maxAttempts(scormPackage.getMaxAttempts())
                                .packageUrl(scormPackage.getPackageUrl())
                                .createdAt(scormPackage.getCreatedAt())
                                .updatedAt(scormPackage.getUpdatedAt())
                                .questions(scormPackage.getQuestions() != null ? scormPackage.getQuestions().stream()
                                                .map(this::toQuestionDTO)
                                                .collect(Collectors.toList())
                                                : null)
                                .build();
        }

        public QuestionDTO toQuestionDTO(Question question) {
                return QuestionDTO.builder()
                                .id(question.getId())
                                .text(question.getText())
                                .questionOrder(question.getQuestionOrder())
                                .answers(question.getAnswers() != null ? question.getAnswers().stream()
                                                .map(this::toAnswerDTO)
                                                .collect(Collectors.toList())
                                                : null)
                                .build();
        }

        public AnswerDTO toAnswerDTO(Answer answer) {
                return AnswerDTO.builder()
                                .id(answer.getId())
                                .text(answer.getText())
                                .correct(answer.getCorrect())
                                .answerOrder(answer.getAnswerOrder())
                                .build();
        }
}
