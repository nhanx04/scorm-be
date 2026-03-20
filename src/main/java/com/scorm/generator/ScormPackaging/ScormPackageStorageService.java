package com.scorm.generator.ScormPackaging;

import com.scorm.generator.service.S3StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ScormPackageStorageService {

    private final Path exportDir;
    private final S3StorageService s3StorageService;
    private final String publicBaseUrl;

    public ScormPackageStorageService(S3StorageService s3StorageService,
            @Value("${app.scorm.export-dir:scorm-exports}") String exportDir,
            @Value("${app.r2.public-base-url:}") String publicBaseUrl) {
        this.s3StorageService = s3StorageService;
        this.exportDir = Paths.get(exportDir);
        this.publicBaseUrl = publicBaseUrl;
    }

    public StoredPackage storeZip(byte[] zipPayload, String packageName, Long courseId) {
        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create export directory", e);
        }

        String safeName = packageName == null ? "scorm" : packageName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = safeName + "-" + UUID.randomUUID() + ".zip";
        Path zipPath = exportDir.resolve(fileName);

        try {
            Files.write(zipPath, zipPayload);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write SCORM zip", e);
        }

        String cloudKey = s3StorageService.uploadBytes(buildCloudPrefix(courseId), fileName, zipPayload,
                "application/zip");
        String publicUrl = buildPublicUrl(cloudKey);

        return new StoredPackage(zipPath, cloudKey, publicUrl);
    }

    private String buildCloudPrefix(Long courseId) {
        return "scorm-packages/" + (courseId == null ? "unknown" : courseId);
    }

    private String buildPublicUrl(String key) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return null;
        }
        return publicBaseUrl.endsWith("/") ? publicBaseUrl + key : publicBaseUrl + "/" + key;
    }

    public record StoredPackage(Path zipPath, String cloudKey, String publicUrl) {
    }
}
