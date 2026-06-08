package com.scorm.generator.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client gọi trực tiếp Gemini Embedding API (Google AI Studio / Generative
 * Language API) bằng API key.
 *
 * <p>Vì sao không dùng EmbeddingModel của Spring AI: bản google-genai trong
 * Spring AI 1.1.0-M1 chỉ ship phần chat, KHÔNG có embedding model
 * ({@code GoogleGenAiTextEmbeddingModel} không tồn tại trên classpath nên
 * autoconfig embedding không bao giờ khớp). Gọi REST thẳng giúp RAG hoạt động
 * với đúng API key hiện có mà không phải nâng version Spring AI giữa chừng.
 *
 * <p>Dùng {@code taskType} đúng ngữ cảnh (RETRIEVAL_DOCUMENT khi index,
 * RETRIEVAL_QUERY khi truy vấn) và {@code outputDimensionality} để cắt vector
 * (MRL) cho khớp cột {@code vector(N)} trong DB.
 */
@Component
public class GeminiEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingClient.class);

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String TASK_DOCUMENT = "RETRIEVAL_DOCUMENT";
    private static final String TASK_QUERY = "RETRIEVAL_QUERY";
    /** Giới hạn số văn bản mỗi request batch của Gemini. */
    private static final int MAX_BATCH = 100;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int dimensions;

    public GeminiEmbeddingClient(
            @Value("${app.ai.rag.embedding.api-key:${GOOGLE_GENAI_API_KEY:}}") String apiKey,
            @Value("${app.ai.rag.embedding.model:gemini-embedding-001}") String model,
            @Value("${app.ai.rag.embedding-dimension:768}") int dimensions) {
        this.apiKey = apiKey;
        this.model = model;
        this.dimensions = dimensions;
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    /** RAG chỉ bật khi có API key. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Nhúng một câu truy vấn (taskType = RETRIEVAL_QUERY). */
    public float[] embedQuery(String text) {
        List<float[]> result = embed(List.of(text), TASK_QUERY);
        return result.isEmpty() ? null : result.get(0);
    }

    /** Nhúng các đoạn tài liệu để index (taskType = RETRIEVAL_DOCUMENT). */
    public List<float[]> embedDocuments(List<String> texts) {
        return embed(texts, TASK_DOCUMENT);
    }

    private List<float[]> embed(List<String> texts, String taskType) {
        if (!isConfigured() || texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<float[]> all = new ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += MAX_BATCH) {
            int to = Math.min(from + MAX_BATCH, texts.size());
            all.addAll(callBatch(texts.subList(from, to), taskType));
        }
        return all;
    }

    @SuppressWarnings("unchecked")
    private List<float[]> callBatch(List<String> texts, String taskType) {
        String qualifiedModel = "models/" + model;
        List<Map<String, Object>> requests = new ArrayList<>(texts.size());
        for (String text : texts) {
            requests.add(Map.of(
                    "model", qualifiedModel,
                    "content", Map.of("parts", List.of(Map.of("text", text))),
                    "taskType", taskType,
                    "outputDimensionality", dimensions));
        }

        // Lưu ý: chỉ truyền TÊN model trần vào path-variable; nếu nhét cả tiền tố
        // "models/" thì dấu '/' bị URL-encode thành %2F → 404. Phần "/models/"
        // phải nằm cố định trong template.
        Map<String, Object> response = restClient.post()
                .uri("/models/{model}:batchEmbedContents", model)
                .header("x-goog-api-key", apiKey)
                .body(Map.of("requests", requests))
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("embeddings") == null) {
            log.warn("Gemini embedding: empty response for batch of {}", texts.size());
            return List.of();
        }
        List<Map<String, Object>> embeddings = (List<Map<String, Object>>) response.get("embeddings");
        List<float[]> result = new ArrayList<>(embeddings.size());
        for (Map<String, Object> emb : embeddings) {
            List<Number> values = (List<Number>) emb.get("values");
            float[] vec = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vec[i] = values.get(i).floatValue();
            }
            result.add(vec);
        }
        return result;
    }
}
