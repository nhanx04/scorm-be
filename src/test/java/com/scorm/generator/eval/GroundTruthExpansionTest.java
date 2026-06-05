package com.scorm.generator.eval;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.genai.Client;

/**
 * D8 — AI-assisted ground truth expansion.
 *
 * <p>The original ground truth has 5 facts/doc which created a sparse-GT
 * bias in D3/D6 LLM-judge measurements (judge flagged ~70% of generated
 * claims as "unsupported" simply because GT didn't cover the source).
 *
 * <p>This test asks Gemini 2.5 Pro to extract additional atomic facts
 * from each source document. Each extracted fact is auto-verified by
 * substring-matching the verbatim quote against the source text — facts
 * that fail substring verification are dropped (model fabricated the
 * quote). Survivors are merged into {@code ground_truth_expanded.json}
 * alongside the original 5 manual facts.
 *
 * <p>This is the same AI-assisted methodology D4 used for IWF scoring;
 * thesis 5.4.7 should disclose it consistently.
 *
 * <p>Gated by {@code -Dai-eval.enabled=true}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "ai-eval.enabled", matches = "true")
class GroundTruthExpansionTest {

    private static final Path RESULTS_DIR =
            Paths.get("src/test/resources/ai-eval/results");
    private static final Path DOCS_DIR =
            Paths.get("src/test/resources/ai-eval/documents");
    private static final Path GT_PATH =
            Paths.get("src/test/resources/ai-eval/ground-truth/ground_truth.json");
    private static final Path GT_EXPANDED_PATH =
            Paths.get("src/test/resources/ai-eval/ground-truth/ground_truth_expanded.json");

    private static final int MIN_QUOTE_LENGTH = 6;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final ObjectMapper json = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private EvalConfig config;
    private ChatClient extractorClient;
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
                .model(config.model().name())   // Gemini 2.5 Pro
                .temperature(0.0)               // deterministic extraction
                .maxOutputTokens(8192)
                .build();
        GoogleGenAiChatModel model = GoogleGenAiChatModel.builder()
                .genAiClient(genai)
                .defaultOptions(options)
                .build();
        extractorClient = ChatClient.builder(model).build();

        System.out.println("[gt-expand] model=" + config.model().name()
                + " temp=0.0  (deterministic extraction)");
    }

    @Test
    @DisplayName("Expand each doc's GT from 5 → ~15 facts (D05: ~25) via AI extract + substring verify")
    void expandGroundTruth() throws Exception {
        JsonNode origRoot = json.readTree(GT_PATH.toFile());

        ObjectNode expandedRoot = json.createObjectNode();
        expandedRoot.put("schema_version", "2.0");
        expandedRoot.put("extracted_at", LocalDateTime.now().toString());
        expandedRoot.put("description",
                "Expanded ground truth — original 5 manual facts/doc PLUS AI-extracted facts "
                + "(Gemini 2.5 Pro, temp=0.0) that passed substring verification against source.");
        ArrayNode extractionNotes = json.createArrayNode();
        extractionNotes.add("Original 5 facts/doc remain marked source=manual.");
        extractionNotes.add("AI-extracted facts marked source=ai_extracted with verbatim_quote.");
        extractionNotes.add("Each AI fact's verbatim_quote was substring-verified against "
                + "Tika-extracted source text before inclusion. Facts failing this check were dropped.");
        extractionNotes.add("Targets: 15 facts/doc; D05 (360p): 25 facts. Actual yield depends on "
                + "substring survival rate.");
        expandedRoot.set("extraction_notes", extractionNotes);

        ArrayNode docsOut = json.createArrayNode();
        expandedRoot.set("documents", docsOut);

        int totalOriginal = 0;
        int totalExtracted = 0;
        int totalSurvived = 0;

        for (EvalConfig.DocumentCfg doc : config.documents()) {
            int targetNew = "D05".equals(doc.id()) ? 20 : 10;
            System.out.println("\n[gt-expand] ----- " + doc.id() + " " + doc.file()
                    + " (target +" + targetNew + " facts) -----");

            String sourceText = readPdfText(doc);
            String normalisedSource = normalise(sourceText);

            // Find original facts block
            ArrayNode origFacts = findOriginalFacts(origRoot, doc.id());

            // Ask Gemini to extract atomic facts
            List<ExtractedFact> candidates = extractFacts(doc, sourceText, targetNew);
            totalExtracted += candidates.size();

            // Substring-verify each candidate
            List<ExtractedFact> survived = new ArrayList<>();
            for (ExtractedFact f : candidates) {
                String normQuote = normalise(f.verbatimQuote);
                if (normQuote.length() >= MIN_QUOTE_LENGTH
                        && normalisedSource.contains(normQuote)) {
                    survived.add(f);
                }
            }
            totalSurvived += survived.size();
            System.out.println("[gt-expand]   extracted=" + candidates.size()
                    + ", survived substring=" + survived.size()
                    + " (" + (candidates.isEmpty() ? 0
                            : 100 * survived.size() / candidates.size()) + "%)");

            // Build merged fact list — original first, then AI-extracted
            ArrayNode mergedFacts = json.createArrayNode();
            int origCount = origFacts == null ? 0 : origFacts.size();
            totalOriginal += origCount;
            if (origFacts != null) {
                for (int i = 0; i < origFacts.size(); i++) {
                    ObjectNode orig = (ObjectNode) origFacts.get(i).deepCopy();
                    orig.put("source", "manual");
                    mergedFacts.add(orig);
                }
            }
            int next = origCount + 1;
            for (ExtractedFact f : survived) {
                ObjectNode aiFact = json.createObjectNode();
                aiFact.put("id", doc.id() + "-F" + next++);
                aiFact.put("fact", f.fact);
                aiFact.put("verbatim_quote", f.verbatimQuote);
                aiFact.put("source", "ai_extracted");
                mergedFacts.add(aiFact);
            }

            ObjectNode docNode = json.createObjectNode();
            docNode.put("doc_id", doc.id());
            docNode.put("file", doc.file());
            docNode.put("title", doc.title());
            docNode.put("language", doc.language());
            docNode.put("manual_facts_count", origCount);
            docNode.put("ai_extracted_facts_count", survived.size());
            docNode.set("key_facts", mergedFacts);
            docsOut.add(docNode);
        }

        expandedRoot.put("totals",
                String.format("manual=%d, ai_extracted=%d (from %d candidates, "
                        + "substring survival %d%%), total=%d",
                        totalOriginal, totalSurvived, totalExtracted,
                        totalExtracted == 0 ? 0 : 100 * totalSurvived / totalExtracted,
                        totalOriginal + totalSurvived));

        Files.createDirectories(GT_EXPANDED_PATH.getParent());
        json.writeValue(GT_EXPANDED_PATH.toFile(), expandedRoot);

        System.out.println("\n[gt-expand] DONE.");
        System.out.println("  Original facts:    " + totalOriginal);
        System.out.println("  AI candidates:     " + totalExtracted);
        System.out.println("  Substring-survived: " + totalSurvived
                + " (" + (totalExtracted == 0 ? 0 : 100 * totalSurvived / totalExtracted) + "%)");
        System.out.println("  Total merged:      " + (totalOriginal + totalSurvived));
        System.out.println("  Cost: $" + String.format("%.4f", cumulativeCostUsd));
        System.out.println("  Output: " + GT_EXPANDED_PATH);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private List<ExtractedFact> extractFacts(EvalConfig.DocumentCfg doc,
                                              String sourceText, int target) {
        String trimmed = sourceText.length() > 30_000
                ? sourceText.substring(0, 30_000) + "\n[...truncated...]"
                : sourceText;
        String prompt = ("""
                Bạn là chuyên gia bóc tách (extract) atomic facts từ tài liệu giáo dục \
                để xây ground truth cho đánh giá hệ thống AI sinh câu hỏi.

                TÀI LIỆU NGUỒN:
                =================================
                %s
                =================================

                NHIỆM VỤ: Trích xuất CHÍNH XÁC %d atomic facts từ tài liệu trên.

                QUY TẮC NGHIÊM NGẶT cho mỗi fact:
                1. **fact**: 1 câu khẳng định ngắn (≤ 30 từ), verifiable yes/no, KHÔNG phải opinion.
                2. **verbatim_quote**: chuỗi TRÍCH NGUYÊN VĂN từ tài liệu (5-30 từ), LIỀN MẠCH trong 1 câu.
                   TUYỆT ĐỐI KHÔNG paraphrase, dịch, thêm/xoá dấu câu, hay nối nhiều đoạn bằng "...".
                3. Mỗi fact target một KHÁI NIỆM khác nhau — không trùng lặp.
                4. Ưu tiên facts là: định nghĩa, công thức, taxonomy, số liệu cụ thể, tên tác giả/năm.
                5. TRÁNH facts mơ hồ kiểu "tài liệu nói về X" — phải cụ thể, verifiable.
                6. Ngôn ngữ fact + quote phải khớp ngôn ngữ tài liệu (%s).

                Trả về JSON đúng format sau (KHÔNG markdown fence, KHÔNG giải thích thêm):
                [
                  {"fact": "...", "verbatim_quote": "..."},
                  {"fact": "...", "verbatim_quote": "..."},
                  ...
                ]
                Đúng %d phần tử trong mảng.
                """).formatted(trimmed, target, languageFullName(doc.language()), target);

        ChatResponse response = extractorClient.prompt().user(prompt).call().chatResponse();
        String raw = response.getResult().getOutput().getText();
        accumulateCost(response);

        String cleaned = raw.replace("```json", "").replace("```", "").trim();
        List<ExtractedFact> result = new ArrayList<>();
        try {
            JsonNode arr = json.readTree(cleaned);
            if (arr.isArray()) {
                for (JsonNode item : arr) {
                    String f = item.path("fact").asText();
                    String q = item.path("verbatim_quote").asText();
                    if (!f.isBlank() && !q.isBlank()) {
                        result.add(new ExtractedFact(f, q));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[gt-expand] parse failed for " + doc.id() + ": " + e.getMessage());
        }
        return result;
    }

    private void accumulateCost(ChatResponse response) {
        if (response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return;
        }
        var u = response.getMetadata().getUsage();
        int in = u.getPromptTokens() == null ? 0 : u.getPromptTokens();
        int out = u.getCompletionTokens() == null ? 0 : u.getCompletionTokens();
        cumulativeCostUsd += config.cost().computeUsd(in, out);
    }

    private String readPdfText(EvalConfig.DocumentCfg doc) throws Exception {
        Path file = DOCS_DIR.resolve(doc.file());
        TikaDocumentReader reader = new TikaDocumentReader(
                new FileSystemResource(file.toFile()));
        List<Document> docs = reader.get();
        StringBuilder sb = new StringBuilder();
        for (Document d : docs) {
            sb.append(d.getText()).append('\n');
        }
        return sb.toString();
    }

    private static ArrayNode findOriginalFacts(JsonNode root, String docId) {
        for (JsonNode d : root.path("documents")) {
            if (docId.equals(d.path("doc_id").asText())) {
                return (ArrayNode) d.path("key_facts");
            }
        }
        return null;
    }

    private static String normalise(String s) {
        return WHITESPACE.matcher(s.toLowerCase()
                .replace('“', '"').replace('”', '"')
                .replace('‘', '\'').replace('’', '\''))
                .replaceAll(" ").trim();
    }

    private static String languageFullName(String code) {
        return switch (code == null ? "vi" : code) {
            case "en" -> "English";
            default -> "Vietnamese";
        };
    }

    private record ExtractedFact(String fact, String verbatimQuote) {
    }

    // Silence unused import warning — Map is referenced via TypeReference elsewhere
    @SuppressWarnings("unused")
    private static final Class<?> KEEP_MAP_IMPORT = Map.class;

    @SuppressWarnings("unused")
    private static final Class<?> KEEP_RESULTS_DIR = RESULTS_DIR.getClass();
}
