package com.scorm.generator.service.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests cho thuật toán cắt chunk (cửa sổ trượt + cắt tại ranh giới câu).
 * Không phụ thuộc Spring/DB/embedding — chạy thuần.
 */
class DocumentChunkerTest {

    @Test
    void nullOrBlank_returnsEmpty() {
        assertThat(DocumentChunker.chunk(null, 100, 20)).isEmpty();
        assertThat(DocumentChunker.chunk("", 100, 20)).isEmpty();
        assertThat(DocumentChunker.chunk("   \n  ", 100, 20)).isEmpty();
    }

    @Test
    void shortText_returnsSingleChunk() {
        String text = "Một đoạn ngắn gọn.";
        List<String> chunks = DocumentChunker.chunk(text, 100, 20);
        assertThat(chunks).containsExactly(text);
    }

    @Test
    void longText_splitsIntoMultipleChunks() {
        String sentence = "Đây là một câu mẫu dùng để kiểm thử việc cắt đoạn. ";
        String text = sentence.repeat(50); // ~2500 ký tự
        List<String> chunks = DocumentChunker.chunk(text, 500, 100);
        assertThat(chunks).hasSizeGreaterThan(1);
    }

    @Test
    void everyChunk_respectsMaxSizeRoughly() {
        String text = "abcdefghij".repeat(300); // 3000 ký tự, không có ranh giới câu
        int size = 400;
        List<String> chunks = DocumentChunker.chunk(text, size, 50);
        // Cho phép vượt nhẹ vì strip()/boundary, nhưng không được phình quá mức.
        assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(size + 10));
    }

    @Test
    void consecutiveChunks_overlap() {
        String text = "abcdefghijklmnopqrstuvwxyz".repeat(40); // 1040 ký tự liền, không có khoảng trắng
        int size = 300;
        int overlap = 100;
        List<String> chunks = DocumentChunker.chunk(text, size, overlap);
        assertThat(chunks).hasSizeGreaterThan(1);
        // Đuôi của chunk[0] phải xuất hiện ở đầu chunk[1] (do overlap).
        String tailOfFirst = chunks.get(0).substring(chunks.get(0).length() - overlap);
        assertThat(chunks.get(1)).startsWith(tailOfFirst);
    }

    @Test
    void cutsAtSentenceBoundary_whenAvailable() {
        // Câu đầu kết thúc (dấu chấm tại index 36) nằm trong vùng cắt "đẹp" — 30%
        // cuối của cửa sổ size=45 (ngưỡng ~31.5) — nên chunker phải cắt tại đó.
        String first = "Câu đầu kết thúc tại vị trí gần cuối."; // dấu '.' ở index 36
        String text = first + " Câu thứ hai tiếp tục thêm cho đủ dài hơn để tách chunk.";
        List<String> chunks = DocumentChunker.chunk(text, 45, 8);
        assertThat(chunks).hasSizeGreaterThan(1);
        // Chunk đầu kết thúc bằng dấu chấm (cắt tại ranh giới câu), không đứt giữa từ.
        assertThat(chunks.get(0)).endsWith(".");
    }

    @Test
    void invalidOverlap_isSanitized_andStillTerminates() {
        // overlap >= size là không hợp lệ; chunker phải tự điều chỉnh và không treo.
        String text = "x".repeat(1000);
        List<String> chunks = DocumentChunker.chunk(text, 200, 500);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks).hasSizeGreaterThan(1);
    }
}
