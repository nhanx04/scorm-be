package com.scorm.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorm.generator.dto.CourseCreateRequest;
import com.scorm.generator.dto.CourseResponse;
import com.scorm.generator.dto.CourseUpdateRequest;
import com.scorm.generator.entity.Course;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final ObjectMapper objectMapper;

    public CourseServiceImpl(CourseRepository courseRepository, ObjectMapper objectMapper) {
        this.courseRepository = courseRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public CourseResponse create(CourseCreateRequest request, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        Course course = Course.builder()
                .title(request.getTitle())
                .passingScore(request.getPassingScore())
                .attemptLimit(request.getAttemptLimit())
                .durationMin(request.getDurationMin())
                .status(request.getStatus())
                .extraInfor(request.getExtraInfor() != null ? request.getExtraInfor().toString() : null)
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
    public CourseResponse getById(Long courseId, Authentication authentication) {
        Course course = getOwnedCourseOrThrow(courseId, authentication);
        return CourseResponse.fromEntity(course, parseJson(course.getExtraInfor()));
    }

    @Override
    public CourseResponse update(Long courseId, CourseUpdateRequest request, Authentication authentication) {
        Course course = getOwnedCourseOrThrow(courseId, authentication);

        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
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
        if (request.getExtraInfor() != null) {
            course.setExtraInfor(request.getExtraInfor().toString());
        }

        Course saved = courseRepository.save(course);
        return CourseResponse.fromEntity(saved, parseJson(saved.getExtraInfor()));
    }

    @Override
    public void delete(Long courseId, Authentication authentication) {
        Course course = getOwnedCourseOrThrow(courseId, authentication);
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

