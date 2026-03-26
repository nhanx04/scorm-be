package com.scorm.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorm.generator.dto.ContentBlockResponse;
import com.scorm.generator.dto.CourseCreateRequest;
import com.scorm.generator.dto.CourseDetailResponse;
import com.scorm.generator.dto.CourseResponse;
import com.scorm.generator.dto.CourseUpdateRequest;
import com.scorm.generator.dto.QuizPageResponse;
import com.scorm.generator.dto.ThumbnailOfCourseResponse;
import com.scorm.generator.dto.Question.QuestionSummaryDto;
import com.scorm.generator.entity.Course;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.ContentBlockRepository;
import com.scorm.generator.repository.CourseRepository;
import com.scorm.generator.repository.PageRepository;
import com.scorm.generator.repository.ScormExportConfigRepository;
import com.scorm.generator.repository.ScormPackageRepository;
import com.scorm.generator.repository.SectionRepository;
import com.scorm.generator.repository.ThumbnailOfCourseRepository;
import com.scorm.generator.repository.Question.QuestionOfQuizRepository;
import com.scorm.generator.repository.Question.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final PageRepository pageRepository;
    private final ContentBlockRepository contentBlockRepository;
    private final ThumbnailOfCourseRepository thumbnailOfCourseRepository;
    private final QuestionOfQuizRepository questionOfQuizRepository;
    private final QuestionRepository questionRepository;
    private final ScormExportConfigRepository scormExportConfigRepository;
    private final ScormPackageRepository scormPackageRepository;
    private final ObjectMapper objectMapper;

    public CourseServiceImpl(
            CourseRepository courseRepository,
            SectionRepository sectionRepository,
            PageRepository pageRepository,
            ContentBlockRepository contentBlockRepository,
            ThumbnailOfCourseRepository thumbnailOfCourseRepository,
            QuestionOfQuizRepository questionOfQuizRepository,
            QuestionRepository questionRepository,
            ScormExportConfigRepository scormExportConfigRepository,
            ScormPackageRepository scormPackageRepository,
            ObjectMapper objectMapper) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.pageRepository = pageRepository;
        this.contentBlockRepository = contentBlockRepository;
        this.thumbnailOfCourseRepository = thumbnailOfCourseRepository;
        this.questionOfQuizRepository = questionOfQuizRepository;
        this.questionRepository = questionRepository;
        this.scormExportConfigRepository = scormExportConfigRepository;
        this.scormPackageRepository = scormPackageRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public CourseResponse create(CourseCreateRequest request, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            title = "Untitled Course";
        }

        BigDecimal passingScore = request.getPassingScore() != null ? request.getPassingScore() : BigDecimal.ZERO;
        Integer attemptLimit = request.getAttemptLimit() != null ? request.getAttemptLimit() : 0;
        Integer durationMin = request.getDurationMin() != null ? request.getDurationMin() : 0;
        String status = request.getStatus() != null ? request.getStatus() : "DRAFT";
        String editorVersion = request.getEditorVersion() != null ? request.getEditorVersion() : "course-editor-v1";
        String editorStatus = request.getEditorStatus() != null ? request.getEditorStatus() : "DRAFT";

        Course course = Course.builder()
                .title(title)
                .description(request.getDescription())
                .coverImageUrl(request.getCoverImageUrl())
                .passingScore(passingScore)
                .attemptLimit(attemptLimit)
                .durationMin(durationMin)
                .status(status)
                .textHtml(request.getTextHtml())
                .themeOverride(request.getThemeOverride())
                .layoutMode(request.getLayoutMode())
                .layoutMeta(request.getLayoutMeta())
                .extraInfor(request.getExtraInfor() != null ? request.getExtraInfor().toString() : "{}")
                .editorState(request.getEditorState())
                .editorVersion(editorVersion)
                .editorStatus(editorStatus)
                .user(currentUser)
                .build();

        Course saved = courseRepository.save(course);
        return CourseResponse.fromEntity(saved, parseJson(saved.getExtraInfor()));
    }

    @Override
    public List<CourseResponse> listMine(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return courseRepository.findByUser_UserIdOrderByCreatedAtDesc(currentUser.getUserId())
                .stream()
                .map(c -> CourseResponse.fromEntity(c, parseJson(c.getExtraInfor())))
                .toList();
    }

    @Override
    public CourseDetailResponse getById(Long courseId, Authentication authentication) {
        Course course = getOwnedCourseOrThrow(courseId, authentication);

        JsonNode extraInfor = parseJson(course.getExtraInfor());

        java.util.List<CourseDetailResponse.SectionDetailDto> sections = sectionRepository
                .findByCourse_CourseIdOrderByOrderIndexAsc(courseId)
                .stream()
                .map(section -> {
                    java.util.List<CourseDetailResponse.PageDetailDto> pages = pageRepository
                            .findBySection_SectionIdOrderByOrderIndexAsc(section.getSectionId())
                            .stream()
                            .map(page -> {
                                CourseDetailResponse.ContentPageDetailDto contentPageDetail = null;
                                if (page.getContentPage() != null) {
                                    java.util.List<ContentBlockResponse> blocks = contentBlockRepository
                                            .findByContentPage_PageIdOrderByOrderIndexAsc(page.getPageId())
                                            .stream()
                                            .map(ContentBlockResponse::fromEntity)
                                            .toList();

                                    contentPageDetail = CourseDetailResponse.ContentPageDetailDto.builder()
                                            .pageId(page.getContentPage().getPageId())
                                            .layoutMode(page.getContentPage().getLayoutMode())
                                            .layoutType(page.getContentPage().getLayoutType())
                                            .layoutMeta(page.getContentPage().getLayoutMeta())
                                            .blocks(blocks)
                                            .build();
                                }

                                QuizPageResponse quizPageResponse = null;
                                if (page.getQuizPage() != null) {
                                    quizPageResponse = QuizPageResponse.fromEntity(page.getQuizPage());
                                    var questionLinks = questionOfQuizRepository.findById_QuizPageId(page.getPageId());
                                    if (!questionLinks.isEmpty()) {
                                        var questionIds = questionLinks.stream()
                                                .map(q -> q.getId().getQuizQuestionId())
                                                .toList();
                                        var summaries = questionRepository.findSummariesByIds(questionIds);
                                        var summaryDtos = summaries.stream()
                                                .map(s -> QuestionSummaryDto.builder()
                                                        .questionId(s.getQuestionId())
                                                        .title(s.getTitle())
                                                        .instruction(s.getInstruction())
                                                        .promptHtml(s.getPromptHtml())
                                                        .textHtml(s.getTextHtml())
                                                        .questionType(s.getQuestionType())
                                                        .themeOverride(s.getThemeOverride())
                                                        .layoutMode(s.getLayoutMode())
                                                        .layoutMeta(s.getLayoutMeta())
                                                        .templateData(s.getTemplateData())
                                                        .points(s.getPoints())
                                                        .shuffleOptions(s.getShuffleOptions())
                                                        .caseSensitive(s.getCaseSensitive())
                                                        .extraConfig(s.getExtraConfig())
                                                        .build())
                                                .toList();
                                        quizPageResponse.setQuestions(summaryDtos);
                                    }
                                }

                                return CourseDetailResponse.PageDetailDto.builder()
                                        .pageId(page.getPageId())
                                        .title(page.getTitle())
                                        .orderIndex(page.getOrderIndex())
                                        .pageType(page.getPageType())
                                        .textHtml(page.getTextHtml())
                                        .themeOverride(page.getThemeOverride())
                                        .layoutMode(page.getLayoutMode())
                                        .layoutType(page.getLayoutType())
                                        .layoutMeta(page.getLayoutMeta())
                                        .contentPage(contentPageDetail)
                                        .quizPage(quizPageResponse)
                                        .build();
                            })
                            .toList();

                    return CourseDetailResponse.SectionDetailDto.builder()
                            .sectionId(section.getSectionId())
                            .title(section.getTitle())
                            .description(section.getDescription())
                            .orderIndex(section.getOrderIndex())
                            .learningObjective(section.getLearningObjective())
                            .textHtml(section.getTextHtml())
                            .themeOverride(section.getThemeOverride())
                            .layoutMode(section.getLayoutMode())
                            .layoutMeta(section.getLayoutMeta())
                            .pages(pages)
                            .build();
                })
                .toList();

        ThumbnailOfCourseResponse thumbnail = null;
        if (thumbnailOfCourseRepository.existsById(courseId)) {
            thumbnail = ThumbnailOfCourseResponse
                    .fromEntity(thumbnailOfCourseRepository.findById(courseId).orElse(null));
        }

        return CourseDetailResponse.builder()
                .courseId(course.getCourseId())
                .title(course.getTitle())
                .description(course.getDescription())
                .coverImageUrl(course.getCoverImageUrl())
                .passingScore(course.getPassingScore())
                .attemptLimit(course.getAttemptLimit())
                .durationMin(course.getDurationMin())
                .status(course.getStatus())
                .textHtml(course.getTextHtml())
                .themeOverride(course.getThemeOverride())
                .layoutMode(course.getLayoutMode())
                .layoutMeta(course.getLayoutMeta())
                .lastPublishedAt(course.getLastPublishedAt())
                .updatedAt(course.getUpdatedAt())
                .extraInfor(extraInfor)
                .editorState(course.getEditorState())
                .editorVersion(course.getEditorVersion())
                .editorStatus(course.getEditorStatus())
                .createdAt(course.getCreatedAt())
                .courseUserId(course.getUser() != null ? course.getUser().getUserId() : null)
                .thumbnail(thumbnail)
                .sections(sections)
                .build();
    }

    @Override
    public CourseResponse update(Long courseId, CourseUpdateRequest request, Authentication authentication) {
        Course course = getOwnedCourseOrThrow(courseId, authentication);

        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getCoverImageUrl() != null) {
            course.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getPassingScore() != null) {
            course.setPassingScore(request.getPassingScore());
        }
        if (request.getAttemptLimit() != null) {
            course.setAttemptLimit(request.getAttemptLimit());
        }
        if (request.getDurationMin() != null) {
            course.setDurationMin(request.getDurationMin());
        }
        if (request.getStatus() != null) {
            course.setStatus(request.getStatus());
        }
        if (request.getTextHtml() != null) {
            course.setTextHtml(request.getTextHtml());
        }
        if (request.getThemeOverride() != null) {
            course.setThemeOverride(request.getThemeOverride());
        }
        if (request.getLayoutMode() != null) {
            course.setLayoutMode(request.getLayoutMode());
        }
        if (request.getLayoutMeta() != null) {
            course.setLayoutMeta(request.getLayoutMeta());
        }
        if (request.getExtraInfor() != null) {
            course.setExtraInfor(request.getExtraInfor().toString());
        }
        if (request.getEditorState() != null) {
            course.setEditorState(request.getEditorState());
        }
        if (request.getEditorVersion() != null) {
            course.setEditorVersion(request.getEditorVersion());
        }
        if (request.getEditorStatus() != null) {
            course.setEditorStatus(request.getEditorStatus());
        }

        Course saved = courseRepository.save(course);
        return CourseResponse.fromEntity(saved, parseJson(saved.getExtraInfor()));
    }

    @Override
    @Transactional
    public void delete(Long courseId, Authentication authentication) {
        Course course = getOwnedCourseOrThrow(courseId, authentication);
        scormPackageRepository.deleteByCourse_CourseId(courseId);
        scormExportConfigRepository.deleteByCourse_CourseId(courseId);
        courseRepository.delete(course);
    }

    private Course getOwnedCourseOrThrow(Long courseId, Authentication authentication) {
        if (courseId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "courseId is required");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Course not found"));

        User currentUser = (User) authentication.getPrincipal();
        Long ownerId = course.getUser() != null ? course.getUser().getUserId() : null;
        if (ownerId == null || !ownerId.equals(currentUser.getUserId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to access this course");
        }

        return course;
    }

    private JsonNode parseJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }
}
