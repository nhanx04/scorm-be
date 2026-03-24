package com.scorm.generator.ScormPackaging;

import org.springframework.stereotype.Component;

@Component
public class ScormPackageRenderer {

    public String renderIndex(String courseTitle) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width,initial-scale=1"/>
                  <title>%s</title>
                  <link rel="stylesheet" href="assets/css/base.css"/>
                </head>
                <body>
                  <div id="app"></div>
                  <script type="module" src="assets/js/app.js"></script>
                </body>
                </html>
                """.formatted(escapeHtml(courseTitle == null ? "SCORM Course" : courseTitle));
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
