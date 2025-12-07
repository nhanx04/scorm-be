package com.scorm.generator.service;

import com.scorm.generator.entity.ScormPackage;

import com.scorm.generator.ScormPackageExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ScormExportService {

    private final ScormPackageExporter packageExporter;
    private final AwsS3Service s3Service;

    /**
     * Exports a SCORM package to a byte array, then uploads it to AWS S3.
     *
     * @param scormPackage The package to export.
     * @return The public URL of the uploaded file in Google Cloud Storage.
     * @throws IOException If an I/O error occurs.
     */
    public String exportPackage(ScormPackage scormPackage) throws IOException {
        String packageName = generatePackageName(scormPackage);

        // Export the package to an in-memory byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        packageExporter.exportPackageToStream(scormPackage, baos);
        byte[] packageData = baos.toByteArray();

        // Upload the byte array to Amazon S3
        return s3Service.uploadFile(packageData, packageName, "application/zip");
    }

    private String generatePackageName(ScormPackage scormPackage) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String title = scormPackage.getTitle()
                .replaceAll("[^a-zA-Z0-9_-]", "_")
                .replaceAll("_+", "_");
        // Ensure the filename is not too long and ends cleanly
        if (title.length() > 50) {
            title = title.substring(0, 50);
        }
        return "scorm/" + title + "_" + timestamp + ".zip"; // Added a folder for organization
    }
}
