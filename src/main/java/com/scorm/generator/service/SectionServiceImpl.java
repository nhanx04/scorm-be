package com.scorm.generator.service;

import com.scorm.generator.dto.SectionCreateRequest;
import com.scorm.generator.dto.SectionResponse;
import com.scorm.generator.dto.SectionUpdateRequest;
import com.scorm.generator.entity.Course;
import com.scorm.generator.entity.Section;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.CourseRepository;
import com.scorm.generator.repository.SectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;

    public SectionServiceImpl(SectionRepository sectionRepository, CourseRepository courseRepository) {
        this.sectionRepository = sectionRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public SectionResponse create(Long courseId, SectionCreateRequest request, Authentication authentication) {
        Course course = getOwnedCourseOrThrow(courseId, authentication);

        Section section = Section.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .orderIndex(request.getOrderIndex())
                .learningObjective(request.getLearningObjective())
                .textHtml(request.getTextHtml())
                .themeOverride(request.getThemeOverride())
                .layoutMode(request.getLayoutMode())
                .layoutMeta(request.getLayoutMeta())
                .course(course)
                .build();

        return SectionResponse.fromEntity(sectionRepository.save(section));
    }

    @Override
    public SectionResponse update(Long sectionId, SectionUpdateRequest request, Authentication authentication) {
        Section section = getOwnedSectionOrThrow(sectionId, authentication);

        if (request.getTitle() != null) {
            section.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            section.setDescription(request.getDescription());
        }
        if (request.getOrderIndex() != null) {
            section.setOrderIndex(request.getOrderIndex());
        }
        if (request.getLearningObjective() != null) {
            section.setLearningObjective(request.getLearningObjective());
        }
        if (request.getTextHtml() != null) {
            section.setTextHtml(request.getTextHtml());
        }
        if (request.getThemeOverride() != null) {
            section.setThemeOverride(request.getThemeOverride());
        }
        if (request.getLayoutMode() != null) {
            section.setLayoutMode(request.getLayoutMode());
        }
        if (request.getLayoutMeta() != null) {
            section.setLayoutMeta(request.getLayoutMeta());
        }

        return SectionResponse.fromEntity(sectionRepository.save(section));
    }

    @Override
    public void delete(Long sectionId, Authentication authentication) {
        Section section = getOwnedSectionOrThrow(sectionId, authentication);
        sectionRepository.delete(section);
    }

    @Override
    public java.util.List<SectionResponse> listByCourseId(Long courseId, Authentication authentication) {
        getOwnedCourseOrThrow(courseId, authentication);
        return sectionRepository.findByCourse_CourseIdOrderByOrderIndexAsc(courseId)
                .stream()
                .map(SectionResponse::fromEntity)
                .toList();
    }

    @Override
    public SectionResponse getById(Long sectionId, Authentication authentication) {
        Section section = getOwnedSectionOrThrow(sectionId, authentication);
        return SectionResponse.fromEntity(section);
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
}
