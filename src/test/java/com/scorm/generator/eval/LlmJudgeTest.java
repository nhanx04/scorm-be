package com.scorm.generator.eval;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;

/**
 * Group C — LLM-as-judge automated faithfulness evaluation.
 *
 * <p>For each saved output produced by {@link AiQualityEvaluationTest#saveOutputsPass()},
 * feed the source ground-truth facts plus the generated content into Gemini 2.5 Flash
 * (the cheaper judge model) and ask it to:
 *
 * <ul>
 *   <li>Score faithfulness 1–5</li>
 *   <li>Flag whether any hallucination exists</li>
 *   <li>List specific unsupported claims (if any)</li>
 *   <li>Give one-line reasoning</li>
 * </ul>
 *
 * <p>Uses Flash (~10× cheaper than Pro) since the judge task is structured
 * and doesn't need the strongest model. Cost estimate: ~$0.20 for 30 outputs.
 *
 * <p>Gated by {@code -Dai-eval.enabled=true} like the main eval.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "ai-eval.enabled", matches = "true")
class LlmJudgeTest {

    // Override which ground-truth file to load via -Dai-eval.gt-file=...
    // Default: 5-facts/doc baseline. Pass "ground_truth_expanded.json" to use
    // the D8 AI-expanded version (~15 facts/doc).
    private static final Path GROUND_TRUTH_PATH = Paths.get(
            "src/test/resources/ai-eval/ground-truth/" +
                    System.getProperty("ai-eval.gt-file", "ground_truth.json"));
    private static final Path RESULTS_DIR =
            Paths.get("src/test/resources/ai-eval/results");
    // Allow judging post-fix outputs without overwriting baseline judge CSV.
    private static final Path OUTPUTS_DIR =
            RESULTS_DIR.resolve(System.getProperty("ai-eval.outputs-subdir", "outputs"));
    private static final String CSV_SUFFIX =
            System.getProperty("ai-eval.csv-suffix", "");

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private EvalConfig config;
    private ChatClient judgeClient;
    private Map<String, List<String>> factsByDocId;
    private double cumulativeCostUsd = 0.0;

    @BeforeAll
    void setup() throws Exception {
        config = EvalConfigLoader.load();
        String apiKey = System.getenv("GOOGLE_GENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GOOGLE_GENAI_API_KEY required");
        }

        Client genai = Client.builder().apiKey(apiKey).build();
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(config.model().judgeName())
                .temperature(config.model().judgeTemperature())
                .maxOutputTokens(2048)
                // Disable "thinking" so the full output budget goes to the actual
                // JSON response. Without this Flash spends most of maxOutputTokens
                // on internal reasoning and the JSON gets truncated mid-string.
                .thinkingBudget(0)
                .build();
        GoogleGenAiChatModel model = GoogleGenAiChatModel.builder()
                .genAiClient(genai)
                .defaultOptions(options)
                .build();
        judgeClient = ChatClient.builder(model).build();

        factsByDocId = loadGroundTruth();
        System.out.println("[ai-eval] Judge: model=" + config.model().judgeName()
                + ", temp=" + config.model().judgeTemperature()
                + ", ground-truth docs=" + factsByDocId.size());
    }

    @Test
    @DisplayName("Run LLM-judge faithfulness on all saved outputs")
    void runJudgeOnSavedOutputs() throws Exception {
        if (!Files.isDirectory(OUTPUTS_DIR)) {
            throw new IllegalStateException("No outputs dir at " + OUTPUTS_DIR
                    + " - run saveOutputsPass first.");
        }

        List<Path> outputFiles;
        try (Stream<Path> s = Files.list(OUTPUTS_DIR)) {
            outputFiles = s.filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
        if (outputFiles.isEmpty()) {
            throw new IllegalStateException("No JSON outputs found");
        }
        System.out.println("[ai-eval] Judging " + outputFiles.size() + " outputs");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String stem = "faithfulness_auto"
                + (CSV_SUFFIX.isEmpty() ? "" : "_" + CSV_SUFFIX)
                + "_" + timestamp;
        Path csvPath = RESULTS_DIR.resolve(stem + ".csv");

        try (CsvReporter csv = new CsvReporter(csvPath,
                "timestamp", "doc_id", "feature", "run", "judge_model",
                "faithfulness_score", "hallucination", "unsupported_claims_count",
                "reasoning", "judge_latency_ms", "judge_input_tokens",
                "judge_output_tokens", "judge_cost_usd", "judge_parse_ok",
                "error_type")) {

            int idx = 0;
            for (Path output : outputFiles) {
                idx++;
                Map<String, Object> data = jsonMapper.readValue(
                        output.toFile(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                String docId = (String) data.get("doc_id");
                String feature = (String) data.get("feature");
                Integer run = (Integer) data.get("run");
                Boolean schemaOk = (Boolean) data.get("schema_ok");

                if (schemaOk == null || !schemaOk) {
                    System.out.println("[ai-eval] (" + idx + "/" + outputFiles.size()
                            + ") SKIP " + output.getFileName() + " - schema not OK");
                    continue;
                }

                String generatedSummary = summarizeGenerated(feature, data.get("parsed"));
                List<String> facts = factsByDocId.getOrDefault(docId, List.of());
                if (facts.isEmpty()) {
                    System.out.println("[ai-eval] (" + idx + "/" + outputFiles.size()
                            + ") SKIP " + docId + " - no ground truth");
                    continue;
                }

                System.out.println("[ai-eval] (" + idx + "/" + outputFiles.size()
                        + ") judging " + docId + "/" + feature + " run" + run);
                Map<String, Object> row = judgeOne(docId, feature, run, facts, generatedSummary);
                csv.writeRow(row);
            }

            System.out.println("\n[ai-eval] Judge DONE. " + csv.getRowCount()
                    + " rows. Cost=$" + String.format("%.4f", cumulativeCostUsd));
            System.out.println("[ai-eval] CSV: " + csvPath);
        }
    }

    // ---------------------------------------------------------------------
    // Per-output judging
    // ---------------------------------------------------------------------

    private Map<String, Object> judgeOne(String docId, String feature, Integer run,
                                          List<String> facts, String generatedSummary) {
        String factsBlock = String.join("\n", facts.stream()
                .map(f -> "- " + f)
                .toList());

        String judgePrompt = """
                You are evaluating AI-generated educational content for a Vietnamese e-learning platform.

                GROUND-TRUTH FACTS (extracted manually from the source document):
                =================================
                %s
                =================================

                AI-GENERATED CONTENT (feature: %s):
                =================================
                %s
                =================================

                TASK:
                Judge how faithful the generated content is to the source. Use this rubric:
                - 5: Every substantive claim in the output is supported by or consistent with the source facts.
                - 4: Mostly faithful; at most one minor unsupported but plausible claim.
                - 3: A few unsupported claims, but no direct contradiction with the source.
                - 2: Multiple unsupported claims OR one clear contradiction with the source.
                - 1: Major hallucinations - claims that contradict the source.

                Return ONLY a JSON object (no markdown fences) with this exact shape:
                {
                  "faithfulness_score": <integer 1-5>,
                  "hallucination": <true|false>,
                  "unsupported_claims": [<string>, ...],
                  "reasoning": "<one short sentence in Vietnamese>"
                }
                """.formatted(factsBlock, feature, generatedSummary);

        long start = System.nanoTime();
        ChatResponse response = null;
        String raw = "";
        String errorType = null;
        int score = 0;
        boolean hallucination = false;
        int unsupportedCount = 0;
        String reasoning = "";
        boolean parseOk = false;

        try {
            response = judgeClient.prompt()
                    .user(judgePrompt)
                    .call()
                    .chatResponse();
            raw = response.getResult().getOutput().getText();
            String cleaned = raw.replace("```json", "").replace("```", "").trim();

            JsonNode node = jsonMapper.readTree(cleaned);
            score = node.path("faithfulness_score").asInt(0);
            hallucination = node.path("hallucination").asBoolean(false);
            JsonNode claims = node.path("unsupported_claims");
            unsupportedCount = claims.isArray() ? claims.size() : 0;
            reasoning = node.path("reasoning").asText("");
            parseOk = score >= 1 && score <= 5;
        } catch (Exception e) {
            errorType = e.getClass().getSimpleName();
        }

        long latencyMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        int inTok = 0, outTok = 0;
        if (response != null && response.getMetadata() != null) {
            Usage u = response.getMetadata().getUsage();
            if (u != null) {
                inTok = nz(u.getPromptTokens());
                outTok = nz(u.getCompletionTokens());
            }
        }
        // Flash pricing (rough): $0.30/M input, $2.50/M output
        double cost = (inTok / 1_000_000.0) * 0.30 + (outTok / 1_000_000.0) * 2.50;
        cumulativeCostUsd += cost;

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("timestamp", LocalDateTime.now().toString());
        row.put("doc_id", docId);
        row.put("feature", feature);
        row.put("run", run);
        row.put("judge_model", config.model().judgeName());
        row.put("faithfulness_score", score);
        row.put("hallucination", hallucination);
        row.put("unsupported_claims_count", unsupportedCount);
        row.put("reasoning", truncate(reasoning, 250));
        row.put("judge_latency_ms", latencyMs);
        row.put("judge_input_tokens", inTok);
        row.put("judge_output_tokens", outTok);
        row.put("judge_cost_usd", String.format("%.6f", cost));
        row.put("judge_parse_ok", parseOk);
        row.put("error_type", errorType == null ? "" : errorType);

        System.out.println(String.format(
                "[ai-eval]   -> score=%d hallu=%s claims=%d latency=%dms $%.5f%s",
                score, hallucination, unsupportedCount, latencyMs, cost,
                errorType == null ? "" : " ERR=" + errorType));
        return row;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Turn the {@code parsed} JSON node from a saved output into a concise text
     * block the judge can read.
     */
    @SuppressWarnings("unchecked")
    private String summarizeGenerated(String feature, Object parsed) {
        if (parsed == null) {
            return "(no parsed content)";
        }
        Map<String, Object> map = (Map<String, Object>) parsed;
        return switch (feature) {
            case "outline" -> summarizeOutline(map);
            case "page-content" -> summarizePageContent(map);
            case "quiz" -> summarizeQuiz(map);
            default -> map.toString();
        };
    }

    @SuppressWarnings("unchecked")
    private String summarizeOutline(Map<String, Object> outline) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(outline.get("title")).append('\n');
        if (outline.get("description") != null) {
            sb.append("Description: ").append(outline.get("description")).append('\n');
        }
        List<Map<String, Object>> sections = (List<Map<String, Object>>) outline.get("sections");
        if (sections != null) {
            sb.append("Sections:\n");
            for (int i = 0; i < sections.size(); i++) {
                Map<String, Object> s = sections.get(i);
                sb.append("  ").append(i + 1).append(". ").append(s.get("title")).append('\n');
                List<String> topics = (List<String>) s.get("topics");
                if (topics != null) {
                    for (String t : topics) {
                        sb.append("     - ").append(t).append('\n');
                    }
                }
            }
        }
        return sb.toString();
    }

    private String summarizePageContent(Map<String, Object> page) {
        StringBuilder sb = new StringBuilder();
        sb.append("Page title: ").append(page.get("pageTitle")).append('\n');
        if (page.get("shortSummary") != null) {
            sb.append("Summary: ").append(page.get("shortSummary")).append('\n');
        }
        String html = (String) page.get("htmlContent");
        if (html != null) {
            // Strip HTML tags for the judge
            String stripped = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            sb.append("Content (text):\n").append(truncate(stripped, 3500));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String summarizeQuiz(Map<String, Object> quiz) {
        StringBuilder sb = new StringBuilder();
        List<Map<String, Object>> questions = (List<Map<String, Object>>) quiz.get("questions");
        if (questions == null) {
            return "(no questions)";
        }
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            sb.append(i + 1).append(". [").append(q.get("type")).append("] ")
                    .append(q.get("prompt")).append('\n');
            if (q.get("options") != null) {
                List<String> opts = (List<String>) q.get("options");
                for (int j = 0; j < opts.size(); j++) {
                    sb.append("   ").append((char) ('A' + j)).append(") ")
                            .append(opts.get(j)).append('\n');
                }
            }
            Object ans = q.get("correctAnswer");
            if (ans != null) {
                sb.append("   Correct: ").append(ans).append('\n');
            }
        }
        return sb.toString();
    }

    private Map<String, List<String>> loadGroundTruth() throws Exception {
        JsonNode root = jsonMapper.readTree(GROUND_TRUTH_PATH.toFile());
        Map<String, List<String>> map = new HashMap<>();
        for (JsonNode doc : root.path("documents")) {
            String docId = doc.path("doc_id").asText();
            List<String> facts = new ArrayList<>();
            for (JsonNode fact : doc.path("key_facts")) {
                facts.add(fact.path("fact").asText());
            }
            map.put(docId, facts);
        }
        return map;
    }

    private static int nz(Integer i) {
        return i == null ? 0 : i;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
