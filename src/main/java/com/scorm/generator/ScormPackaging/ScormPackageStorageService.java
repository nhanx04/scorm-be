package com.scorm.generator.ScormPackaging;

import com.scorm.generator.service.S3StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ScormPackageStorageService {

    public record StoredPackage(Path zipPath, String cloudKey, String publicUrl) {
    }

    private final S3StorageService storageService;
    private final String publicBaseUrl;

    public ScormPackageStorageService(S3StorageService storageService,
            @Value("${app.r2.public-base-url}") String publicBaseUrl) {
        this.storageService = storageService;
        this.publicBaseUrl = publicBaseUrl;
    }

    public StoredPackage storeZip(byte[] zipPayload, String packageName, Long courseId) {
        try {
            Path tempDir = Files.createTempDirectory("scorm-packages-");
            String safeName = packageName == null ? "package" : packageName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path zipPath = tempDir.resolve(safeName + ".zip");
            Files.write(zipPath, zipPayload);

            String cloudKey = storageService.uploadBytes("scorm-packages/" + (courseId == null ? "unknown" : courseId),
                    safeName + ".zip",
                    zipPayload,
                    "application/zip");
            String publicUrl = publicBaseUrl + "/" + cloudKey;
            return new StoredPackage(zipPath, cloudKey, publicUrl);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store SCORM package", e);
        }
    }
}

