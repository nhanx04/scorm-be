package com.scorm.generator.service.rag;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Truy cập bảng {@code document_chunk} (pgvector) bằng JdbcTemplate.
 *
 * <p>Không dùng JPA entity vì Hibernate chạy {@code ddl-auto=validate} và không
 * hiểu kiểu cột {@code vector}. Vector được truyền/đọc dưới dạng literal
 * {@code "[v1,v2,...]"} và ép kiểu {@code ::vector} ngay trong câu SQL.
 */
@Repository
public class DocumentChunkDao {

    private final JdbcTemplate jdbc;

    public DocumentChunkDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Xóa toàn bộ chunk của một khóa học (dùng trước khi re-ingest). */
    @Transactional
    public int deleteByCourseId(Long courseId) {
        return jdbc.update("DELETE FROM document_chunk WHERE course_id = ?", courseId);
    }

    /** Số chunk đã index cho khóa học — dùng để quyết định có thể truy xuất RAG hay không. */
    public int countByCourseId(Long courseId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM document_chunk WHERE course_id = ?", Integer.class, courseId);
        return count != null ? count : 0;
    }

    /** Chèn một batch chunk cùng vector embedding cho một khóa học. */
    @Transactional
    public void insertBatch(Long courseId, List<String> contents, List<float[]> embeddings) {
        if (contents.size() != embeddings.size()) {
            throw new IllegalArgumentException("contents and embeddings size mismatch");
        }
        jdbc.batchUpdate(
                "INSERT INTO document_chunk (course_id, chunk_index, content, embedding) "
                        + "VALUES (?, ?, ?, ?::vector)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setLong(1, courseId);
                        ps.setInt(2, i);
                        ps.setString(3, contents.get(i));
                        ps.setString(4, toVectorLiteral(embeddings.get(i)));
                    }

                    @Override
                    public int getBatchSize() {
                        return contents.size();
                    }
                });
    }

    /**
     * Truy xuất top-k chunk gần nhất với vector truy vấn (cosine distance) trong
     * phạm vi một khóa học. Trả về nội dung chunk theo thứ tự liên quan giảm dần.
     */
    public List<String> searchSimilar(Long courseId, float[] queryEmbedding, int topK) {
        String vec = toVectorLiteral(queryEmbedding);
        return jdbc.query(
                "SELECT content FROM document_chunk "
                        + "WHERE course_id = ? "
                        + "ORDER BY embedding <=> ?::vector "
                        + "LIMIT ?",
                (rs, rowNum) -> rs.getString("content"),
                courseId, vec, topK);
    }

    /** Chuyển float[] thành literal pgvector: {@code [0.1,0.2,...]}. */
    static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
