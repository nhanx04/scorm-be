package com.scorm.generator.service;

import com.scorm.generator.dto.ThumbnailOfCourseResponse;
import com.scorm.generator.dto.ThumbnailOfCourseUpsertRequest;
import com.scorm.generator.entity.Course;
import com.scorm.generator.entity.ImageAsset;
import com.scorm.generator.entity.ThumbnailOfCourse;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.CourseRepository;
import com.scorm.generator.repository.ImageAssetRepository;
import com.scorm.generator.repository.ThumbnailOfCourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ThumbnailOfCourseServiceImpl implements ThumbnailOfCourseService {

    private final ThumbnailOfCourseRepository thumbnailOfCourseRepository;
    private final CourseRepository courseRepository;
    private final ImageAssetRepository imageAssetRepository;

    public ThumbnailOfCourseServiceImpl(ThumbnailOfCourseRepository thumbnailOfCourseRepository,
                                       CourseRepository courseRepository,
                                       ImageAssetRepository imageAssetRepository) {
        this.thumbnailOfCourseRepository = thumbnailOfCourseRepository;
        this.courseRepository = courseRepository;
        this.imageAssetRepository = imageAssetRepository;
    }

    @Override
    public ThumbnailOfCourseResponse upsert(Long courseId, ThumbnailOfCourseUpsertRequest request, Authentication authentication) {
        Course course = getOwnedCourseOrThrow(courseId, authentication);

        if (request.getImageMediaId() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "imageMediaId is required");
        }

        ImageAsset imageAsset = imageAssetRepository.findById(request.getImageMediaId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "ImageAsset not found"));

        ThumbnailOfCourse toc = thumbnailOfCourseRepository.findById(courseId)
                .orElse(ThumbnailOfCourse.builder().course(course).build());

        toc.setImageAsset(imageAsset);

        return ThumbnailOfCourseResponse.fromEntity(thumbnailOfCourseRepository.save(toc));
    }

    @Override
    public ThumbnailOfCourseResponse getByCourseId(Long courseId, Authentication authentication) {
        getOwnedCourseOrThrow(courseId, authentication);

        ThumbnailOfCourse toc = thumbnailOfCourseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "ThumbnailOfCourse not found"));

        return ThumbnailOfCourseResponse.fromEntity(toc);
    }

    @Override
    public void delete(Long courseId, Authentication authentication) {
        getOwnedCourseOrThrow(courseId, authentication);

        if (!thumbnailOfCourseRepository.existsById(courseId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "ThumbnailOfCourse not found");
        }

        thumbnailOfCourseRepository.deleteById(courseId);
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
}

