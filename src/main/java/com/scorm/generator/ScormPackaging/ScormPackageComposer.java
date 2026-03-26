package com.scorm.generator.ScormPackaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ScormPackageComposer {

    private static final List<String> TEMPLATE_FILES = List.of(
            "assets/css/base.css",
            "assets/js/app.js",
            "assets/js/player.js",
            "assets/js/renderer.js",
            "assets/js/scorm.js",
            "assets/js/state.js",
            "assets/js/components/fill-blank.js",
            "assets/js/components/grouping.js",
            "assets/js/components/mcq-multiple.js",
            "assets/js/components/mcq-single.js",
            "assets/js/components/short-answer.js",
            "assets/js/components/true-false.js",
            "index.html");

    private final ObjectMapper objectMapper;

    public ScormPackageComposer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] compose(Long courseId,
            String courseTitle,
            String packageType,
            JsonNode editorStateSnapshot,
            JsonNode themeSnapshot) {
        String exportId = UUID.randomUUID().toString();
        String scormVersion = packageType != null && packageType.contains("1.2") ? "1.2" : "2004";

        try {
            Path workspace = Files.createTempDirectory("scorm-export-");
            copyTemplate(workspace);
            writeDataFiles(workspace, exportId, courseId, courseTitle, scormVersion, editorStateSnapshot, themeSnapshot);
            writeManifest(workspace, exportId, courseTitle, scormVersion);

            byte[] zip = zipDirectory(workspace);
            deleteDirectory(workspace);
            return zip;
        } catch (IOException e) {
            throw new RuntimeException("Failed to compose SCORM package", e);
        }
    }

    private void copyTemplate(Path workspace) throws IOException {
        for (String templateFile : TEMPLATE_FILES) {
            Path target = workspace.resolve(templateFile);
            Files.createDirectories(target.getParent());
            ClassPathResource resource = new ClassPathResource("scorm-template/" + templateFile);
            try (InputStream inputStream = resource.getInputStream()) {
                Files.write(target, inputStream.readAllBytes());
            }
        }
    }

    private void writeDataFiles(Path workspace,
            String exportId,
            Long courseId,
            String courseTitle,
            String scormVersion,
            JsonNode editorStateSnapshot,
            JsonNode themeSnapshot) throws IOException {
        Path dataDir = workspace.resolve("data");
        Files.createDirectories(dataDir);

        ObjectNode courseNode = objectMapper.createObjectNode();
        courseNode.put("exportId", exportId);
        courseNode.put("courseId", courseId == null ? 0 : courseId);
        courseNode.put("courseTitle", courseTitle == null ? "Untitled Course" : courseTitle);
        courseNode.put("scormVersion", scormVersion);
        courseNode.set("editorStateSnapshot", editorStateSnapshot == null ? objectMapper.createObjectNode() : editorStateSnapshot);
        courseNode.set("themeSnapshot", themeSnapshot == null ? objectMapper.createObjectNode() : themeSnapshot);

        Files.writeString(dataDir.resolve("course.json"), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(courseNode), StandardCharsets.UTF_8);
        Files.writeString(dataDir.resolve("editor-state.json"), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(editorStateSnapshot == null ? objectMapper.createObjectNode() : editorStateSnapshot), StandardCharsets.UTF_8);
        Files.writeString(dataDir.resolve("theme.json"), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(themeSnapshot == null ? objectMapper.createObjectNode() : themeSnapshot), StandardCharsets.UTF_8);
    }

    private void writeManifest(Path workspace, String exportId, String courseTitle, String scormVersion) throws IOException {
        String schema = "1.2".equals(scormVersion) ? "ADL SCORM" : "ADL SCORM";
        String schemaVersion = "1.2".equals(scormVersion) ? "1.2" : "2004 4th Edition";

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<manifest identifier=\"MANIFEST-" + exportId + "\" version=\"1.0\" "
                + "xmlns=\"http://www.imsglobal.org/xsd/imscp_v1p1\" "
                + "xmlns:adlcp=\"http://www.adlnet.org/xsd/adlcp_v1p3\" "
                + "xmlns:imsss=\"http://www.imsglobal.org/xsd/imsss\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                + "  <metadata>\n"
                + "    <schema>" + schema + "</schema>\n"
                + "    <schemaversion>" + schemaVersion + "</schemaversion>\n"
                + "  </metadata>\n"
                + "  <organizations default=\"ORG-1\">\n"
                + "    <organization identifier=\"ORG-1\">\n"
                + "      <title>" + escapeXml(courseTitle == null ? "Untitled Course" : courseTitle) + "</title>\n"
                + "      <item identifier=\"ITEM-1\" identifierref=\"RES-1\">\n"
                + "        <title>" + escapeXml(courseTitle == null ? "Untitled Course" : courseTitle) + "</title>\n"
                + "      </item>\n"
                + "    </organization>\n"
                + "  </organizations>\n"
                + "  <resources>\n"
                + "    <resource identifier=\"RES-1\" type=\"webcontent\" adlcp:scormType=\"sco\" href=\"index.html\">\n"
                + "      <file href=\"index.html\"/>\n"
                + "      <file href=\"data/course.json\"/>\n"
                + "      <file href=\"data/editor-state.json\"/>\n"
                + "      <file href=\"data/theme.json\"/>\n"
                + "      <file href=\"assets/css/base.css\"/>\n"
                + "      <file href=\"assets/js/app.js\"/>\n"
                + "      <file href=\"assets/js/player.js\"/>\n"
                + "      <file href=\"assets/js/renderer.js\"/>\n"
                + "      <file href=\"assets/js/scorm.js\"/>\n"
                + "      <file href=\"assets/js/state.js\"/>\n"
                + "      <file href=\"assets/js/components/fill-blank.js\"/>\n"
                + "      <file href=\"assets/js/components/grouping.js\"/>\n"
                + "      <file href=\"assets/js/components/mcq-multiple.js\"/>\n"
                + "      <file href=\"assets/js/components/mcq-single.js\"/>\n"
                + "      <file href=\"assets/js/components/short-answer.js\"/>\n"
                + "      <file href=\"assets/js/components/true-false.js\"/>\n"
                + "    </resource>\n"
                + "  </resources>\n"
                + "</manifest>\n";

        Files.writeString(workspace.resolve("imsmanifest.xml"), xml, StandardCharsets.UTF_8);
    }

    private byte[] zipDirectory(Path sourceDir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Files.walk(sourceDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String entryName = sourceDir.relativize(path).toString().replace('\\', '/');
                        try (InputStream is = Files.newInputStream(path)) {
                            zos.putNextEntry(new ZipEntry(entryName));
                            is.transferTo(zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return baos.toByteArray();
    }

    private void deleteDirectory(Path path) {
        try {
            Files.walk(path)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

