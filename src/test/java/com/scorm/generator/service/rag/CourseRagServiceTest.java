package com.scorm.generator.service.rag;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests cho CourseRagService — tập trung vào hành vi fail-soft và việc nối
 * đúng các bước chunk → embed → store / embed-query → search. GeminiEmbeddingClient
 * và DAO đều được mock; không cần Spring context, DB hay gọi API thật.
 */
class CourseRagServiceTest {

    private static final long COURSE_ID = 42L;
    private static final float[] VEC = {0.1f, 0.2f, 0.3f};

    /** Tạo service với client đã (hoặc chưa) cấu hình + cờ bật/tắt RAG. */
    private CourseRagService newService(DocumentChunkDao dao, GeminiEmbeddingClient client,
            boolean clientConfigured, boolean ragEnabled) {
        when(client.isConfigured()).thenReturn(clientConfigured);
        return new CourseRagService(dao, client, ragEnabled, 200, 40, 5);
    }

    // --- isEnabled / fail-soft ------------------------------------------------

    @Test
    void ragDisabledByFlag_retrieveReturnsEmpty_andTouchesNothing() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        GeminiEmbeddingClient client = mock(GeminiEmbeddingClient.class);
        CourseRagService svc = newService(dao, client, true, false);

        assertThat(svc.isEnabled()).isFalse();
        assertThat(svc.retrieveContext(COURSE_ID, "anything", null)).isEmpty();
        assertThat(svc.hasIndex(COURSE_ID)).isFalse();
        verifyNoInteractions(dao);
    }

    @Test
    void clientNotConfigured_isDisabled() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        GeminiEmbeddingClient client = mock(GeminiEmbeddingClient.class);
        CourseRagService svc = newService(dao, client, false, true);

        assertThat(svc.isEnabled()).isFalse();
        assertThat(svc.retrieveContext(COURSE_ID, "q", null)).isEmpty();
        verifyNoInteractions(dao);
    }

    // --- ingest ---------------------------------------------------------------

    @Test
    void ingest_blankText_isNoOp() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        GeminiEmbeddingClient client = mock(GeminiEmbeddingClient.class);
        CourseRagService svc = newService(dao, client, true, true);

        svc.ingest(COURSE_ID, "   ");

        verifyNoInteractions(dao);
    }

    @Test
    void ingest_happyPath_deletesOldThenInsertsEmbeddedChunks() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        GeminiEmbeddingClient client = mock(GeminiEmbeddingClient.class);
        // Trả về 1 vector cho mỗi chunk để khớp số lượng.
        when(client.embedDocuments(anyList())).thenAnswer(inv -> {
            List<String> batch = inv.getArgument(0);
            List<float[]> out = new ArrayList<>();
            for (int i = 0; i < batch.size(); i++) {
                out.add(VEC);
            }
            return out;
        });
        CourseRagService svc = newService(dao, client, true, true);

        String longText = "Đây là một câu kiểm thử dùng để cắt đoạn. ".repeat(40);
        svc.ingest(COURSE_ID, longText);

        verify(dao).deleteByCourseId(COURSE_ID);
        verify(dao).insertBatch(eq(COURSE_ID), anyList(), anyList());
    }

    @Test
    void ingest_embeddingCountMismatch_skipsInsert() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        GeminiEmbeddingClient client = mock(GeminiEmbeddingClient.class);
        // Trả về ít vector hơn số chunk → phải bỏ qua, không ghi DB hỏng.
        when(client.embedDocuments(anyList())).thenReturn(List.of(VEC));
        CourseRagService svc = newService(dao, client, true, true);

        svc.ingest(COURSE_ID, "Câu một. ".repeat(60));

        verify(dao, never()).deleteByCourseId(anyLong());
        verify(dao, never()).insertBatch(anyLong(), anyList(), anyList());
    }

    // --- retrieve -------------------------------------------------------------

    @Test
    void retrieveContext_joinsRetrievedPassages() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        GeminiEmbeddingClient client = mock(GeminiEmbeddingClient.class);
        when(client.embedQuery(anyString())).thenReturn(VEC);
        when(dao.searchSimilar(eq(COURSE_ID), any(float[].class), eq(5)))
                .thenReturn(List.of("đoạn A", "đoạn B"));
        CourseRagService svc = newService(dao, client, true, true);

        String ctx = svc.retrieveContext(COURSE_ID, "chủ đề", null);

        assertThat(ctx).isEqualTo("đoạn A\n\n---\n\nđoạn B");
    }

    @Test
    void retrieve_blankQuery_returnsEmpty_withoutEmbedding() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        GeminiEmbeddingClient client = mock(GeminiEmbeddingClient.class);
        CourseRagService svc = newService(dao, client, true, true);

        assertThat(svc.retrievePassages(COURSE_ID, "  ", null)).isEmpty();
        verify(client, never()).embedQuery(anyString());
        verifyNoInteractions(dao);
    }

    @Test
    void retrieve_nullQueryEmbedding_returnsEmpty() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        GeminiEmbeddingClient client = mock(GeminiEmbeddingClient.class);
        when(client.embedQuery(anyString())).thenReturn(null);
        CourseRagService svc = newService(dao, client, true, true);

        assertThat(svc.retrievePassages(COURSE_ID, "q", null)).isEmpty();
        verifyNoInteractions(dao);
    }

    @Test
    void retrieve_daoThrows_isFailSoft_returnsEmpty() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        GeminiEmbeddingClient client = mock(GeminiEmbeddingClient.class);
        when(client.embedQuery(anyString())).thenReturn(VEC);
        when(dao.searchSimilar(anyLong(), any(float[].class), anyInt()))
                .thenThrow(new RuntimeException("pgvector down"));
        CourseRagService svc = newService(dao, client, true, true);

        assertThat(svc.retrievePassages(COURSE_ID, "q", null)).isEmpty();
        assertThat(svc.retrieveContext(COURSE_ID, "q", null)).isEmpty();
    }

    @Test
    void customTopK_isPassedThrough() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        GeminiEmbeddingClient client = mock(GeminiEmbeddingClient.class);
        when(client.embedQuery(anyString())).thenReturn(VEC);
        when(dao.searchSimilar(eq(COURSE_ID), any(float[].class), eq(3)))
                .thenReturn(List.of("x"));
        CourseRagService svc = newService(dao, client, true, true);

        assertThat(svc.retrievePassages(COURSE_ID, "q", 3)).containsExactly("x");
        verify(dao).searchSimilar(eq(COURSE_ID), any(float[].class), eq(3));
    }

    // --- hasIndex -------------------------------------------------------------

    @Test
    void hasIndex_reflectsChunkCount() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        GeminiEmbeddingClient client = mock(GeminiEmbeddingClient.class);
        CourseRagService svc = newService(dao, client, true, true);

        when(dao.countByCourseId(COURSE_ID)).thenReturn(7);
        assertThat(svc.hasIndex(COURSE_ID)).isTrue();

        when(dao.countByCourseId(COURSE_ID)).thenReturn(0);
        assertThat(svc.hasIndex(COURSE_ID)).isFalse();
    }
}
