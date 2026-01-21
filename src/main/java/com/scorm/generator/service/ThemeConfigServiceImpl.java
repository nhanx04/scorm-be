package com.scorm.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.scorm.generator.entity.Course;
import com.scorm.generator.entity.Page;
import com.scorm.generator.entity.ScormExportConfig;
import com.scorm.generator.entity.Section;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.CourseRepository;
import com.scorm.generator.repository.PageRepository;
import com.scorm.generator.repository.ScormExportConfigRepository;
import com.scorm.generator.repository.SectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ThemeConfigServiceImpl implements ThemeConfigService {

    private final CourseRepository courseRepository;
    private final ScormExportConfigRepository scormExportConfigRepository;
    private final SectionRepository sectionRepository;
    private final PageRepository pageRepository;

    public ThemeConfigServiceImpl(
            CourseRepository courseRepository,
            ScormExportConfigRepository scormExportConfigRepository,
            SectionRepository sectionRepository,
            PageRepository pageRepository) {
        this.courseRepository = courseRepository;
        this.scormExportConfigRepository = scormExportConfigRepository;
        this.sectionRepository = sectionRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    public JsonNode upsertCourseTheme(Long courseId, JsonNode themeConfig, Authentication authentication) {
        Course course = getOwnedCourseOrThrow(courseId, authentication);

        ScormExportConfig config = scormExportConfigRepository.findByCourse_CourseId(courseId)
                .orElseGet(() -> ScormExportConfig.builder().course(course).build());

        config.setThemeConfig(themeConfig);

        return scormExportConfigRepository.save(config).getThemeConfig();
    }

    @Override
    public JsonNode upsertSectionTheme(Long sectionId, JsonNode themeOverride, Authentication authentication) {
        Section section = getOwnedSectionOrThrow(sectionId, authentication);
        section.setThemeOverride(themeOverride);
        return sectionRepository.save(section).getThemeOverride();
    }

    @Override
    public JsonNode upsertPageTheme(Long pageId, JsonNode themeOverride, Authentication authentication) {
        Page page = getOwnedPageOrThrow(pageId, authentication);
        page.setThemeOverride(themeOverride);
        return pageRepository.save(page).getThemeOverride();
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

    private Section getOwnedSectionOrThrow(Long sectionId, Authentication authentication) {
        if (sectionId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "sectionId is required");
        }

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Section not found"));

        User currentUser = (User) authentication.getPrincipal();
        Long ownerId = section.getCourse() != null && section.getCourse().getUser() != null
                ? section.getCourse().getUser().getUserId()
                : null;

        if (ownerId == null || !ownerId.equals(currentUser.getUserId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to access this section");
        }

        return section;
    }

    private Page getOwnedPageOrThrow(Long pageId, Authentication authentication) {
        if (pageId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "pageId is required");
        }

        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Page not found"));

        User currentUser = (User) authentication.getPrincipal();
        Long ownerId = page.getSection() != null && page.getSection().getCourse() != null
                && page.getSection().getCourse().getUser() != null
                        ? page.getSection().getCourse().getUser().getUserId()
                        : null;

        if (ownerId == null || !ownerId.equals(currentUser.getUserId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to access this page");
        }

        return page;
    }
}

