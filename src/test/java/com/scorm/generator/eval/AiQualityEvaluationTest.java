package com.scorm.generator.eval;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;

import com.google.genai.Client;
import com.scorm.generator.dto.ai.AiCourseOutline;
import com.scorm.generator.dto.ai.AiPageContentResponse;
import com.scorm.generator.dto.ai.AiQuizResponse;
import com.scorm.generator.service.QuizPrompts;

/**
 * Quantitative AI evaluation harness for thesis section 5.4.
 *
 * <p>Drives the three production AI features (outline / page-content / quiz)
 * against the {@code ai-eval} test set and records per-call:
 *
 * <ul>
 *   <li><b>Group A (Reliability):</b> JSON parse success, schema validity,
 *       markdown-fence usage, empty fields</li>
 *   <li><b>Group B (Latency &amp; Cost):</b> wall-clock latency, token usage,
 *       USD cost</li>
 * </ul>
 *
 * <p>Gated by {@code -Dai-eval.enabled=true} because it spends real API
 * credit (~$8–10 for a full run). Two entry points:
 *
 * <ul>
 *   <li>{@link #smokeTest()} — single call on the smallest doc, ~$0.05</li>
 *   <li>{@link #fullEvaluation()} — 10 docs × 3 features × 2 runs = 60 calls</li>
 * </ul>
 *
 * <p>The harness intentionally builds its own {@link ChatClient} rather than
 * pulling the Spring Boot context, so it can pin temperature to 0.2 (vs the
 * production default of 0.7) without disturbing the running app.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "ai-eval.enabled", matches = "true")
class AiQualityEvaluationTest {

    private static final Path RESULTS_DIR = Paths.get("src/test/resources/ai-eval/results");
    // Outputs subdir can be overridden so post-fix re-runs don't overwrite the
    // baseline JSONs that the IWF scoring and LLM-judge results were built on.
    private static final Path OUTPUTS_DIR = RESULTS_DIR.resolve(
            System.getProperty("ai-eval.outputs-subdir", "outputs"));
    private static final Path DOCS_DIR = Paths.get("src/test/resources/ai-eval/documents");

    private EvalConfig config;
    private ChatClient chatClient;
    private double cumulativeCostUsd = 0.0;
    // Tika extraction takes 5-15s on long PDFs; cache by doc_id so re-running
    // multiple passes (smoke / save / postfix / full) doesn't re-parse.
    private final java.util.Map<String, String> pdfTextCache = new java.util.HashMap<>();
    private final ObjectMapper jsonMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private boolean saveOutputs = false;

    @BeforeAll
    void setup() throws Exception {
        config = EvalConfigLoader.load();

        String apiKey = System.getenv("GOOGLE_GENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_GENAI_API_KEY env var is required for ai-eval tests.");
        }

        Client genai = Client.builder().apiKey(apiKey).build();
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(config.model().name())
                .temperature(config.model().temperature())
                .topP(config.model().topP())
                .maxOutputTokens(config.model().maxOutputTokens())
                .build();
        GoogleGenAiChatModel model = GoogleGenAiChatModel.builder()
                .genAiClient(genai)
                .defaultOptions(options)
                .build();
        chatClient = ChatClient.builder(model)
                .defaultSystem("Bạn là một chuyên gia thiết kế giáo trình e-learning (Instructional Designer) "
                        + "với 10 năm kinh nghiệm. Nhiệm vụ của bạn là xây dựng cấu trúc khóa học chi tiết, "
                        + "logic và hấp dẫn.")
                .build();

        System.out.println("[ai-eval] Loaded config: " + config.experimentId()
                + ", model=" + config.model().name()
                + ", temperature=" + config.model().temperature()
                + ", docs=" + config.documents().size()
                + ", runs=" + config.runsPerDocPerFeature());
    }

    @Test
    @DisplayName("Smoke test - run outline on smallest doc once to verify pipeline")
    void smokeTest() throws Exception {
        String timestamp = nowStamp();
        try (CsvReporter reliability = openReliabilityCsv("smoke_reliability_" + timestamp);
             CsvReporter latencyCost = openLatencyCostCsv("smoke_latency_cost_" + timestamp)) {

            // Pick the doc with the fewest pages (D09 - 25 pages)
            EvalConfig.DocumentCfg doc = config.documents().stream()
                    .min((a, b) -> Integer.compare(a.actualPages(), b.actualPages()))
                    .orElseThrow();

            System.out.println("[ai-eval] smoke: " + doc.id() + " " + doc.file() + " ("
                    + doc.actualPages() + " pages)");
            String text = readPdfText(doc);
            System.out.println("[ai-eval] smoke: extracted " + text.length() + " chars from PDF");

            runOutline(doc, text, 1, reliability, latencyCost);
            System.out.println("[ai-eval] smoke OK. Cumulative cost = $"
                    + String.format("%.4f", cumulativeCostUsd));
        }
    }

    @Test
    @DisplayName("Quiz-only post-fix verification - 10 docs × quiz × 1 run, persist parsed JSON")
    void quizOnlyPostFixPass() throws Exception {
        runSavePass("postfix_quiz", List.of(this::runQuiz));
    }

    @Test
    @DisplayName("Save-outputs pass - 10 docs × 3 features × 1 run, persist parsed JSON")
    void saveOutputsPass() throws Exception {
        runSavePass("save", List.of(this::runOutline, this::runPageContent, this::runQuiz));
    }

    /**
     * Drive one save-pass over the test set: iterate docs, run each requested
     * feature once per doc with run=1, persist parsed JSON, write CSVs whose
     * names start with {@code stemPrefix}, and stop early when the cost budget
     * is exhausted.
     */
    private void runSavePass(String stemPrefix, List<FeatureRunner> features) throws Exception {
        saveOutputs = true;
        String timestamp = nowStamp();
        try (CsvReporter reliability = openReliabilityCsv(stemPrefix + "_reliability_" + timestamp);
             CsvReporter latencyCost = openLatencyCostCsv(stemPrefix + "_latency_cost_" + timestamp)) {

            int totalCalls = config.documents().size() * features.size();
            System.out.println("[ai-eval] " + stemPrefix + " pass: "
                    + totalCalls + " calls planned");
            System.out.println("[ai-eval] outputs will be written to " + OUTPUTS_DIR);

            int callIdx = 0;
            for (EvalConfig.DocumentCfg doc : config.documents()) {
                System.out.println("\n[ai-eval] ----- " + doc.id() + " " + doc.file()
                        + " (" + doc.actualPages() + " pages, " + doc.language() + ") -----");
                String text;
                try {
                    text = readPdfText(doc);
                } catch (Exception e) {
                    System.out.println("[ai-eval] FAILED to read PDF: " + e.getMessage());
                    continue;
                }
                for (FeatureRunner feature : features) {
                    callIdx = trackCall(callIdx, totalCalls,
                            () -> feature.run(doc, text, 1, reliability, latencyCost));
                    if (overBudget()) {
                        return;
                    }
                }
            }

            System.out.println("\n[ai-eval] " + stemPrefix + " DONE. " + callIdx
                    + " calls, cost=$" + String.format("%.4f", cumulativeCostUsd));
        }
    }

    @FunctionalInterface
    private interface FeatureRunner {
        void run(EvalConfig.DocumentCfg doc, String text, int run,
                 CsvReporter reliability, CsvReporter latencyCost);
    }

    @Test
    @DisplayName("Full evaluation - 10 docs × 3 features × N runs")
    void fullEvaluation() throws Exception {
        String timestamp = nowStamp();
        try (CsvReporter reliability = openReliabilityCsv("reliability_" + timestamp);
             CsvReporter latencyCost = openLatencyCostCsv("latency_cost_" + timestamp)) {

            int totalCalls = config.documents().size() * 3 * config.runsPerDocPerFeature();
            System.out.println("[ai-eval] full: " + totalCalls + " calls planned, budget=$"
                    + config.cost().totalBudgetUsd());

            int callIdx = 0;
            for (EvalConfig.DocumentCfg doc : config.documents()) {
                System.out.println("\n[ai-eval] ----- " + doc.id() + " " + doc.file()
                        + " (" + doc.actualPages() + " pages, " + doc.language() + ") -----");
                String text;
                try {
                    text = readPdfText(doc);
                } catch (Exception e) {
                    System.out.println("[ai-eval] FAILED to read PDF: " + e.getMessage());
                    continue;
                }
                System.out.println("[ai-eval] extracted " + text.length() + " chars");

                for (int runIdx = 1; runIdx <= config.runsPerDocPerFeature(); runIdx++) {
                    final int run = runIdx;
                    callIdx = trackCall(callIdx, totalCalls,
                            () -> runOutline(doc, text, run, reliability, latencyCost));
                    if (overBudget()) {
                        return;
                    }
                    callIdx = trackCall(callIdx, totalCalls,
                            () -> runPageContent(doc, text, run, reliability, latencyCost));
                    if (overBudget()) {
                        return;
                    }
                    callIdx = trackCall(callIdx, totalCalls,
                            () -> runQuiz(doc, text, run, reliability, latencyCost));
                    if (overBudget()) {
                        return;
                    }
                }
            }

            System.out.println("\n[ai-eval] DONE. " + callIdx + " calls, cost=$"
                    + String.format("%.4f", cumulativeCostUsd));
        }
    }

    // ---------------------------------------------------------------------
    // Feature implementations - each replicates the production prompt and
    // captures metrics into the CSV reporters.
    // ---------------------------------------------------------------------

    private void runOutline(EvalConfig.DocumentCfg doc, String docContext, int run,
                            CsvReporter reliability, CsvReporter latencyCost) {
        String userPrompt = """
                Hãy đọc, phân tích toàn bộ tài liệu chuyên môn dưới đây và biến nó thành một dàn ý khóa học e-learning chi tiết, có cấu trúc tốt.

                NỘI DUNG TÀI LIỆU GỐC (Dùng làm nguồn kiến thức chính):
                =================================
                {documentContext}
                =================================

                THÔNG TIN CẤU HÌNH KHÓA HỌC:
                - Tên khóa học mong muốn (Title): {courseTitle}
                - Đối tượng học viên (Target Audience): Sinh viên đại học năm 2-3
                - Trình độ hiện tại (Proficiency Level): Trung cấp
                - Thời lượng dự kiến (Duration): 8 tuần
                - Ngôn ngữ đầu ra (Language): {language}
                - Yêu cầu thêm: Bám sát tài liệu gốc

                YÊU CẦU CHUYÊN MÔN:
                1. Dựa trên nội dung tài liệu gốc, hãy tự động phân bổ số lượng chương (sections) và các bài học (topics) sao cho bao quát hết kiến thức, logic và khoa học.
                2. Các ý chính, khái niệm quan trọng trong tài liệu phải được tách thành các bài học riêng biệt.
                3. Đảm bảo luồng kiến thức đi từ cơ bản đến nâng cao.

                YÊU CẦU ĐỊNH DẠNG:
                1. Trả về kết quả CHÍNH XÁC theo định dạng JSON được yêu cầu.
                2. KHÔNG thêm bất kỳ lời dẫn hay giải thích nào bên ngoài JSON.

                {formatInstructions}
                """;

        BeanOutputConverter<AiCourseOutline.Draft> converter = new BeanOutputConverter<>(AiCourseOutline.Draft.class);

        executeCall(doc, "outline", run, reliability, latencyCost,
                () -> chatClient.prompt()
                        .user(u -> u.text(userPrompt)
                                .param("documentContext", docContext)
                                .param("courseTitle", doc.title())
                                .param("language", languageFullName(doc.language()))
                                .param("formatInstructions", converter.getFormat()))
                        .call()
                        .chatResponse(),
                (cleaned) -> {
                    AiCourseOutline.Draft outline = converter.convert(cleaned);
                    boolean schemaOk = outline != null
                            && outline.title() != null
                            && outline.sections() != null
                            && !outline.sections().isEmpty();
                    boolean emptyField = !schemaOk
                            || outline.title().isBlank()
                            || outline.sections().stream().anyMatch(s -> s.title() == null
                                    || s.title().isBlank()
                                    || s.topics() == null
                                    || s.topics().isEmpty());
                    return ParseOutcome.of(true, schemaOk, emptyField, outline);
                });
    }

    private void runPageContent(EvalConfig.DocumentCfg doc, String docContext, int run,
                                CsvReporter reliability, CsvReporter latencyCost) {
        // Use truncated doc as "section context" - mirrors how the production
        // page-content endpoint receives partial context, not the whole textbook.
        String trimmed = truncate(docContext, 6000);

        String userPrompt = """
                Bạn là một chuyên gia thiết kế nội dung e-learning (Instructional Designer).
                Hãy viết nội dung giảng dạy chi tiết cho một bài học (Page) dựa trên ngữ cảnh sau:
                - Tên toàn bộ khóa học: {courseTopic}
                - Thuộc chương (Section): {sectionTitle}
                - Chủ đề bài học này (Topic): {pageTopic}
                - Ngôn ngữ: {language}
                - Ngữ cảnh tham khảo (trích từ tài liệu gốc):
                ---
                {context}
                ---

                YÊU CẦU VỀ NỘI DUNG (Trường htmlContent):
                1. Trình bày bài học một cách sư phạm: Có đoạn mở đầu dẫn dắt, giải thích chi tiết các khái niệm, đưa ra ví dụ minh họa và tóm tắt ngắn ở cuối bài.
                2. Giá trị của trường `htmlContent` PHẢI là chuỗi mã HTML hợp lệ để hiển thị trực tiếp trên trình duyệt.
                3. CHỈ SỬ DỤNG các thẻ HTML cơ bản để định dạng: <h2>, <h3>, <p>, <ul>, <ol>, <li>, <strong>, <em>, blockquote.
                4. TUYỆT ĐỐI KHÔNG sử dụng các thẻ <html>, <head>, <body>, <script>, <style>.

                YÊU CẦU VỀ ĐỊNH DẠNG ĐẦU RA:
                1. Trả về kết quả CHÍNH XÁC theo định dạng JSON được yêu cầu.
                2. KHÔNG thêm bất kỳ lời dẫn hay giải thích nào bên ngoài block JSON.

                {formatInstructions}
                """;

        BeanOutputConverter<AiPageContentResponse> converter =
                new BeanOutputConverter<>(AiPageContentResponse.class);

        executeCall(doc, "page-content", run, reliability, latencyCost,
                () -> chatClient.prompt()
                        .user(u -> u.text(userPrompt)
                                .param("courseTopic", doc.title())
                                .param("sectionTitle", "Chương mở đầu")
                                .param("pageTopic", deriveFirstTopic(doc))
                                .param("language", languageFullName(doc.language()))
                                .param("context", trimmed)
                                .param("formatInstructions", converter.getFormat()))
                        .call()
                        .chatResponse(),
                (cleaned) -> {
                    AiPageContentResponse content = converter.convert(cleaned);
                    boolean schemaOk = content != null
                            && content.htmlContent() != null
                            && content.pageTitle() != null;
                    boolean emptyField = !schemaOk
                            || content.htmlContent().isBlank()
                            || content.pageTitle().isBlank()
                            || content.shortSummary() == null
                            || content.shortSummary().isBlank();
                    return ParseOutcome.of(true, schemaOk, emptyField, content);
                });
    }

    private void runQuiz(EvalConfig.DocumentCfg doc, String docContext, int run,
                         CsvReporter reliability, CsvReporter latencyCost) {
        String trimmed = truncate(docContext, 6000);
        BeanOutputConverter<AiQuizResponse> converter = new BeanOutputConverter<>(AiQuizResponse.class);

        // Reuse the production template so the eval can't drift from prod.
        executeCall(doc, "quiz", run, reliability, latencyCost,
                () -> chatClient.prompt()
                        .user(u -> u.text(QuizPrompts.FROM_TEXT)
                                .param("sourceText", trimmed)
                                .param("numberOfQuestions", 5)
                                .param("difficulty", "Trung bình")
                                .param("language", languageFullName(doc.language()))
                                .param("fewShotExamples", "")
                                .param("formatInstructions", converter.getFormat()))
                        .call()
                        .chatResponse(),
                (cleaned) -> {
                    AiQuizResponse quiz = converter.convert(cleaned);
                    boolean schemaOk = quiz != null
                            && quiz.questions() != null
                            && !quiz.questions().isEmpty();
                    boolean emptyField = !schemaOk
                            || quiz.questions().stream().anyMatch(q -> q.prompt() == null
                                    || q.prompt().isBlank()
                                    || q.type() == null
                                    || q.type().isBlank());
                    return ParseOutcome.of(true, schemaOk, emptyField, quiz);
                });
    }

    // ---------------------------------------------------------------------
    // Generic call execution: time it, capture tokens, write both CSVs.
    // ---------------------------------------------------------------------

    private void executeCall(EvalConfig.DocumentCfg doc, String feature, int run,
                             CsvReporter reliability, CsvReporter latencyCost,
                             ResponseSupplier supplier, ParseStrategy parseStrategy) {
        long start = System.nanoTime();
        String timestamp = LocalDateTime.now().toString();
        ChatResponse response = null;
        String raw = null;
        String cleaned = "";
        boolean usedFence = false;
        ParseOutcome outcome = ParseOutcome.of(false, false, false, null);
        String errorMessage = null;
        String errorType = null;

        try {
            response = supplier.get();
            raw = response.getResult().getOutput().getText();
            usedFence = raw != null && raw.contains("```");
            cleaned = stripJsonFence(raw == null ? "" : raw);
            try {
                outcome = parseStrategy.parse(cleaned);
            } catch (Exception parseEx) {
                errorMessage = parseEx.getMessage();
                errorType = parseEx.getClass().getSimpleName();
                outcome = ParseOutcome.failure(parseEx);
            }
        } catch (Exception apiEx) {
            errorMessage = apiEx.getMessage();
            errorType = apiEx.getClass().getSimpleName();
        }

        long latencyMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        if (response != null && response.getMetadata() != null) {
            Usage usage = response.getMetadata().getUsage();
            if (usage != null) {
                promptTokens = nz(usage.getPromptTokens());
                completionTokens = nz(usage.getCompletionTokens());
                totalTokens = nz(usage.getTotalTokens());
            }
        }
        double costUsd = config.cost().computeUsd(promptTokens, completionTokens);
        cumulativeCostUsd += costUsd;

        if (saveOutputs) {
            saveOutputJson(doc, feature, run, timestamp, raw, cleaned, outcome,
                    errorType, errorMessage, promptTokens, completionTokens, latencyMs);
        }

        try {
            Map<String, Object> r = CsvReporter.row();
            r.put("timestamp", timestamp);
            r.put("doc_id", doc.id());
            r.put("doc_file", doc.file());
            r.put("doc_pages", doc.actualPages());
            r.put("doc_language", doc.language());
            r.put("feature", feature);
            r.put("run", run);
            r.put("api_ok", response != null);
            r.put("json_parse_ok", outcome.parseOk());
            r.put("schema_ok", outcome.schemaOk());
            r.put("empty_field", outcome.emptyField());
            r.put("used_markdown_fence", usedFence);
            r.put("raw_length", raw == null ? 0 : raw.length());
            r.put("error_type", errorType == null ? "" : errorType);
            r.put("error_message", errorMessage == null ? "" : truncate(errorMessage, 200));
            reliability.writeRow(r);

            Map<String, Object> l = CsvReporter.row();
            l.put("timestamp", timestamp);
            l.put("doc_id", doc.id());
            l.put("doc_pages", doc.actualPages());
            l.put("feature", feature);
            l.put("run", run);
            l.put("latency_ms", latencyMs);
            l.put("prompt_tokens", promptTokens);
            l.put("completion_tokens", completionTokens);
            l.put("total_tokens", totalTokens);
            l.put("cost_usd", String.format("%.6f", costUsd));
            l.put("cumulative_cost_usd", String.format("%.6f", cumulativeCostUsd));
            latencyCost.writeRow(l);
        } catch (Exception ioEx) {
            System.err.println("[ai-eval] CSV write failed: " + ioEx.getMessage());
        }

        System.out.println(String.format(
                "[ai-eval] %s/%s/run%d  latency=%dms  in=%d out=%d  $%.4f  parse=%s schema=%s%s",
                doc.id(), feature, run, latencyMs, promptTokens, completionTokens, costUsd,
                outcome.parseOk(), outcome.schemaOk(),
                errorType == null ? "" : "  ERR=" + errorType));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Write a per-call JSON file with the full raw response, the cleaned JSON,
     * and the parsed object — used downstream by LlmJudge (D3) and IWF scoring
     * (D4) which both need the actual generated content, not just metrics.
     */
    private void saveOutputJson(EvalConfig.DocumentCfg doc, String feature, int run,
                                 String timestamp, String raw, String cleaned,
                                 ParseOutcome outcome, String errorType, String errorMessage,
                                 int promptTokens, int completionTokens, long latencyMs) {
        Map<String, Object> payload = new TreeMap<>();
        payload.put("doc_id", doc.id());
        payload.put("doc_file", doc.file());
        payload.put("doc_pages", doc.actualPages());
        payload.put("doc_language", doc.language());
        payload.put("doc_title", doc.title());
        payload.put("feature", feature);
        payload.put("run", run);
        payload.put("timestamp", timestamp);
        payload.put("model", config.model().name());
        payload.put("temperature", config.model().temperature());
        payload.put("prompt_tokens", promptTokens);
        payload.put("completion_tokens", completionTokens);
        payload.put("latency_ms", latencyMs);
        payload.put("json_parse_ok", outcome.parseOk());
        payload.put("schema_ok", outcome.schemaOk());
        payload.put("empty_field", outcome.emptyField());
        payload.put("error_type", errorType);
        payload.put("error_message", errorMessage);
        payload.put("raw", raw);
        payload.put("cleaned", cleaned);
        payload.put("parsed", outcome.parsed());

        try {
            Files.createDirectories(OUTPUTS_DIR);
            String filename = String.format("%s_%s_run%d.json", doc.id(), feature, run);
            Path target = OUTPUTS_DIR.resolve(filename);
            jsonMapper.writeValue(target.toFile(), payload);
        } catch (Exception e) {
            System.err.println("[ai-eval] save-output FAILED for "
                    + doc.id() + "/" + feature + "/" + run + ": " + e.getMessage());
        }
    }

    private CsvReporter openReliabilityCsv(String stem) throws Exception {
        return new CsvReporter(RESULTS_DIR.resolve(stem + ".csv"),
                "timestamp", "doc_id", "doc_file", "doc_pages", "doc_language",
                "feature", "run", "api_ok", "json_parse_ok", "schema_ok",
                "empty_field", "used_markdown_fence", "raw_length",
                "error_type", "error_message");
    }

    private CsvReporter openLatencyCostCsv(String stem) throws Exception {
        return new CsvReporter(RESULTS_DIR.resolve(stem + ".csv"),
                "timestamp", "doc_id", "doc_pages", "feature", "run",
                "latency_ms", "prompt_tokens", "completion_tokens", "total_tokens",
                "cost_usd", "cumulative_cost_usd");
    }

    private String readPdfText(EvalConfig.DocumentCfg doc) throws Exception {
        String cached = pdfTextCache.get(doc.id());
        if (cached != null) {
            return cached;
        }
        Path file = DOCS_DIR.resolve(doc.file());
        if (!Files.exists(file)) {
            throw new IllegalStateException("Missing test document: " + file);
        }
        TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(file.toFile()));
        List<Document> docs = reader.get();
        StringBuilder sb = new StringBuilder();
        for (Document d : docs) {
            sb.append(d.getText()).append('\n');
        }
        String text = sb.toString();
        pdfTextCache.put(doc.id(), text);
        return text;
    }

    private static String stripJsonFence(String raw) {
        return raw.replace("```json", "").replace("```", "").trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String languageFullName(String code) {
        return switch (code) {
            case "vi" -> "Vietnamese";
            case "en" -> "English";
            default -> "Vietnamese";
        };
    }

    private static String deriveFirstTopic(EvalConfig.DocumentCfg doc) {
        String title = doc.title();
        if (title == null) {
            return "Mở đầu";
        }
        // Take first 8 words to keep prompt context bounded.
        String[] words = title.split("\\s+");
        if (words.length <= 8) {
            return title;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(words[i]);
        }
        return sb.toString();
    }

    private static int nz(Integer i) {
        return i == null ? 0 : i;
    }

    private String nowStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private boolean overBudget() {
        if (cumulativeCostUsd > config.cost().totalBudgetUsd()) {
            System.out.println("[ai-eval] STOP - over budget at $"
                    + String.format("%.4f", cumulativeCostUsd));
            return true;
        }
        return false;
    }

    private int trackCall(int idx, int total, Runnable r) {
        int next = idx + 1;
        System.out.print("[ai-eval] (" + next + "/" + total + ") ");
        r.run();
        return next;
    }

    @FunctionalInterface
    private interface ResponseSupplier {
        ChatResponse get() throws Exception;
    }

    @FunctionalInterface
    private interface ParseStrategy {
        ParseOutcome parse(String cleaned) throws Exception;
    }

    private record ParseOutcome(boolean parseOk, boolean schemaOk, boolean emptyField,
                                Exception error, Object parsed) {

        static ParseOutcome of(boolean parseOk, boolean schemaOk, boolean emptyField, Object parsed) {
            return new ParseOutcome(parseOk, schemaOk, emptyField, null, parsed);
        }

        static ParseOutcome failure(Exception e) {
            return new ParseOutcome(false, false, false, e, null);
        }
    }
}
