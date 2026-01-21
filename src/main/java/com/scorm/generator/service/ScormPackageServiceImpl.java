package com.scorm.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ScormPackageServiceImpl implements ScormPackageService {

    private final CourseRepository courseRepository;
    private final ScormExportConfigRepository scormExportConfigRepository;
    private final ScormPackageRepository scormPackageRepository;
    private final ObjectMapper objectMapper;

    private final Path exportDir;

    public ScormPackageServiceImpl(
            CourseRepository courseRepository,
            ScormExportConfigRepository scormExportConfigRepository,
            ScormPackageRepository scormPackageRepository,
            ObjectMapper objectMapper,
            @Value("${app.scorm.export-dir:scorm-exports}") String exportDir) {
        this.courseRepository = courseRepository;
        this.scormExportConfigRepository = scormExportConfigRepository;
        this.scormPackageRepository = scormPackageRepository;
        this.objectMapper = objectMapper;
        this.exportDir = Paths.get(exportDir);
    }

    @Override
    public ScormPackageResponse createPackage(Long courseId, ScormPackageCreateRequest request,
            Authentication authentication) {
        Course course = getOwnedCourseOrThrow(courseId, authentication);
        User currentUser = (User) authentication.getPrincipal();

        ScormExportConfig config = scormExportConfigRepository.findByCourse_CourseId(courseId)
                .orElseGet(() -> scormExportConfigRepository.save(ScormExportConfig.builder().course(course).build()));

        JsonNode themeSnapshot = config.getThemeConfig();

        String packageType = request.getPackageType() == null ? "SCORM_2004" : request.getPackageType();
        String packageName = request.getPackageName() == null ? ("course-" + courseId + "-" + OffsetDateTime.now())
                : request.getPackageName();

        Path zipPath = generateMinimalScormZip(courseId);

        ScormPackage scormPackage = ScormPackage.builder()
                .packageName(packageName)
                .packageType(packageType)
                .zipFilePath(zipPath.toAbsolutePath().toString())
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
        ScormPackage scormPackage = getOwnedPackageOrThrow(packageId, authentication);
        return ScormPackageResponse.fromEntity(scormPackage);
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
        String filename = (scormPackage.getPackageName() == null ? ("scorm-" + scormPackage.getScormPackageId())
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

    private Path generateMinimalScormZip(Long courseId) {
        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot create export directory");
        }

        String filename = "scorm-" + courseId + "-" + UUID.randomUUID() + ".zip";
        Path zipPath = exportDir.resolve(filename);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            String manifest = buildMinimalManifest(courseId);
            zos.putNextEntry(new ZipEntry("imsmanifest.xml"));
            zos.write(manifest.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            String indexHtml = "<html><head><meta charset=\"utf-8\"/></head><body>SCORM package for course "
                    + courseId + "</body></html>";
            zos.putNextEntry(new ZipEntry("index.html"));
            zos.write(indexHtml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate zip");
        }

        return zipPath;
    }

    private String buildMinimalManifest(Long courseId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<manifest identifier=\"manifest-" + courseId + "\" version=\"1.0\"\n" +
                "  xmlns=\"http://www.imsglobal.org/xsd/imscp_v1p1\"\n" +
                "  xmlns:adlcp=\"http://www.adlnet.org/xsd/adlcp_v1p3\"\n" +
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "  xsi:schemaLocation=\"http://www.imsglobal.org/xsd/imscp_v1p1 imscp_v1p1.xsd\">\n" +
                "  <organizations default=\"ORG-1\">\n" +
                "    <organization identifier=\"ORG-1\">\n" +
                "      <title>Course " + courseId + "</title>\n" +
                "      <item identifier=\"ITEM-1\" identifierref=\"RES-1\">\n" +
                "        <title>Index</title>\n" +
                "      </item>\n" +
                "    </organization>\n" +
                "  </organizations>\n" +
                "  <resources>\n" +
                "    <resource identifier=\"RES-1\" type=\"webcontent\" adlcp:scormType=\"sco\" href=\"index.html\">\n"
                +
                "      <file href=\"index.html\"/>\n" +
                "    </resource>\n" +
                "  </resources>\n" +
                "</manifest>\n";
    }

    private JsonNode toJsonNode(Object obj) {
        if (obj == null) {
            return null;
        }
        return objectMapper.valueToTree(obj);
    }
}

