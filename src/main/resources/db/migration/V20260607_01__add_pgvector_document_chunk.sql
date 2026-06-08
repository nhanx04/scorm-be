-- RAG (Retrieval-Augmented Generation) storage.
--
-- Tài liệu nguồn của khóa học (course.source_document_text) được cắt thành các
-- đoạn nhỏ (chunk), nhúng thành vector bằng embedding model của Google GenAI, và
-- lưu ở đây. Khi sinh nội dung bài học hoặc quiz có chủ đề trọng tâm, hệ thống
-- truy xuất top-k chunk liên quan thay vì nhồi toàn bộ tài liệu vào prompt.
--
-- Bảng này KHÔNG có JPA entity (Hibernate ddl-auto=validate không hiểu kiểu
-- `vector`); mọi thao tác đi qua JdbcTemplate trong DocumentChunkDao.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS document_chunk (
    id           BIGSERIAL PRIMARY KEY,
    course_id    BIGINT NOT NULL REFERENCES course(courseid) ON DELETE CASCADE,
    chunk_index  INTEGER NOT NULL,
    content      TEXT NOT NULL,
    -- 768 chiều khớp với text-embedding-004. Nếu đổi embedding model sang số
    -- chiều khác, phải tạo migration mới đổi kiểu cột này cho khớp.
    embedding    vector(768) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

-- Lọc nhanh theo khóa học (mọi truy vấn RAG đều scope theo course_id) và dọn chunk cũ khi re-ingest.
CREATE INDEX IF NOT EXISTS idx_document_chunk_course ON document_chunk (course_id);

-- ANN index cho similarity search bằng cosine distance (toán tử <=>).
CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding
    ON document_chunk USING hnsw (embedding vector_cosine_ops);
