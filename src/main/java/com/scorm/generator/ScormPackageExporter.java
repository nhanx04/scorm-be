package com.scorm.generator;

import com.scorm.generator.entity.ScormPackage;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Exports SCORM packages as ZIP files
 */
@org.springframework.stereotype.Service
@lombok.RequiredArgsConstructor
public class ScormPackageExporter {

    private final ScormManifestGenerator manifestGenerator;
    private final HtmlContentGenerator htmlGenerator;

    public void exportPackageToStream(ScormPackage scormPackage, OutputStream outputStream) throws IOException {
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(outputStream)) {

            // Set encoding to UTF-8
            zos.setEncoding("UTF-8");

            // Add manifest file
            addManifestToZip(zos, scormPackage);

            // Add HTML content
            addHtmlContentToZip(zos, scormPackage);

            // Add JavaScript files
            addJavaScriptFilesToZip(zos);

            // Add CSS file
            addCssFileToZip(zos);

            zos.finish();
        }
    }

    private void addManifestToZip(ZipArchiveOutputStream zos, ScormPackage scormPackage) throws IOException {
        String manifestContent = manifestGenerator.generateManifest(scormPackage);

        ZipArchiveEntry entry = new ZipArchiveEntry("imsmanifest.xml");
        entry.setSize(manifestContent.getBytes(StandardCharsets.UTF_8).length);
        zos.putArchiveEntry(entry);

        zos.write(manifestContent.getBytes(StandardCharsets.UTF_8));
        zos.closeArchiveEntry();
    }

    private void addHtmlContentToZip(ZipArchiveOutputStream zos, ScormPackage scormPackage) throws IOException {
        String htmlContent = htmlGenerator.generateIndexHtml(scormPackage);

        ZipArchiveEntry entry = new ZipArchiveEntry("index.html");
        entry.setSize(htmlContent.getBytes(StandardCharsets.UTF_8).length);
        zos.putArchiveEntry(entry);

        zos.write(htmlContent.getBytes(StandardCharsets.UTF_8));
        zos.closeArchiveEntry();
    }

    private void addJavaScriptFilesToZip(ZipArchiveOutputStream zos) throws IOException {
        // Add SCORM API JavaScript
        addResourceFileToZip(zos, "scorm_api.js", "scorm_api.js");

        // Add Quiz JavaScript
        addResourceFileToZip(zos, "quiz.js", "quiz.js");
    }

    private void addCssFileToZip(ZipArchiveOutputStream zos) throws IOException {
        addResourceFileToZip(zos, "style.css", "style.css");
    }

    private void addResourceFileToZip(ZipArchiveOutputStream zos, String resourcePath, String zipEntryName)
            throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }

            byte[] content = IOUtils.toByteArray(is);

            ZipArchiveEntry entry = new ZipArchiveEntry(zipEntryName);
            entry.setSize(content.length);
            zos.putArchiveEntry(entry);

            zos.write(content);
            zos.closeArchiveEntry();
        }
    }

    public String generatePackageName(ScormPackage scormPackage) {
        String safeName = scormPackage.getTitle()
                .replaceAll("[^a-zA-Z0-9\\s-_]", "")
                .replaceAll("\\s+", "_")
                .toLowerCase();

        if (safeName.length() > 50) {
            safeName = safeName.substring(0, 50);
        }

        return safeName + "_scorm_package.zip";
    }
}
