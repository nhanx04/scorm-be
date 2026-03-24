package com.scorm.generator.service.Question;

import com.scorm.generator.dto.Question.*;
import com.scorm.generator.entity.Question.*;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.Question.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionChoiceOptionRepository choiceOptionRepository;
    private final QuestionTrueFalseRepository trueFalseRepository;
    private final QuestionFillBlankRepository fillBlankRepository;
    private final QuestionBlankRepository blankRepository;
    private final QuestionBlankAnswerRepository blankAnswerRepository;
    private final QuestionMatchingLeftRepository matchingLeftRepository;
    private final QuestionMatchingRightRepository matchingRightRepository;
    private final QuestionMatchingPairRepository matchingPairRepository;
    private final QuestionShortAnswerExpectedRepository shortAnswerExpectedRepository;
    private final QuestionGroupRepository groupRepository;
    private final QuestionGroupItemRepository groupItemRepository;
    private final QuestionGroupItemAnswerRepository groupItemAnswerRepository;
    private final QuestionOfQuizRepository questionOfQuizRepository;
    private final ImageOfChoiceOptionRepository imageOfChoiceOptionRepository;
    private final MediaOfQuestionRepository mediaOfQuestionRepository;

    @Override
    @Transactional
    public QuestionResponse create(QuestionCreateRequest request) {
        Question question = Question.builder()
                .title(request.getTitle())
                .instruction(request.getInstruction())
                .promptHtml(request.getPromptHtml())
                .textHtml(request.getTextHtml())
                .questionType(request.getQuestionType())
                .themeOverride(request.getThemeOverride())
                .layoutMode(request.getLayoutMode())
                .layoutMeta(request.getLayoutMeta())
                .templateData(request.getTemplateData())
                .points(request.getPoints() != null ? request.getPoints() : BigDecimal.ONE)
                .shuffleOptions(request.getShuffleOptions() != null ? request.getShuffleOptions() : false)
                .caseSensitive(request.getCaseSensitive() != null ? request.getCaseSensitive() : false)
                .extraConfig(request.getExtraConfig())
                .build();

        question = questionRepository.save(question);
        return mapToResponse(question);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Question not found"));
        return mapToResponse(question);
    }

    @Override
    @Transactional
    public QuestionResponse update(Long questionId, QuestionUpdateRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Question not found"));

        if (request.getTitle() != null)
            question.setTitle(request.getTitle());
        if (request.getInstruction() != null)
            question.setInstruction(request.getInstruction());
        if (request.getPromptHtml() != null)
            question.setPromptHtml(request.getPromptHtml());
        if (request.getTextHtml() != null)
            question.setTextHtml(request.getTextHtml());
        if (request.getThemeOverride() != null)
            question.setThemeOverride(request.getThemeOverride());
        if (request.getLayoutMode() != null)
            question.setLayoutMode(request.getLayoutMode());
        if (request.getLayoutMeta() != null)
            question.setLayoutMeta(request.getLayoutMeta());
        if (request.getTemplateData() != null)
            question.setTemplateData(request.getTemplateData());
        if (request.getPoints() != null)
            question.setPoints(request.getPoints());
        if (request.getShuffleOptions() != null)
            question.setShuffleOptions(request.getShuffleOptions());
        if (request.getCaseSensitive() != null)
            question.setCaseSensitive(request.getCaseSensitive());
        if (request.getExtraConfig() != null)
            question.setExtraConfig(request.getExtraConfig());

        question = questionRepository.save(question);
        return mapToResponse(question);
    }

    @Override
    @Transactional
    public QuestionResponse replaceDetails(Long questionId, QuestionDetailsRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Question not found"));

        // Update base question fields if provided
        if (request.getQuestion() != null) {
            QuestionUpdateRequest updateRequest = request.getQuestion();
            if (updateRequest.getTitle() != null)
                question.setTitle(updateRequest.getTitle());
            if (updateRequest.getInstruction() != null)
                question.setInstruction(updateRequest.getInstruction());
            if (updateRequest.getPromptHtml() != null)
                question.setPromptHtml(updateRequest.getPromptHtml());
            if (updateRequest.getTextHtml() != null)
                question.setTextHtml(updateRequest.getTextHtml());
            if (updateRequest.getQuestionType() != null)
                question.setQuestionType(updateRequest.getQuestionType());
            if (updateRequest.getThemeOverride() != null)
                question.setThemeOverride(updateRequest.getThemeOverride());
            if (updateRequest.getLayoutMode() != null)
                question.setLayoutMode(updateRequest.getLayoutMode());
            if (updateRequest.getLayoutMeta() != null)
                question.setLayoutMeta(updateRequest.getLayoutMeta());
            if (updateRequest.getTemplateData() != null)
                question.setTemplateData(updateRequest.getTemplateData());
            if (updateRequest.getPoints() != null)
                question.setPoints(updateRequest.getPoints());
            if (updateRequest.getShuffleOptions() != null)
                question.setShuffleOptions(updateRequest.getShuffleOptions());
            if (updateRequest.getCaseSensitive() != null)
                question.setCaseSensitive(updateRequest.getCaseSensitive());
            if (updateRequest.getExtraConfig() != null)
                question.setExtraConfig(updateRequest.getExtraConfig());

            question = questionRepository.save(question);
        }

        if (question.getQuestionType() == null || question.getQuestionType().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "questionType is required");
        }

        // Replace details: cleanup then insert new details
        deleteQuestionDetails(questionId);

        Object details = request.getDetails();
        String type = question.getQuestionType();

        if (details == null) {
            return mapToResponse(question);
        }

        switch (type) {
            case "MCQ_SINGLE", "MCQ_MULTI" -> {
                MultipleChoiceDetailsDto dto = (MultipleChoiceDetailsDto) details;
                if (dto.getOptions() == null || dto.getOptions().size() < 2) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "MCQ requires at least 2 options");
                }
                boolean hasCorrect = dto.getOptions().stream().anyMatch(o -> Boolean.TRUE.equals(o.getIsCorrect()));
                if (!hasCorrect) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "MCQ requires at least 1 correct option");
                }
                for (ChoiceOptionDto opt : dto.getOptions()) {
                    QuestionChoiceOption entity = QuestionChoiceOption.builder()
                            .question(question)
                            .orderIndex(opt.getOrderIndex())
                            .contentHtml(opt.getContentHtml())
                            .isCorrect(opt.getIsCorrect())
                            .scoreFraction(opt.getScoreFraction())
                            .build();
                    choiceOptionRepository.save(entity);
                }
            }
            case "TRUE_FALSE" -> {
                TrueFalseDetailsDto dto = (TrueFalseDetailsDto) details;
                if (dto.getCorrectValue() == null) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "TRUE_FALSE requires correctValue");
                }
                QuestionTrueFalse tf = QuestionTrueFalse.builder()
                        .question(question)
                        .correctValue(dto.getCorrectValue())
                        .build();
                trueFalseRepository.save(tf);
            }
            case "FILL_BLANK" -> {
                FillBlankDetailsDto dto = (FillBlankDetailsDto) details;
                if (dto.getBlanks() == null || dto.getBlanks().isEmpty()) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "FILL_BLANK requires blanks");
                }
                QuestionFillBlank fb = QuestionFillBlank.builder()
                        .question(question)
                        .orderedBlanks(dto.getOrderedBlanks() != null && dto.getOrderedBlanks())
                        .build();
                fillBlankRepository.save(fb);

                for (BlankDto blankDto : dto.getBlanks()) {
                    if (blankDto.getAnswers() == null || blankDto.getAnswers().isEmpty()) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "Each blank requires at least 1 answer");
                    }
                    QuestionBlank blank = blankRepository.save(QuestionBlank.builder()
                            .question(question)
                            .blankKey(blankDto.getBlankKey())
                            .orderIndex(blankDto.getOrderIndex())
                            .build());

                    for (BlankAnswerDto ansDto : blankDto.getAnswers()) {
                        blankAnswerRepository.save(QuestionBlankAnswer.builder()
                                .blank(blank)
                                .answerText(ansDto.getAnswerText())
                                .matchRule(ansDto.getMatchRule())
                                .build());
                    }
                }
            }
            case "MATCHING" -> {
                MatchingDetailsDto dto = (MatchingDetailsDto) details;
                if (dto.getLeftItems() == null || dto.getLeftItems().isEmpty() || dto.getRightItems() == null
                        || dto.getRightItems().isEmpty()) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "MATCHING requires leftItems and rightItems");
                }
                if (dto.getPairs() == null || dto.getPairs().isEmpty()) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "MATCHING requires pairs");
                }
                for (MatchingItemDto leftDto : dto.getLeftItems()) {
                    matchingLeftRepository.save(QuestionMatchingLeft.builder()
                            .question(question)
                            .orderIndex(leftDto.getOrderIndex())
                            .contentHtml(leftDto.getContentHtml())
                            .build());
                }
                for (MatchingItemDto rightDto : dto.getRightItems()) {
                    matchingRightRepository.save(QuestionMatchingRight.builder()
                            .question(question)
                            .orderIndex(rightDto.getOrderIndex())
                            .contentHtml(rightDto.getContentHtml())
                            .build());
                }
                for (MatchingPairDto pairDto : dto.getPairs()) {
                    if (pairDto.getLeftId() == null || pairDto.getRightId() == null) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "Pair requires leftId and rightId");
                    }
                    matchingPairRepository.save(QuestionMatchingPair.builder()
                            .question(question)
                            .leftId(pairDto.getLeftId())
                            .rightId(pairDto.getRightId())
                            .build());
                }
            }
            case "SHORT_ANSWER" -> {
                ShortAnswerDetailsDto dto = (ShortAnswerDetailsDto) details;
                if (dto.getExpectedAnswers() == null || dto.getExpectedAnswers().isEmpty()) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "SHORT_ANSWER requires expectedAnswers");
                }
                for (ShortAnswerExpectedDto expDto : dto.getExpectedAnswers()) {
                    shortAnswerExpectedRepository.save(QuestionShortAnswerExpected.builder()
                            .question(question)
                            .answerText(expDto.getAnswerText())
                            .matchRule(expDto.getMatchRule())
                            .build());
                }
            }
            case "GROUPING" -> {
                GroupingDetailsDto dto = (GroupingDetailsDto) details;
                if (dto.getGroups() == null || dto.getGroups().isEmpty() || dto.getItems() == null
                        || dto.getItems().isEmpty()) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "GROUPING requires groups and items");
                }
                for (GroupDto g : dto.getGroups()) {
                    groupRepository.save(QuestionGroup.builder()
                            .question(question)
                            .orderIndex(g.getOrderIndex())
                            .title(g.getTitle())
                            .build());
                }
                for (GroupItemDto i : dto.getItems()) {
                    groupItemRepository.save(QuestionGroupItem.builder()
                            .question(question)
                            .orderIndex(i.getOrderIndex())
                            .contentHtml(i.getContentHtml())
                            .build());
                }
                if (dto.getItemAnswers() != null) {
                    for (GroupItemAnswerDto a : dto.getItemAnswers()) {
                        if (a.getItemId() == null || a.getGroupId() == null) {
                            throw new AppException(HttpStatus.BAD_REQUEST, "ItemAnswer requires itemId and groupId");
                        }
                        groupItemAnswerRepository.save(QuestionGroupItemAnswer.builder()
                                .itemId(a.getItemId())
                                .groupId(a.getGroupId())
                                .build());
                    }
                }
            }
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "Unsupported questionType: " + type);
        }

        return mapToResponse(question);
    }

    @Override
    @Transactional
    public void delete(Long questionId) {
        // Check if question is used in any quiz
        long usageCount = questionOfQuizRepository.countById_QuizQuestionId(questionId);
        if (usageCount > 0) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Cannot delete question as it is being used in " + usageCount + " quiz(es).");
        }

        // Delete all related data
        deleteQuestionDetails(questionId);
        questionRepository.deleteById(questionId);
    }

    private void deleteQuestionDetails(Long questionId) {
        // Delete all related data in the correct order to avoid constraint violations
        groupItemAnswerRepository.deleteByQuestionId(questionId);
        groupItemRepository.deleteByQuestionId(questionId);
        groupRepository.deleteByQuestionId(questionId);
        blankAnswerRepository.deleteByQuestionId(questionId);
        blankRepository.deleteByQuestionId(questionId);
        matchingPairRepository.deleteByQuestionId(questionId);
        matchingLeftRepository.deleteByQuestionId(questionId);
        matchingRightRepository.deleteByQuestionId(questionId);
        shortAnswerExpectedRepository.deleteByQuestionId(questionId);
        choiceOptionRepository.deleteByQuestion_QuestionId(questionId);
        trueFalseRepository.deleteByQuestion_QuestionId(questionId);
        fillBlankRepository.deleteByQuestion_QuestionId(questionId);
    }

    private QuestionResponse mapToResponse(Question question) {
        QuestionBaseDto base = QuestionBaseDto.builder()
                .questionId(question.getQuestionId())
                .title(question.getTitle())
                .instruction(question.getInstruction())
                .promptHtml(question.getPromptHtml())
                .textHtml(question.getTextHtml())
                .questionType(question.getQuestionType())
                .themeOverride(question.getThemeOverride())
                .layoutMode(question.getLayoutMode())
                .layoutMeta(question.getLayoutMeta())
                .templateData(question.getTemplateData())
                .points(question.getPoints())
                .shuffleOptions(question.getShuffleOptions())
                .caseSensitive(question.getCaseSensitive())
                .extraConfig(question.getExtraConfig())
                .build();

        Object details = null;
        String type = question.getQuestionType();
        if (type != null) {
            switch (type) {
                case "MCQ_SINGLE", "MCQ_MULTI" -> {
                    var options = choiceOptionRepository
                            .findByQuestion_QuestionIdOrderByOrderIndexAsc(question.getQuestionId());
                    var optionIds = options.stream().map(QuestionChoiceOption::getOptionId).toList();
                    var imageLinks = optionIds.isEmpty() ? java.util.List.<ImageOfChoiceOption>of()
                            : imageOfChoiceOptionRepository.findById_OptionIdIn(optionIds);

                    java.util.Map<Long, java.util.List<Long>> optionIdToImages = imageLinks.stream()
                            .collect(java.util.stream.Collectors.groupingBy(i -> i.getId().getOptionId(),
                                    java.util.stream.Collectors.mapping(i -> i.getId().getMediaId(),
                                            java.util.stream.Collectors.toList())));

                    var optionDtos = options.stream().map(o -> ChoiceOptionDto.builder()
                            .optionId(o.getOptionId())
                            .orderIndex(o.getOrderIndex())
                            .contentHtml(o.getContentHtml())
                            .isCorrect(o.getIsCorrect())
                            .scoreFraction(o.getScoreFraction())
                            .imageMediaIds(optionIdToImages.getOrDefault(o.getOptionId(), java.util.List.of()))
                            .build()).toList();

                    details = MultipleChoiceDetailsDto.builder().options(optionDtos).build();
                }
                case "TRUE_FALSE" -> {
                    var tf = trueFalseRepository.findById(question.getQuestionId()).orElse(null);
                    details = tf == null ? null
                            : TrueFalseDetailsDto.builder().correctValue(tf.getCorrectValue()).build();
                }
                case "FILL_BLANK" -> {
                    var fb = fillBlankRepository.findById(question.getQuestionId()).orElse(null);
                    var blanks = blankRepository
                            .findByQuestion_QuestionIdOrderByOrderIndexAsc(question.getQuestionId());
                    java.util.Map<Long, java.util.List<QuestionBlankAnswer>> answersByBlank = blanks.isEmpty()
                            ? java.util.Map.of()
                            : blankAnswerRepository.findAll().stream()
                                    .filter(a -> blanks.stream()
                                            .anyMatch(b -> b.getBlankId().equals(a.getBlank().getBlankId())))
                                    .collect(java.util.stream.Collectors.groupingBy(a -> a.getBlank().getBlankId()));

                    var blankDtos = blanks.stream().map(b -> {
                        var ans = answersByBlank.getOrDefault(b.getBlankId(), java.util.List.of());
                        var ansDtos = ans.stream().map(a -> BlankAnswerDto.builder()
                                .answerId(a.getAnswerId())
                                .answerText(a.getAnswerText())
                                .matchRule(a.getMatchRule())
                                .build()).toList();
                        return BlankDto.builder()
                                .blankId(b.getBlankId())
                                .blankKey(b.getBlankKey())
                                .orderIndex(b.getOrderIndex())
                                .answers(ansDtos)
                                .build();
                    }).toList();

                    details = FillBlankDetailsDto.builder()
                            .orderedBlanks(fb != null ? fb.getOrderedBlanks() : null)
                            .blanks(blankDtos)
                            .build();
                }
                case "MATCHING" -> {
                    var left = matchingLeftRepository
                            .findByQuestion_QuestionIdOrderByOrderIndexAsc(question.getQuestionId());
                    var right = matchingRightRepository
                            .findByQuestion_QuestionIdOrderByOrderIndexAsc(question.getQuestionId());
                    var pairs = matchingPairRepository.findByQuestion_QuestionId(question.getQuestionId());

                    var leftDtos = left.stream().map(l -> MatchingItemDto.builder()
                            .id(l.getLeftId())
                            .orderIndex(l.getOrderIndex())
                            .contentHtml(l.getContentHtml())
                            .build()).toList();
                    var rightDtos = right.stream().map(r -> MatchingItemDto.builder()
                            .id(r.getRightId())
                            .orderIndex(r.getOrderIndex())
                            .contentHtml(r.getContentHtml())
                            .build()).toList();
                    var pairDtos = pairs.stream().map(p -> MatchingPairDto.builder()
                            .pairId(p.getPairId())
                            .leftId(p.getLeftId())
                            .rightId(p.getRightId())
                            .build()).toList();

                    details = MatchingDetailsDto.builder()
                            .leftItems(leftDtos)
                            .rightItems(rightDtos)
                            .pairs(pairDtos)
                            .build();
                }
                case "SHORT_ANSWER" -> {
                    var expected = shortAnswerExpectedRepository.findByQuestion_QuestionId(question.getQuestionId());
                    var expectedDtos = expected.stream().map(e -> ShortAnswerExpectedDto.builder()
                            .expectedId(e.getExpectedId())
                            .answerText(e.getAnswerText())
                            .matchRule(e.getMatchRule())
                            .build()).toList();
                    details = ShortAnswerDetailsDto.builder().expectedAnswers(expectedDtos).build();
                }
                case "GROUPING" -> {
                    var groups = groupRepository
                            .findByQuestion_QuestionIdOrderByOrderIndexAsc(question.getQuestionId());
                    var items = groupItemRepository
                            .findByQuestion_QuestionIdOrderByOrderIndexAsc(question.getQuestionId());
                    var itemIds = items.stream().map(QuestionGroupItem::getItemId).toList();
                    var answers = itemIds.isEmpty() ? java.util.List.<QuestionGroupItemAnswer>of()
                            : groupItemAnswerRepository.findByItemIdIn(itemIds);

                    var groupDtos = groups.stream().map(g -> GroupDto.builder()
                            .groupId(g.getGroupId())
                            .orderIndex(g.getOrderIndex())
                            .title(g.getTitle())
                            .build()).toList();
                    var itemDtos = items.stream().map(i -> GroupItemDto.builder()
                            .itemId(i.getItemId())
                            .orderIndex(i.getOrderIndex())
                            .contentHtml(i.getContentHtml())
                            .build()).toList();
                    var answerDtos = answers.stream().map(a -> GroupItemAnswerDto.builder()
                            .answerId(a.getAnswerId())
                            .itemId(a.getItemId())
                            .groupId(a.getGroupId())
                            .build()).toList();

                    details = GroupingDetailsDto.builder()
                            .groups(groupDtos)
                            .items(itemDtos)
                            .itemAnswers(answerDtos)
                            .build();
                }
                default -> {
                    // ignore
                }
            }
        }

        // Load media ids attached to question (optional; can be used by editor)
        // Note: currently not returned in response DTO; can be added later if needed.
        mediaOfQuestionRepository.findById_QuestionId(question.getQuestionId());

        return QuestionResponse.builder().question(base).details(details).build();
    }
}
