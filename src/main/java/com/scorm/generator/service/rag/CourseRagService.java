package com.scorm.generator.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Tầng RAG của khóa học: nạp (ingest) tài liệu nguồn thành vector và truy xuất
 * (retrieve) các đoạn liên quan để làm ngữ cảnh cho việc sinh nội dung/quiz.
 *
 * <p>Toàn bộ thiết kế "fail-soft": nếu RAG bị tắt, không có embedding model, hoặc
 * khóa học chưa được index, các phương thức trả về kết quả rỗng để caller tự
 * fallback về cách nhồi nguyên tài liệu (hành vi cũ). Không bao giờ ném lỗi làm
 * hỏng luồng tạo khóa học.
 */
@Service
public class CourseRagService {

    private static final Logger log = LoggerFactory.getLogger(CourseRagService.class);

    /** Google giới hạn số lượng văn bản mỗi request embedding; chia batch cho an toàn. */
    private static final int EMBED_BATCH_SIZE = 50;

    private final DocumentChunkDao chunkDao;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final boolean ragEnabled;
    private final int chunkSize;
    private final int chunkOverlap;
    private final int defaultTopK;

    public CourseRagService(
            DocumentChunkDao chunkDao,
            ObjectProvider<EmbeddingModel> embeddingModelProvider,
            @Value("${app.ai.rag.enabled:true}") boolean ragEnabled,
            @Value("${app.ai.rag.chunk-size:1200}") int chunkSize,
            @Value("${app.ai.rag.chunk-overlap:200}") int chunkOverlap,
            @Value("${app.ai.rag.top-k:6}") int defaultTopK) {
        this.chunkDao = chunkDao;
        this.embeddingModelProvider = embeddingModelProvider;
        this.ragEnabled = ragEnabled;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.defaultTopK = defaultTopK;
    }

    public boolean isEnabled() {
        return ragEnabled && embeddingModelProvider.getIfAvailable() != null;
    }

    /** Khóa học đã có chunk index → có thể truy xuất RAG. */
    public boolean hasIndex(Long courseId) {
        if (!isEnabled() || courseId == null) {
            return false;
        }
        try {
            return chunkDao.countByCourseId(courseId) > 0;
        } catch (Exception e) {
            log.warn("RAG: countByCourseId failed for course {}: {}", courseId, e.getMessage());
            return false;
        }
    }

    /**
     * Index lại tài liệu nguồn của một khóa học: cắt chunk → embedding → ghi DB
     * (xóa chunk cũ trước). Chạy bất đồng bộ để không chặn response lưu khóa học.
     * Mọi lỗi chỉ được log; khóa học vẫn dùng được (fallback nhồi tài liệu).
     */
    @Async
    public void ingestAsync(Long courseId, String sourceText) {
        ingest(courseId, sourceText);
    }

    /** Phiên bản đồng bộ của {@link #ingestAsync} — tách ra để test/tái sử dụng. */
    public void ingest(Long courseId, String sourceText) {
        if (!isEnabled() || courseId == null || sourceText == null || sourceText.isBlank()) {
            return;
        }
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            return;
        }
        try {
            List<String> chunks = DocumentChunker.chunk(sourceText, chunkSize, chunkOverlap);
            if (chunks.isEmpty()) {
                return;
            }
            List<float[]> embeddings = embedInBatches(embeddingModel, chunks);

            chunkDao.deleteByCourseId(courseId);
            chunkDao.insertBatch(courseId, chunks, embeddings);
            log.info("RAG: indexed {} chunk(s) for course {}", chunks.size(), courseId);
        } catch (Exception e) {
            log.warn("RAG: ingest failed for course {} — falling back to full-document grounding. Cause: {}",
                    courseId, e.getMessage());
        }
    }

    /**
     * Truy xuất các đoạn tài liệu liên quan nhất tới {@code query} trong phạm vi
     * khóa học, ghép lại thành một khối văn bản dùng làm ngữ cảnh (grounding).
     *
     * @return văn bản ngữ cảnh đã ghép, hoặc chuỗi rỗng nếu không truy xuất được
     *         (caller nên fallback về nhồi nguyên tài liệu).
     */
    public String retrieveContext(Long courseId, String query, Integer topK) {
        List<String> passages = retrievePassages(courseId, query, topK);
        if (passages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < passages.size(); i++) {
            if (i > 0) {
                sb.append("\n\n---\n\n");
            }
            sb.append(passages.get(i));
        }
        return sb.toString();
    }

    /** Như {@link #retrieveContext} nhưng trả về từng đoạn riêng lẻ. */
    public List<String> retrievePassages(Long courseId, String query, Integer topK) {
        if (!isEnabled() || courseId == null || query == null || query.isBlank()) {
            return List.of();
        }
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            return List.of();
        }
        int k = (topK != null && topK > 0) ? topK : defaultTopK;
        try {
            float[] queryEmbedding = embeddingModel.embed(query);
            return chunkDao.searchSimilar(courseId, queryEmbedding, k);
        } catch (Exception e) {
            log.warn("RAG: retrieve failed for course {}: {}", courseId, e.getMessage());
            return List.of();
        }
    }

    private List<float[]> embedInBatches(EmbeddingModel embeddingModel, List<String> chunks) {
        List<float[]> all = new ArrayList<>(chunks.size());
        for (int from = 0; from < chunks.size(); from += EMBED_BATCH_SIZE) {
            int to = Math.min(from + EMBED_BATCH_SIZE, chunks.size());
            List<String> batch = chunks.subList(from, to);
            all.addAll(embeddingModel.embed(batch));
        }
        return all;
    }
}
