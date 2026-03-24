package com.scorm.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.scorm.generator.ScormPackaging.ScormPackageComposer;
import com.scorm.generator.ScormPackaging.ScormPackageStorageService;
import com.scorm.generator.dto.ScormPackageCreateRequest;
import com.scorm.generator.dto.ScormPackageResponse;
import com.scorm.generator.entity.Course;
import com.scorm.generator.entity.ScormExportConfig;
import com.scorm.generator.entity.ScormPackage;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.CourseRepository;
import com.scorm.generator.repository.ScormExportConfigRepository;
import com.scorm.generator.repository.ScormPackageRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ScormPackageServiceImpl implements ScormPackageService {

    private final CourseRepository courseRepository;
    private final ScormExportConfigRepository scormExportConfigRepository;
    private final ScormPackageRepository scormPackageRepository;
    private final ScormPackageComposer scormPackageComposer;
    private final ScormPackageStorageService scormPackageStorageService;
    private final EditorStateExportNormalizer editorStateExportNormalizer;

    public ScormPackageServiceImpl(CourseRepository courseRepository,
            ScormExportConfigRepository scormExportConfigRepository,
            ScormPackageRepository scormPackageRepository,
            ScormPackageComposer scormPackageComposer,
            ScormPackageStorageService scormPackageStorageService,
            EditorStateExportNormalizer editorStateExportNormalizer) {
        this.courseRepository = courseRepository;
        this.scormExportConfigRepository = scormExportConfigRepository;
        this.scormPackageRepository = scormPackageRepository;
        this.scormPackageComposer = scormPackageComposer;
        this.scormPackageStorageService = scormPackageStorageService;
        this.editorStateExportNormalizer = editorStateExportNormalizer;
    }

    @Override
    public ScormPackageResponse createPackage(Long courseId, ScormPackageCreateRequest request,
            Authentication authentication) {
        Course course = getOwnedCourseOrThrow(courseId, authentication);
        User currentUser = (User) authentication.getPrincipal();

        ScormExportConfig config = scormExportConfigRepository.findByCourse_CourseId(courseId)
                .orElseGet(() -> scormExportConfigRepository.save(ScormExportConfig.builder().course(course).build()));

        String packageType = request.getPackageType() == null ? "SCORM_2004" : request.getPackageType();
        String packageName = request.getPackageName() == null ? "course-" + courseId : request.getPackageName();

        JsonNode rawEditorStateSnapshot = request.getEditorStateSnapshot() != null
                ? request.getEditorStateSnapshot()
                : course.getEditorState();
        JsonNode editorStateSnapshot = editorStateExportNormalizer.normalizeForScorm(rawEditorStateSnapshot, course);
        JsonNode themeSnapshot = request.getInterfaceSnapshot() != null
                ? request.getInterfaceSnapshot()
                : (config.getThemeConfig() != null ? config.getThemeConfig() : course.getThemeOverride());

        byte[] zipPayload = scormPackageComposer.compose(
                course.getCourseId(),
                course.getTitle(),
                packageType,
                editorStateSnapshot,
                themeSnapshot);

        ScormPackageStorageService.StoredPackage stored = scormPackageStorageService
                .storeZip(zipPayload, packageName, courseId);

        ScormPackage scormPackage = ScormPackage.builder()
                .packageName(packageName)
                .packageType(packageType)
                .zipFilePath(stored.zipPath().toAbsolutePath().toString())
                .cloudKey(stored.cloudKey())
                .cloudUrl(stored.publicUrl())
                .themeSnapshot(themeSnapshot)
                .course(course)
                .config(config)
                .user(currentUser)
                .build();

        return ScormPackageResponse.fromEntity(scormPackageRepository.save(scormPackage));
    }

    @Override
    public List<ScormPackageResponse> listPackages(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return scormPackageRepository.findByUser_UserIdOrderByScormPackageIdDesc(currentUser.getUserId())
                .stream()
                .map(ScormPackageResponse::fromEntity)
                .toList();
    }

    @Override
    public ScormPackageResponse getPackageById(Long packageId, Authentication authentication) {
        return ScormPackageResponse.fromEntity(getOwnedPackageOrThrow(packageId, authentication));
    }

    @Override
    public ResponseEntity<Resource> downloadPackage(Long packageId, Authentication authentication) {
        ScormPackage scormPackage = getOwnedPackageOrThrow(packageId, authentication);

        if (scormPackage.getZipFilePath() == null) {
            throw new AppException(HttpStatus.NOT_FOUND, "Zip file not found");
        }

        Path zipPath = Paths.get(scormPackage.getZipFilePath());
        if (!Files.exists(zipPath)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Zip file not found");
        }

        Resource resource = new FileSystemResource(zipPath);
        String filename = (scormPackage.getPackageName() == null ? "scorm-" + scormPackage.getScormPackageId()
                : scormPackage.getPackageName()) + ".zip";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @Override
    public void deletePackage(Long packageId, Authentication authentication) {
        ScormPackage scormPackage = getOwnedPackageOrThrow(packageId, authentication);

        if (scormPackage.getZipFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(scormPackage.getZipFilePath()));
            } catch (IOException ignored) {
            }
        }

        scormPackageRepository.delete(scormPackage);
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

    private ScormPackage getOwnedPackageOrThrow(Long packageId, Authentication authentication) {
        if (packageId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "packageId is required");
        }

        ScormPackage scormPackage = scormPackageRepository.findById(packageId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SCORM package not found"));

        User currentUser = (User) authentication.getPrincipal();
        Long ownerId = scormPackage.getUser() != null ? scormPackage.getUser().getUserId() : null;
        if (ownerId == null || !ownerId.equals(currentUser.getUserId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to access this SCORM package");
        }

        return scormPackage;
    }
}
