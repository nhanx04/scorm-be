package com.scorm.generator.service;

import com.scorm.generator.dto.CreateScormPackageRequest;
import com.scorm.generator.dto.ScormPackageDTO;
import com.scorm.generator.entity.Answer;
import com.scorm.generator.entity.Question;
import com.scorm.generator.entity.ScormPackage;
import com.scorm.generator.entity.User;
import com.scorm.generator.repository.ScormPackageRepository;
import com.scorm.generator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScormPackageService {

    private final ScormPackageRepository scormPackageRepository;
    private final UserRepository userRepository;
    private final ScormExportService scormExportService;
    private final MapperService mapperService;
    private final AwsS3Service s3Service;

    @Transactional
    public ScormPackageDTO createScormPackage(Long userId, CreateScormPackageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ScormPackage scormPackage = ScormPackage.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .passingScore(request.getPassingScore() != null ? request.getPassingScore() : 70)
                .maxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 3)
                .user(user)
                .questions(new java.util.ArrayList<>())
                .build();

        // Add questions
        if (request.getQuestions() != null) {
            for (int index = 0; index < request.getQuestions().size(); index++) {
                var questionDTO = request.getQuestions().get(index);
                Question question = Question.builder()
                        .text(questionDTO.getText())
                        .questionOrder(index)
                        .scormPackage(scormPackage)
                        .answers(new java.util.ArrayList<>())
                        .build();

                if (questionDTO.getAnswers() != null) {
                    for (int ansIndex = 0; ansIndex < questionDTO.getAnswers().size(); ansIndex++) {
                        var answerDTO = questionDTO.getAnswers().get(ansIndex);
                        Answer answer = Answer.builder()
                                .text(answerDTO.getText())
                                .correct(answerDTO.getCorrect())
                                .answerOrder(ansIndex)
                                .question(question)
                                .build();
                        question.getAnswers().add(answer);
                    }
                }

                scormPackage.getQuestions().add(question);
            }
        }

        scormPackage = scormPackageRepository.save(scormPackage);

        // Generate SCORM package file
        try {
            String packageUrl = scormExportService.exportPackage(scormPackage);
            scormPackage.setPackageUrl(packageUrl);
            scormPackage = scormPackageRepository.save(scormPackage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate SCORM package: " + e.getMessage());
        }

        return mapperService.toScormPackageDTO(scormPackage);
    }

    @Transactional(readOnly = true)
    public List<ScormPackageDTO> getUserScormPackages(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return scormPackageRepository.findByUser(user)
                .stream()
                .map(mapperService::toScormPackageDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScormPackageDTO getScormPackageById(Long userId, Long packageId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ScormPackage scormPackage = scormPackageRepository.findByIdAndUser(packageId, user)
                .orElseThrow(() -> new RuntimeException("SCORM package not found"));

        return mapperService.toScormPackageDTO(scormPackage);
    }

    @Transactional
    public void deleteScormPackage(Long userId, Long packageId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ScormPackage scormPackage = scormPackageRepository.findByIdAndUser(packageId, user)
                .orElseThrow(() -> new RuntimeException("SCORM package not found"));

        // Delete the file from Amazon S3
        if (scormPackage.getPackageUrl() != null && !scormPackage.getPackageUrl().isEmpty()) {
            s3Service.deleteFileFromUrl(scormPackage.getPackageUrl());
        }

        scormPackageRepository.delete(scormPackage);
    }
}
