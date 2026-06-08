package com.scorm.generator.eval;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.scorm.generator.dto.ai.AiPageContentResponse;
import com.scorm.generator.dto.ai.AiQuizResponse;
import com.scorm.generator.service.QuizPrompts;
import com.scorm.generator.service.rag.DocumentChunker;
import com.scorm.generator.service.rag.GeminiEmbeddingClient;

/**
 * Đánh giá định lượng <b>RAG vs full-document stuffing</b> cho chức năng sinh nội
 * dung bài học (mục 5.4 — bổ sung sau khi tích hợp kỹ thuật RAG).
 *
 * <p>Thiết kế thí nghiệm (so sánh trong cặp — paired): với MỖI tài liệu test,
 * sinh nội dung cho cùng một chủ đề bằng HAI chiến lược grounding, chỉ khác nhau
 * ở phần ngữ cảnh đưa vào prompt:
 *
 * <ul>
 *   <li><b>STUFF (baseline):</b> nhồi toàn bộ tài liệu (cắt ở
 *       {@code -Dragcmp.baseline-max-chars}, mặc định 50k ký tự để tránh vượt
 *       context window và giới hạn chi phí) — đúng hành vi cũ trước khi có RAG.</li>
 *   <li><b>RAG:</b> cắt tài liệu bằng {@link DocumentChunker} (production), nhúng
 *       bằng {@link GeminiEmbeddingClient} (production), truy xuất top-k chunk
 *       liên quan nhất tới chủ đề bằng cosine similarity (in-memory, tương đương
 *       truy vấn pgvector {@code <=>}).</li>
 * </ul>
 *
 * <p>Mỗi lần sinh ghi nhận: số ký tự ngữ cảnh, prompt/completion tokens, latency,
 * chi phí USD (hiệu quả) và điểm faithfulness 1–5 + cờ hallucination do LLM-judge
 * (Gemini Flash) chấm dựa trên key facts trong {@code ground_truth.json} (chất
 * lượng bám nguồn). Tất cả vào một CSV để phân tích theo {@code strategy}.
 *
 * <p>Gated bởi {@code -Dai-eval.enabled=true} vì tốn API credit thật.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "ai-eval.enabled", matches = "true")
class RagComparisonEvaluationTest {

    private static final Path RESULTS_DIR = Paths.get("src/test/resources/ai-eval/results");
    private static final Path DOCS_DIR = Paths.get("src/test/resources/ai-eval/documents");
    private static final Path GROUND_TRUTH_PATH = Paths.get(
            "src/test/resources/ai-eval/ground-truth/ground_truth.json");

    // Knobs (override qua -D). Mặc định khớp production: chunk 1200/overlap 200, top-k 6.
    private static final int BASELINE_MAX_CHARS =
            Integer.getInteger("ragcmp.baseline-max-chars", 50_000);
    private static final int CHUNK_SIZE = Integer.getInteger("ragcmp.chunk-size", 1_200);
    private static final int CHUNK_OVERLAP = Integer.getInteger("ragcmp.chunk-overlap", 200);
    private static final int TOP_K = Integer.getInteger("ragcmp.top-k", 6);
    private static final String EMBED_MODEL =
            System.getProperty("ragcmp.embed-model", "gemini-embedding-001");
    private static final int EMBED_DIMS = Integer.getInteger("ragcmp.embed-dims", 768);
    // Số lần sinh lặp lại mỗi (tài liệu × chiến lược) để giảm nhiễu LLM trước khi
    // lấy trung bình per-doc cho kiểm định Wilcoxon.
    private static final int RUNS = Integer.getInteger("ragcmp.runs", 1);
    // Lưu nội dung sinh ra (text) để human/cross-check faithfulness sau này.
    private static final boolean SAVE_OUTPUTS = Boolean.getBoolean("ragcmp.save-outputs");
    private static final Path OUTPUTS_DIR = RESULTS_DIR.resolve("rag_outputs");

    /** Prompt page-content (giống production); chỉ {context} đổi giữa STUFF/RAG. */
    private static final String PAGE_CONTENT_PROMPT = """
            Bạn là một chuyên gia thiết kế nội dung e-learning (Instructional Designer).
            Hãy viết nội dung giảng dạy chi tiết cho một bài học (Page) dựa trên ngữ cảnh sau:
            - Tên toàn bộ khóa học: {courseTopic}
            - Chủ đề bài học này (Topic): {pageTopic}
            - Ngôn ngữ: {language}

            TÀI LIỆU GỐC (nguồn dữ kiện chính — bám sát, KHÔNG bịa ngoài tài liệu):
            =================================
            {context}
            =================================

            YÊU CẦU VỀ NỘI DUNG (Trường htmlContent):
            1. Trình bày sư phạm: mở đầu dẫn dắt, giải thích khái niệm, ví dụ minh họa, tóm tắt cuối bài.
            2. `htmlContent` PHẢI là HTML hợp lệ; CHỈ dùng <h2>,<h3>,<p>,<ul>,<ol>,<li>,<strong>,<em>,blockquote.
            3. KHÔNG dùng <html>,<head>,<body>,<script>,<style>.
            4. Chỉ trình bày kiến thức CÓ trong tài liệu gốc liên quan tới chủ đề; KHÔNG thêm dữ kiện ngoài tài liệu.

            YÊU CẦU ĐỊNH DẠNG:
            Trả về CHÍNH XÁC theo JSON; KHÔNG thêm lời dẫn ngoài JSON.
            {formatInstructions}
            """;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private EvalConfig config;
    private ChatClient genClient;
    private ChatClient judgeClient;
    private GeminiEmbeddingClient embeddingClient;
    private Map<String, List<String>> factsByDocId;
    private final Map<String, String> pdfTextCache = new HashMap<>();
    private double cumulativeCostUsd = 0.0;

    @BeforeAll
    void setup() throws Exception {
        config = EvalConfigLoader.load();
        String apiKey = System.getenv("GOOGLE_GENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GOOGLE_GENAI_API_KEY required for ragcmp eval.");
        }

        Client genai = Client.builder().apiKey(apiKey).build();

        // Generation model: pin như AiQualityEvaluationTest (Pro, temp thấp).
        GoogleGenAiChatModel genModel = GoogleGenAiChatModel.builder()
                .genAiClient(genai)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(config.model().name())
                        .temperature(config.model().temperature())
                        .topP(config.model().topP())
                        .maxOutputTokens(config.model().maxOutputTokens())
                        .build())
                .build();
        genClient = ChatClient.builder(genModel)
                .defaultSystem("Bạn là một chuyên gia thiết kế nội dung e-learning (Instructional Designer).")
                .build();

        // Judge model: Flash, thinkingBudget 0 để JSON không bị cắt.
        GoogleGenAiChatModel judgeModel = GoogleGenAiChatModel.builder()
                .genAiClient(genai)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(config.model().judgeName())
                        .temperature(config.model().judgeTemperature())
                        .maxOutputTokens(2048)
                        .thinkingBudget(0)
                        .build())
                .build();
        judgeClient = ChatClient.builder(judgeModel).build();

        embeddingClient = new GeminiEmbeddingClient(apiKey, EMBED_MODEL, EMBED_DIMS);
        factsByDocId = loadGroundTruth();

        System.out.println("[ragcmp] setup: gen=" + config.model().name()
                + " judge=" + config.model().judgeName()
                + " embed=" + EMBED_MODEL + "@" + EMBED_DIMS
                + " | baselineMaxChars=" + BASELINE_MAX_CHARS
                + " chunk=" + CHUNK_SIZE + "/" + CHUNK_OVERLAP + " topK=" + TOP_K
                + " | docs=" + config.documents().size());
    }

    // ===== Test entry points: page-content =====
    @Test
    @DisplayName("Smoke page-content: RAG vs STUFF trên 1 tài liệu nhỏ nhất (≈$0.1)")
    void smokeRagComparison() throws Exception {
        runSmoke("page-content", "rag_comparison_smoke_");
    }

    @Test
    @DisplayName("Full page-content: RAG vs STUFF trên toàn bộ tài liệu test")
    void fullRagComparison() throws Exception {
        runFull("page-content", "rag_comparison_");
    }

    // ===== Test entry points: focused-quiz =====
    @Test
    @DisplayName("Smoke quiz: RAG vs STUFF trên 1 tài liệu nhỏ nhất (≈$0.1)")
    void smokeRagComparisonQuiz() throws Exception {
        runSmoke("quiz", "rag_comparison_quiz_smoke_");
    }

    @Test
    @DisplayName("Full quiz: RAG vs STUFF (focused-quiz) trên toàn bộ tài liệu test")
    void fullRagComparisonQuiz() throws Exception {
        runFull("quiz", "rag_comparison_quiz_");
    }

    // ---------------------------------------------------------------------
    // Runners
    // ---------------------------------------------------------------------

    private void runSmoke(String feature, String stemPrefix) throws Exception {
        EvalConfig.DocumentCfg doc = config.documents().stream()
                .min(Comparator.comparingInt(EvalConfig.DocumentCfg::actualPages))
                .orElseThrow();
        try (CsvReporter csv = openComparisonCsv(stemPrefix + nowStamp())) {
            evaluateDoc(doc, feature, csv);
        }
        System.out.println("[ragcmp] smoke(" + feature + ") DONE. cost=$"
                + String.format("%.4f", cumulativeCostUsd));
    }

    private void runFull(String feature, String stemPrefix) throws Exception {
        try (CsvReporter csv = openComparisonCsv(stemPrefix + nowStamp())) {
            int idx = 0;
            int total = config.documents().size();
            for (EvalConfig.DocumentCfg doc : config.documents()) {
                System.out.println("\n[ragcmp] (" + (++idx) + "/" + total + ") [" + feature + "] "
                        + doc.id() + " " + doc.file() + " (" + doc.actualPages() + "p, " + doc.language() + ")");
                try {
                    evaluateDoc(doc, feature, csv);
                } catch (Exception e) {
                    System.out.println("[ragcmp] doc " + doc.id() + " FAILED: " + e.getMessage());
                }
                if (cumulativeCostUsd > config.cost().totalBudgetUsd()) {
                    System.out.println("[ragcmp] STOP - over budget at $"
                            + String.format("%.4f", cumulativeCostUsd));
                    break;
                }
            }
        }
        System.out.println("\n[ragcmp] full(" + feature + ") DONE. cost=$"
                + String.format("%.4f", cumulativeCostUsd));
    }

    // ---------------------------------------------------------------------
    // Per-document: build both contexts once, run the chosen feature on each.
    // ---------------------------------------------------------------------

    private void evaluateDoc(EvalConfig.DocumentCfg doc, String feature, CsvReporter csv) throws Exception {
        String text = readPdfText(doc);
        String query = deriveTopic(doc);
        List<String> facts = factsByDocId.getOrDefault(doc.id(), List.of());

        // Build cả hai ngữ cảnh MỘT LẦN (đều tất định) rồi lặp generation RUNS lần
        // để giảm nhiễu LLM. Retrieval (embed + cosine top-k) không phụ thuộc run.
        String stuffContext = truncate(text, BASELINE_MAX_CHARS);

        long ragStart = System.nanoTime();
        List<String> chunks = DocumentChunker.chunk(text, CHUNK_SIZE, CHUNK_OVERLAP);
        List<float[]> chunkVecs = embeddingClient.embedDocuments(chunks);
        float[] queryVec = embeddingClient.embedQuery(query);
        List<String> top = topKByCosine(chunks, chunkVecs, queryVec, TOP_K);
        long retrievalMs = Duration.ofNanos(System.nanoTime() - ragStart).toMillis();
        String ragContext = String.join("\n\n---\n\n", top);
        System.out.println("[ragcmp]   RAG: " + chunks.size() + " chunks -> top-" + top.size()
                + " (" + ragContext.length() + " chars, retrieval " + retrievalMs + "ms)");

        for (int run = 1; run <= RUNS; run++) {
            runStrategy(doc, feature, "STUFF", run, query, stuffContext, -1, facts, csv);
            runStrategy(doc, feature, "RAG", run, query, ragContext, top.size(), facts, csv);
        }
    }

    /**
     * Sinh nội dung cho một {@code feature} dưới một {@code strategy} (STUFF|RAG),
     * đo token/latency/cost, chấm faithfulness, ghi 1 dòng CSV. Chỉ phần ngữ cảnh
     * ({@code context}) khác nhau giữa hai chiến lược.
     */
    private void runStrategy(EvalConfig.DocumentCfg doc, String feature, String strategy, int run,
                             String topic, String context, int retrievedChunks, List<String> facts,
                             CsvReporter csv) {
        long start = System.nanoTime();
        ChatResponse response = null;
        boolean schemaOk = false;
        String generatedText = "";
        String errorType = null;
        try {
            if ("quiz".equals(feature)) {
                BeanOutputConverter<AiQuizResponse> conv = new BeanOutputConverter<>(AiQuizResponse.class);
                response = genClient.prompt()
                        .user(u -> u.text(QuizPrompts.FROM_DOCUMENT_FOCUSED)
                                .param("focusTopic", topic)
                                .param("sourceText", context)
                                .param("courseTitle", doc.title())
                                .param("courseDescription", "")
                                .param("sectionTitle", "")
                                .param("pageTitle", "")
                                .param("numberOfQuestions", 5)
                                .param("difficulty", "Trung bình")
                                .param("language", languageFullName(doc.language()))
                                .param("fewShotExamples", "")
                                .param("formatInstructions", conv.getFormat()))
                        .call()
                        .chatResponse();
                AiQuizResponse quiz = conv.convert(stripJsonFence(response.getResult().getOutput().getText()));
                schemaOk = quiz != null && quiz.questions() != null && !quiz.questions().isEmpty();
                if (schemaOk) {
                    generatedText = summarizeQuiz(quiz);
                }
            } else {
                BeanOutputConverter<AiPageContentResponse> conv =
                        new BeanOutputConverter<>(AiPageContentResponse.class);
                response = genClient.prompt()
                        .user(u -> u.text(PAGE_CONTENT_PROMPT)
                                .param("courseTopic", doc.title())
                                .param("pageTopic", topic)
                                .param("language", languageFullName(doc.language()))
                                .param("context", context)
                                .param("formatInstructions", conv.getFormat()))
                        .call()
                        .chatResponse();
                AiPageContentResponse parsed =
                        conv.convert(stripJsonFence(response.getResult().getOutput().getText()));
                schemaOk = parsed != null && parsed.htmlContent() != null && !parsed.htmlContent().isBlank();
                if (schemaOk) {
                    generatedText = htmlToText(parsed.htmlContent());
                }
            }
        } catch (Exception e) {
            errorType = e.getClass().getSimpleName();
        }
        long latencyMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        int inTok = 0, outTok = 0, totTok = 0;
        if (response != null && response.getMetadata() != null) {
            Usage u = response.getMetadata().getUsage();
            if (u != null) {
                inTok = nz(u.getPromptTokens());
                outTok = nz(u.getCompletionTokens());
                totTok = nz(u.getTotalTokens());
            }
        }
        double genCost = config.cost().computeUsd(inTok, outTok);
        cumulativeCostUsd += genCost;

        // --- LLM-judge faithfulness vs key facts ---
        int faithScore = 0;
        boolean hallucination = false;
        int unsupportedCount = 0;
        double judgeCost = 0.0;
        if (schemaOk && !facts.isEmpty()) {
            JudgeResult jr = judgeFaithfulness(facts, generatedText);
            faithScore = jr.score();
            hallucination = jr.hallucination();
            unsupportedCount = jr.unsupportedCount();
            judgeCost = jr.costUsd();
            cumulativeCostUsd += judgeCost;
        }

        Map<String, Object> row = CsvReporter.row();
        row.put("timestamp", LocalDateTime.now().toString());
        row.put("doc_id", doc.id());
        row.put("doc_pages", doc.actualPages());
        row.put("doc_language", doc.language());
        row.put("feature", feature);
        row.put("strategy", strategy);
        row.put("run", run);
        row.put("topic", truncate(topic, 80));
        row.put("context_chars", context.length());
        row.put("retrieved_chunks", retrievedChunks);
        row.put("prompt_tokens", inTok);
        row.put("completion_tokens", outTok);
        row.put("total_tokens", totTok);
        row.put("latency_ms", latencyMs);
        row.put("gen_cost_usd", String.format("%.6f", genCost));
        row.put("gen_schema_ok", schemaOk);
        row.put("faithfulness_score", faithScore);
        row.put("hallucination", hallucination);
        row.put("unsupported_claims_count", unsupportedCount);
        row.put("judge_cost_usd", String.format("%.6f", judgeCost));
        row.put("error_type", errorType == null ? "" : errorType);
        try {
            csv.writeRow(row);
        } catch (Exception e) {
            System.err.println("[ragcmp] CSV write failed: " + e.getMessage());
        }

        if (SAVE_OUTPUTS && schemaOk) {
            saveOutput(doc, feature, strategy, run, topic, generatedText, facts, faithScore, hallucination);
        }

        System.out.println(String.format(
                "[ragcmp]   %-12s %-5s run%d ctx=%6d in=%6d out=%5d lat=%5dms $%.4f faith=%d hallu=%s%s",
                feature, strategy, run, context.length(), inTok, outTok, latencyMs, genCost,
                faithScore, hallucination, errorType == null ? "" : " ERR=" + errorType));
    }

    /**
     * Lưu nội dung sinh ra + điểm judge để cross-check faithfulness về sau
     * (reviewer thứ 2 đọc generated_text + facts, chấm độc lập rồi tính κ).
     */
    private void saveOutput(EvalConfig.DocumentCfg doc, String feature, String strategy, int run,
                            String topic, String generatedText, List<String> facts,
                            int judgeScore, boolean judgeHallucination) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        String sampleId = doc.id() + "_" + feature + "_" + strategy + "_run" + run;
        payload.put("sample_id", sampleId);
        payload.put("doc_id", doc.id());
        payload.put("doc_title", doc.title());
        payload.put("feature", feature);
        payload.put("strategy", strategy);
        payload.put("run", run);
        payload.put("topic", topic);
        payload.put("key_facts", facts);
        payload.put("generated_text", generatedText);
        payload.put("judge_faithfulness_score", judgeScore);
        payload.put("judge_hallucination", judgeHallucination);
        try {
            Files.createDirectories(OUTPUTS_DIR);
            jsonMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(OUTPUTS_DIR.resolve(sampleId + ".json").toFile(), payload);
        } catch (Exception e) {
            System.err.println("[ragcmp] save-output failed for " + sampleId + ": " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // In-memory cosine retrieval (equivalent ranking to pgvector <=>).
    // ---------------------------------------------------------------------

    private static List<String> topKByCosine(List<String> chunks, List<float[]> vecs,
                                             float[] query, int k) {
        record Scored(String text, double sim) {
        }
        List<Scored> scored = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size() && i < vecs.size(); i++) {
            scored.add(new Scored(chunks.get(i), cosine(query, vecs.get(i))));
        }
        scored.sort(Comparator.comparingDouble(Scored::sim).reversed());
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(k, scored.size()); i++) {
            out.add(scored.get(i).text());
        }
        return out;
    }

    private static double cosine(float[] a, float[] b) {
        if (a == null || b == null) {
            return -1;
        }
        double dot = 0, na = 0, nb = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return -1;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    // ---------------------------------------------------------------------
    // LLM-judge (mirrors LlmJudgeTest rubric).
    // ---------------------------------------------------------------------

    private record JudgeResult(int score, boolean hallucination, int unsupportedCount, double costUsd) {
    }

    private JudgeResult judgeFaithfulness(List<String> facts, String generatedText) {
        String factsBlock = String.join("\n", facts.stream().map(f -> "- " + f).toList());
        String judgePrompt = """
                You are evaluating AI-generated educational content for a Vietnamese e-learning platform.

                GROUND-TRUTH FACTS (extracted manually from the source document):
                =================================
                %s
                =================================

                AI-GENERATED CONTENT (feature: page-content):
                =================================
                %s
                =================================

                TASK: Judge how faithful the generated content is to the source. Rubric:
                - 5: Every substantive claim is supported by or consistent with the source facts.
                - 4: Mostly faithful; at most one minor unsupported but plausible claim.
                - 3: A few unsupported claims, but no direct contradiction with the source.
                - 2: Multiple unsupported claims OR one clear contradiction.
                - 1: Major hallucinations - claims that contradict the source.

                Return ONLY a JSON object (no markdown fences) with this exact shape:
                {
                  "faithfulness_score": <integer 1-5>,
                  "hallucination": <true|false>,
                  "unsupported_claims": [<string>, ...],
                  "reasoning": "<one short sentence>"
                }
                """.formatted(factsBlock, truncate(generatedText, 3500));

        ChatResponse response = null;
        int score = 0;
        boolean hallucination = false;
        int unsupported = 0;
        try {
            response = judgeClient.prompt().user(judgePrompt).call().chatResponse();
            String cleaned = stripJsonFence(response.getResult().getOutput().getText());
            JsonNode node = jsonMapper.readTree(cleaned);
            score = node.path("faithfulness_score").asInt(0);
            hallucination = node.path("hallucination").asBoolean(false);
            JsonNode claims = node.path("unsupported_claims");
            unsupported = claims.isArray() ? claims.size() : 0;
        } catch (Exception e) {
            // fail-soft: score stays 0 (treated as missing in analysis)
        }
        int inTok = 0, outTok = 0;
        if (response != null && response.getMetadata() != null) {
            Usage u = response.getMetadata().getUsage();
            if (u != null) {
                inTok = nz(u.getPromptTokens());
                outTok = nz(u.getCompletionTokens());
            }
        }
        double cost = (inTok / 1_000_000.0) * 0.30 + (outTok / 1_000_000.0) * 2.50;
        return new JudgeResult(score, hallucination, unsupported, cost);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private CsvReporter openComparisonCsv(String stem) throws Exception {
        return new CsvReporter(RESULTS_DIR.resolve(stem + ".csv"),
                "timestamp", "doc_id", "doc_pages", "doc_language", "feature", "strategy", "run", "topic",
                "context_chars", "retrieved_chunks", "prompt_tokens", "completion_tokens",
                "total_tokens", "latency_ms", "gen_cost_usd", "gen_schema_ok",
                "faithfulness_score", "hallucination", "unsupported_claims_count",
                "judge_cost_usd", "error_type");
    }

    private Map<String, List<String>> loadGroundTruth() throws Exception {
        JsonNode root = jsonMapper.readTree(GROUND_TRUTH_PATH.toFile());
        Map<String, List<String>> map = new HashMap<>();
        for (JsonNode doc : root.path("documents")) {
            List<String> facts = new ArrayList<>();
            for (JsonNode fact : doc.path("key_facts")) {
                facts.add(fact.path("fact").asText());
            }
            map.put(doc.path("doc_id").asText(), facts);
        }
        return map;
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
        StringBuilder sb = new StringBuilder();
        for (Document d : reader.get()) {
            sb.append(d.getText()).append('\n');
        }
        String text = sb.toString();
        pdfTextCache.put(doc.id(), text);
        return text;
    }

    private static String deriveTopic(EvalConfig.DocumentCfg doc) {
        String title = doc.title();
        if (title == null || title.isBlank()) {
            return "Tổng quan";
        }
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

    private static String htmlToText(String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    /** Tóm tắt bộ câu hỏi quiz thành text để LLM-judge chấm faithfulness. */
    private static String summarizeQuiz(AiQuizResponse quiz) {
        StringBuilder sb = new StringBuilder();
        List<AiQuizResponse.AiQuestion> questions = quiz.questions();
        for (int i = 0; i < questions.size(); i++) {
            AiQuizResponse.AiQuestion q = questions.get(i);
            sb.append(i + 1).append(". [").append(q.type()).append("] ").append(q.prompt()).append('\n');
            if (q.options() != null) {
                for (int j = 0; j < q.options().size(); j++) {
                    sb.append("   ").append((char) ('A' + j)).append(") ").append(q.options().get(j)).append('\n');
                }
            }
            if (q.correctAnswer() != null) {
                sb.append("   Correct: ").append(q.correctAnswer()).append('\n');
            }
        }
        return sb.toString();
    }

    private static String stripJsonFence(String raw) {
        return raw == null ? "" : raw.replace("```json", "").replace("```", "").trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String languageFullName(String code) {
        return switch (code == null ? "" : code) {
            case "en" -> "English";
            default -> "Vietnamese";
        };
    }

    private static int nz(Integer i) {
        return i == null ? 0 : i;
    }

    private static String nowStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}
