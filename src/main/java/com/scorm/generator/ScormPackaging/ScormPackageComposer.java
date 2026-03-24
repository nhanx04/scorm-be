package com.scorm.generator.ScormPackaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ScormPackageComposer {

    private final ObjectMapper objectMapper;
    private final ScormPackageRenderer scormPackageRenderer;
    private final ScormPackageAssets scormPackageAssets;

    public ScormPackageComposer(ObjectMapper objectMapper,
            ScormPackageRenderer scormPackageRenderer,
            ScormPackageAssets scormPackageAssets) {
        this.objectMapper = objectMapper;
        this.scormPackageRenderer = scormPackageRenderer;
        this.scormPackageAssets = scormPackageAssets;
    }

    public byte[] compose(Long courseId,
            String courseTitle,
            String packageType,
            JsonNode editorStateSnapshot,
            JsonNode themeSnapshot) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {

            String launchFile = "index.html";
            writeTextEntry(zos, "imsmanifest.xml", buildManifest(courseId, courseTitle, launchFile, packageType));
            writeTextEntry(zos, launchFile, scormPackageRenderer.renderIndex(courseTitle));

            writeTextEntry(zos, "assets/css/base.css", scormPackageAssets.baseCss());
            writeTextEntry(zos, "assets/js/app.js", scormPackageAssets.appJs());
            writeTextEntry(zos, "assets/js/renderer.js", scormPackageAssets.rendererJs());
            writeTextEntry(zos, "assets/js/state.js", scormPackageAssets.stateJs());
            writeTextEntry(zos, "assets/js/player.js", scormPackageAssets.playerJs());
            writeTextEntry(zos, "assets/js/scorm.js", scormPackageAssets.scormJs());
            writeTextEntry(zos, "assets/js/components/mcq-single.js", scormPackageAssets.mcqSingleJs());
            writeTextEntry(zos, "assets/js/components/mcq-multiple.js", scormPackageAssets.mcqMultipleJs());
            writeTextEntry(zos, "assets/js/components/true-false.js", scormPackageAssets.trueFalseJs());
            writeTextEntry(zos, "assets/js/components/short-answer.js", scormPackageAssets.shortAnswerJs());
            writeTextEntry(zos, "assets/js/components/fill-blank.js", scormPackageAssets.fillBlankJs());
            writeTextEntry(zos, "assets/js/components/grouping.js", scormPackageAssets.groupingJs());

            var payload = objectMapper.createObjectNode()
                    .put("exportId", UUID.randomUUID().toString())
                    .put("courseId", courseId == null ? -1 : courseId)
                    .put("courseTitle", courseTitle == null ? "Untitled Course" : courseTitle)
                    .put("scormVersion", "SCORM_12".equalsIgnoreCase(packageType) ? "1.2" : "2004");
            payload.set("editorStateSnapshot",
                    editorStateSnapshot == null ? objectMapper.nullNode() : editorStateSnapshot);
            payload.set("themeSnapshot", themeSnapshot == null ? objectMapper.nullNode() : themeSnapshot);

            writeTextEntry(zos, "data/course.json",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
            writeTextEntry(zos, "data/editor-state.json",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                            editorStateSnapshot == null ? objectMapper.createObjectNode() : editorStateSnapshot));
            writeTextEntry(zos, "data/theme.json",
                    objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(
                                    themeSnapshot == null ? objectMapper.createObjectNode() : themeSnapshot));
            zos.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compose SCORM package", e);
        }
    }

    private void writeTextEntry(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String buildManifest(Long courseId, String title, String launchFile, String packageType) {
        String manifestId = "manifest-" + (courseId == null ? "unknown" : courseId);
        String normalizedTitle = escapeXml(title == null ? "Untitled Course" : title);
        boolean isScorm12 = "SCORM_12".equalsIgnoreCase(packageType);

        if (isScorm12) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<manifest identifier=\"" + manifestId + "\" version=\"1.0\"\n"
                    + "  xmlns=\"http://www.imsproject.org/xsd/imscp_rootv1p1p2\"\n"
                    + "  xmlns:adlcp=\"http://www.adlnet.org/xsd/adlcp_rootv1p2\"\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "  xsi:schemaLocation=\"http://www.imsproject.org/xsd/imscp_rootv1p1p2 imscp_rootv1p1p2.xsd "
                    + "http://www.adlnet.org/xsd/adlcp_rootv1p2 adlcp_rootv1p2.xsd\">\n"
                    + "  <metadata>\n"
                    + "    <schema>ADL SCORM</schema>\n"
                    + "    <schemaversion>1.2</schemaversion>\n"
                    + "  </metadata>\n"
                    + "  <organizations default=\"ORG-1\">\n"
                    + "    <organization identifier=\"ORG-1\">\n"
                    + "      <title>" + normalizedTitle + "</title>\n"
                    + "      <item identifier=\"ITEM-1\" identifierref=\"RES-1\">\n"
                    + "        <title>" + normalizedTitle + "</title>\n"
                    + "      </item>\n"
                    + "    </organization>\n"
                    + "  </organizations>\n"
                    + "  <resources>\n"
                    + "    <resource identifier=\"RES-1\" type=\"webcontent\" adlcp:scormtype=\"sco\" href=\""
                    + launchFile + "\">\n"
                    + "      <file href=\"" + launchFile + "\"/>\n"
                    + "      <file href=\"data/course.json\"/>\n"
                    + "      <file href=\"data/editor-state.json\"/>\n"
                    + "      <file href=\"data/theme.json\"/>\n"
                    + "      <file href=\"assets/css/base.css\"/>\n"
                    + "      <file href=\"assets/js/app.js\"/>\n"
                    + "    </resource>\n"
                    + "  </resources>\n"
                    + "</manifest>\n";
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<manifest identifier=\"" + manifestId + "\" version=\"1.0\"\n"
                + "  xmlns=\"http://www.imsglobal.org/xsd/imscp_v1p1\"\n"
                + "  xmlns:adlcp=\"http://www.adlnet.org/xsd/adlcp_v1p3\"\n"
                + "  xmlns:adlseq=\"http://www.adlnet.org/xsd/adlseq_v1p3\"\n"
                + "  xmlns:imsss=\"http://www.imsglobal.org/xsd/imsss\"\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "  xsi:schemaLocation=\"http://www.imsglobal.org/xsd/imscp_v1p1 imscp_v1p1.xsd "
                + "http://www.adlnet.org/xsd/adlcp_v1p3 adlcp_v1p3.xsd "
                + "http://www.adlnet.org/xsd/adlseq_v1p3 adlseq_v1p3.xsd "
                + "http://www.imsglobal.org/xsd/imsss imsss_v1p0.xsd\">\n"
                + "  <metadata>\n"
                + "    <schema>ADL SCORM</schema>\n"
                + "    <schemaversion>2004 4th Edition</schemaversion>\n"
                + "  </metadata>\n"
                + "  <organizations default=\"ORG-1\">\n"
                + "    <organization identifier=\"ORG-1\">\n"
                + "      <title>" + normalizedTitle + "</title>\n"
                + "      <item identifier=\"ITEM-1\" identifierref=\"RES-1\">\n"
                + "        <title>" + normalizedTitle + "</title>\n"
                + "        <imsss:sequencing>\n"
                + "          <imsss:controlMode flow=\"true\" choice=\"true\"/>\n"
                + "        </imsss:sequencing>\n"
                + "      </item>\n"
                + "    </organization>\n"
                + "  </organizations>\n"
                + "  <resources>\n"
                + "    <resource identifier=\"RES-1\" type=\"webcontent\" adlcp:scormType=\"sco\" href=\"" + launchFile
                + "\">\n"
                + "      <file href=\"" + launchFile + "\"/>\n"
                + "      <file href=\"data/course.json\"/>\n"
                + "      <file href=\"data/editor-state.json\"/>\n"
                + "      <file href=\"data/theme.json\"/>\n"
                + "      <file href=\"assets/css/base.css\"/>\n"
                + "      <file href=\"assets/js/app.js\"/>\n"
                + "    </resource>\n"
                + "  </resources>\n"
                + "</manifest>\n";
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
