package com.scorm.generator.service.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Cắt tài liệu nguồn thành các đoạn (chunk) chồng lấp bằng cửa sổ trượt theo ký
 * tự. Cố gắng cắt tại ranh giới câu/đoạn gần nhất để chunk không bị đứt giữa
 * câu, giúp embedding mang ngữ nghĩa trọn vẹn hơn.
 *
 * <p>Chủ ý dùng thuật toán theo ký tự (không phụ thuộc tokenizer ngoài) để dễ
 * giải thích và không thêm dependency — đủ tốt cho tài liệu giáo trình tiếng
 * Việt/Anh ở quy mô của hệ thống.
 */
public final class DocumentChunker {

    private DocumentChunker() {
    }

    /**
     * @param text    văn bản nguồn (đã trích xuất từ file)
     * @param size    kích thước tối đa mỗi chunk (ký tự)
     * @param overlap số ký tự chồng lấp giữa hai chunk liền kề (giữ ngữ cảnh)
     * @return danh sách chunk theo thứ tự xuất hiện; rỗng nếu text trống
     */
    public static List<String> chunk(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null) {
            return chunks;
        }
        String normalized = text.strip();
        if (normalized.isEmpty()) {
            return chunks;
        }
        if (size <= 0) {
            size = 1200;
        }
        // overlap hợp lệ phải nhỏ hơn size, nếu không cửa sổ sẽ không tiến.
        if (overlap < 0 || overlap >= size) {
            overlap = Math.min(Math.max(0, size / 6), size - 1);
        }

        int length = normalized.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + size, length);
            // Nếu chưa tới cuối văn bản, lùi end về ranh giới câu/khoảng trắng gần nhất
            // để tránh cắt giữa câu.
            if (end < length) {
                int boundary = findBackwardBoundary(normalized, start, end);
                if (boundary > start) {
                    end = boundary;
                }
            }
            String piece = normalized.substring(start, end).strip();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= length) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    /**
     * Tìm vị trí cắt "đẹp" trong khoảng (start, end]: ưu tiên ngay sau dấu kết
     * thúc câu/xuống dòng, sau đó tới khoảng trắng. Chỉ chấp nhận nếu nằm trong
     * khoảng 30% cuối của cửa sổ để không tạo chunk quá ngắn.
     */
    private static int findBackwardBoundary(String text, int start, int end) {
        int minAcceptable = start + (int) ((end - start) * 0.7);
        for (int i = end - 1; i > minAcceptable; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '.' || c == '!' || c == '?' || c == '。') {
                return i + 1;
            }
        }
        for (int i = end - 1; i > minAcceptable; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        return end;
    }
}
