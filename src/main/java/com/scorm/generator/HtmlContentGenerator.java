package com.scorm.generator;

import com.scorm.generator.entity.ScormPackage;
import com.scorm.generator.entity.Question;
import com.scorm.generator.entity.Answer;

/**
 * Generates HTML content for SCORM quiz
 */
@org.springframework.stereotype.Service
public class HtmlContentGenerator {

    public String generateIndexHtml(ScormPackage scormPackage) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>").append(escapeHtml(scormPackage.getTitle())).append("</title>\n");
        html.append("    <link rel=\"stylesheet\" href=\"style.css\">\n");
        html.append("    <script src=\"scorm_api.js\"></script>\n");
        html.append("    <script src=\"quiz.js\"></script>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");
        html.append("        <header>\n");
        html.append("            <h1>").append(escapeHtml(scormPackage.getTitle())).append("</h1>\n");
        if (scormPackage.getDescription() != null && !scormPackage.getDescription().trim().isEmpty()) {
            html.append("            <p class=\"description\">").append(escapeHtml(scormPackage.getDescription()))
                    .append("</p>\n");
        }
        html.append("        </header>\n");
        html.append("        \n");
        html.append("        <div id=\"quiz-container\">\n");
        html.append("            <div id=\"start-screen\" class=\"screen active\">\n");
        html.append("                <h2>Bắt đầu bài kiểm tra</h2>\n");
        html.append("                <p>Số câu hỏi: <strong>").append(scormPackage.getQuestions().size())
                .append("</strong></p>\n");
        html.append("                <p>Điểm đạt: <strong>").append(scormPackage.getPassingScore())
                .append("%</strong></p>\n");
        html.append("                <p>Số lần thử tối đa: <strong>").append(scormPackage.getMaxAttempts())
                .append("</strong></p>\n");
        html.append("                <button id=\"start-btn\" class=\"btn btn-primary\">Bắt đầu</button>\n");
        html.append("            </div>\n");
        html.append("            \n");
        html.append("            <div id=\"quiz-screen\" class=\"screen\">\n");
        html.append("                <div class=\"progress-bar\">\n");
        html.append("                    <div id=\"progress\" class=\"progress-fill\"></div>\n");
        html.append("                </div>\n");
        html.append("                <div id=\"question-counter\"></div>\n");
        html.append("                <div id=\"question-container\"></div>\n");
        html.append("                <div class=\"navigation\">\n");
        html.append("                    <button id=\"prev-btn\" class=\"btn btn-secondary\">Trước</button>\n");
        html.append("                    <button id=\"next-btn\" class=\"btn btn-primary\">Tiếp</button>\n");
        html.append(
                "                    <button id=\"submit-btn\" class=\"btn btn-success\" style=\"display:none;\">Nộp bài</button>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("            \n");
        html.append("            <div id=\"result-screen\" class=\"screen\">\n");
        html.append("                <h2>Kết quả</h2>\n");
        html.append("                <div id=\"score-display\"></div>\n");
        html.append("                <div id=\"result-message\"></div>\n");
        html.append("                <button id=\"restart-btn\" class=\"btn btn-primary\">Làm lại</button>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("    \n");
        html.append("    <script>\n");
        html.append("        // Quiz data\n");
        html.append("        const quizData = ").append(generateQuizDataJson(scormPackage)).append(";\n");
        html.append("        \n");
        html.append("        // Initialize quiz when page loads\n");
        html.append("        document.addEventListener('DOMContentLoaded', function() {\n");
        html.append("            initializeQuiz(quizData);\n");
        html.append("        });\n");
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    private String generateQuizDataJson(ScormPackage scormPackage) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("    \"id\": \"").append(scormPackage.getId()).append("\",\n");
        json.append("    \"title\": \"").append(escapeJson(scormPackage.getTitle())).append("\",\n");
        json.append("    \"description\": \"")
                .append(escapeJson(scormPackage.getDescription() != null ? scormPackage.getDescription() : ""))
                .append("\",\n");
        json.append("    \"passingScore\": ").append(scormPackage.getPassingScore()).append(",\n");
        json.append("    \"maxAttempts\": ").append(scormPackage.getMaxAttempts()).append(",\n");
        json.append("    \"questions\": [\n");

        if (scormPackage.getQuestions() != null) {
            for (int i = 0; i < scormPackage.getQuestions().size(); i++) {
                Question question = scormPackage.getQuestions().get(i);
                int correctAnswerIndex = -1;
                if (question.getAnswers() != null) {
                    for (int j = 0; j < question.getAnswers().size(); j++) {
                        if (question.getAnswers().get(j).getCorrect()) {
                            correctAnswerIndex = j;
                            break;
                        }
                    }
                }

                json.append("        {\n");
                json.append("            \"id\": \"").append(question.getId()).append("\",\n");
                json.append("            \"text\": \"").append(escapeJson(question.getText())).append("\",\n");
                json.append("            \"correctAnswerIndex\": ").append(correctAnswerIndex).append(",\n");
                json.append("            \"explanation\": \"\",\n"); // Explanation not supported in new model
                json.append("            \"answers\": [\n");

                if (question.getAnswers() != null) {
                    for (int j = 0; j < question.getAnswers().size(); j++) {
                        Answer answer = question.getAnswers().get(j);
                        json.append("                {\n");
                        json.append("                    \"id\": \"").append(answer.getId()).append("\",\n");
                        json.append("                    \"text\": \"").append(escapeJson(answer.getText()))
                                .append("\",\n");
                        json.append("                    \"correct\": ").append(answer.getCorrect()).append("\n");
                        json.append("                }");
                        if (j < question.getAnswers().size() - 1) {
                            json.append(",");
                        }
                        json.append("\n");
                    }
                }

                json.append("            ]\n");
                json.append("        }");
                if (i < scormPackage.getQuestions().size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
        }

        json.append("    ]\n");
        json.append("}");

        return json.toString();
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
