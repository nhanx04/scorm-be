package com.scorm.generator.service.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
 * đúng các bước chunk → embed → store / embed-query → search. EmbeddingModel và
 * DAO đều được mock; không cần Spring context hay DB thật.
 */
class CourseRagServiceTest {

    private static final long COURSE_ID = 42L;
    private static final float[] VEC = {0.1f, 0.2f, 0.3f};

    private CourseRagService newService(DocumentChunkDao dao, EmbeddingModel model, boolean enabled) {
        ObjectProvider<EmbeddingModel> provider = mockProvider(model);
        return new CourseRagService(dao, provider, enabled, 200, 40, 5);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<EmbeddingModel> mockProvider(EmbeddingModel model) {
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(model);
        return provider;
    }

    // --- isEnabled / fail-soft ------------------------------------------------

    @Test
    void disabled_retrieveReturnsEmpty_andTouchesNothing() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        EmbeddingModel model = mock(EmbeddingModel.class);
        CourseRagService svc = newService(dao, model, false);

        assertThat(svc.isEnabled()).isFalse();
        assertThat(svc.retrieveContext(COURSE_ID, "anything", null)).isEmpty();
        assertThat(svc.hasIndex(COURSE_ID)).isFalse();
        verifyNoInteractions(dao, model);
    }

    @Test
    void noEmbeddingModel_isDisabled() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        CourseRagService svc = newService(dao, null, true);

        assertThat(svc.isEnabled()).isFalse();
        assertThat(svc.retrieveContext(COURSE_ID, "q", null)).isEmpty();
        verifyNoInteractions(dao);
    }

    // --- ingest ---------------------------------------------------------------

    @Test
    void ingest_blankText_isNoOp() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        EmbeddingModel model = mock(EmbeddingModel.class);
        CourseRagService svc = newService(dao, model, true);

        svc.ingest(COURSE_ID, "   ");

        verifyNoInteractions(dao, model);
    }

    @Test
    void ingest_happyPath_deletesOldThenInsertsEmbeddedChunks() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        EmbeddingModel model = mock(EmbeddingModel.class);
        // Trả về 1 vector cho mỗi văn bản trong batch để khớp số lượng chunk.
        when(model.embed(anyList())).thenAnswer(inv -> {
            List<String> batch = inv.getArgument(0);
            List<float[]> out = new ArrayList<>();
            for (int i = 0; i < batch.size(); i++) {
                out.add(VEC);
            }
            return out;
        });
        CourseRagService svc = newService(dao, model, true);

        String longText = "Đây là một câu kiểm thử dùng để cắt đoạn. ".repeat(40);
        svc.ingest(COURSE_ID, longText);

        verify(dao).deleteByCourseId(COURSE_ID);
        verify(dao).insertBatch(eq(COURSE_ID), anyList(), anyList());
    }

    // --- retrieve -------------------------------------------------------------

    @Test
    void retrieveContext_joinsRetrievedPassages() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embed(anyString())).thenReturn(VEC);
        when(dao.searchSimilar(eq(COURSE_ID), any(float[].class), eq(5)))
                .thenReturn(List.of("đoạn A", "đoạn B"));
        CourseRagService svc = newService(dao, model, true);

        String ctx = svc.retrieveContext(COURSE_ID, "chủ đề", null);

        assertThat(ctx).contains("đoạn A").contains("đoạn B");
        assertThat(ctx).isEqualTo("đoạn A\n\n---\n\nđoạn B");
    }

    @Test
    void retrieve_blankQuery_returnsEmpty_withoutEmbedding() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        EmbeddingModel model = mock(EmbeddingModel.class);
        CourseRagService svc = newService(dao, model, true);

        assertThat(svc.retrievePassages(COURSE_ID, "  ", null)).isEmpty();
        verify(model, never()).embed(anyString());
        verifyNoInteractions(dao);
    }

    @Test
    void retrieve_daoThrows_isFailSoft_returnsEmpty() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embed(anyString())).thenReturn(VEC);
        when(dao.searchSimilar(anyLong(), any(float[].class), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new RuntimeException("pgvector down"));
        CourseRagService svc = newService(dao, model, true);

        assertThat(svc.retrievePassages(COURSE_ID, "q", null)).isEmpty();
        assertThat(svc.retrieveContext(COURSE_ID, "q", null)).isEmpty();
    }

    @Test
    void customTopK_isPassedThrough() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embed(anyString())).thenReturn(VEC);
        when(dao.searchSimilar(eq(COURSE_ID), any(float[].class), eq(3)))
                .thenReturn(List.of("x"));
        CourseRagService svc = newService(dao, model, true);

        assertThat(svc.retrievePassages(COURSE_ID, "q", 3)).containsExactly("x");
        verify(dao).searchSimilar(eq(COURSE_ID), any(float[].class), eq(3));
    }

    // --- hasIndex -------------------------------------------------------------

    @Test
    void hasIndex_reflectsChunkCount() {
        DocumentChunkDao dao = mock(DocumentChunkDao.class);
        EmbeddingModel model = mock(EmbeddingModel.class);
        CourseRagService svc = newService(dao, model, true);

        when(dao.countByCourseId(COURSE_ID)).thenReturn(7);
        assertThat(svc.hasIndex(COURSE_ID)).isTrue();

        when(dao.countByCourseId(COURSE_ID)).thenReturn(0);
        assertThat(svc.hasIndex(COURSE_ID)).isFalse();
    }
}
