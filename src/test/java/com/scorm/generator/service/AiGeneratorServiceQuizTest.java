package com.scorm.generator.service;

import com.scorm.generator.dto.ai.AiQuizResponse;
import com.scorm.generator.dto.ai.GenerateCourseQuizRequest;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.service.ai.PageContentExampleLoader;
import com.scorm.generator.service.ai.QuizExampleLoader;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Branch tests for {@link AiGeneratorService#generateCourseAwareQuiz} — the
 * grounding redesign where the user input is a FOCUS TOPIC and the course's
 * source document is the fact source.
 *
 * <p>Mocks the Spring AI {@link ChatClient} fluent chain so no real LLM call is
 * made. The {@code .user(...)} lambda is replayed against a recording mock to
 * recover which prompt template was used. The canned JSON's citation quote is a
 * substring of the source passed in, so
 * {@link com.scorm.generator.service.ai.QuizSchemaValidator} reports no issue and
 * the single-shot path (no retry) is exercised.
 */
class AiGeneratorServiceQuizTest {

    /** Unique marker present ONLY in QuizPrompts.FROM_DOCUMENT_FOCUSED. */
    private static final String FOCUSED_MARKER = "CHỦ ĐỀ TRỌNG TÂM";

    /** Holds the service plus the prompt templates captured per LLM invocation. */
    private record Harness(AiGeneratorService service, List<String> capturedPrompts) {
    }

    @SuppressWarnings("unchecked")
    private Harness newService(String cannedJson) {
        List<String> capturedPrompts = new ArrayList<>();

        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);

        ChatClient.ChatClientRequestSpec reqSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(reqSpec);
        when(reqSpec.options(any())).thenReturn(reqSpec);
        when(reqSpec.user(any(Consumer.class))).thenAnswer(invocation -> {
            Consumer<ChatClient.PromptUserSpec> userLambda = invocation.getArgument(0);
            ChatClient.PromptUserSpec recording = mock(ChatClient.PromptUserSpec.class);
            when(recording.param(anyString(), any())).thenReturn(recording);
            when(recording.text(anyString())).thenAnswer(t -> {
                capturedPrompts.add(t.getArgument(0));
                return recording;
            });
            userLambda.accept(recording);
            return reqSpec;
        });
        when(reqSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(cannedJson);

        ObjectProvider<MeterRegistry> meterProvider = mock(ObjectProvider.class);
        when(meterProvider.getIfAvailable(any())).thenReturn(new SimpleMeterRegistry());

        AiGeneratorService service = new AiGeneratorService(
                builder,
                mock(QuizExampleLoader.class),
                mock(PageContentExampleLoader.class),
                false, // quiz few-shot disabled → loader not touched
                false, // page-content few-shot disabled
                0.3,
                meterProvider);
        return new Harness(service, capturedPrompts);
    }

    /** A single well-formed MCQ whose verbatimQuote is {@code quote}. */
    private static String validQuizJson(String quote) {
        return "{\"questions\":[{"
                + "\"type\":\"MCQ_SINGLE\","
                + "\"prompt\":\"Câu hỏi?\","
                + "\"options\":[\"A\",\"B\"],"
                + "\"correctAnswer\":\"A\","
                + "\"citation\":{\"sourceLocation\":\"Đoạn 1\","
                + "\"verbatimQuote\":\"" + quote + "\","
                + "\"reasoning\":\"Giải thích ngắn.\"},"
                + "\"sentenceHtml\":null,"
                + "\"pairs\":null,"
                + "\"bloomLevel\":3"
                + "}]}";
    }

    @Test
    void bothDocumentAndFocusEmpty_throwsBadRequestWithoutCallingLlm() {
        Harness h = newService(validQuizJson("không dùng"));
        GenerateCourseQuizRequest req = new GenerateCourseQuizRequest();
        req.setSourceText(null);
        req.setFocusTopic("   ");

        AppException ex = assertThrows(AppException.class, () -> h.service().generateCourseAwareQuiz(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(h.capturedPrompts().isEmpty(), "LLM không được gọi khi không có nguồn");
    }

    @Test
    void documentAndFocus_usesFocusedDocumentPrompt() {
        String document = "Binary Search has time complexity O(log n) on sorted arrays.";
        Harness h = newService(validQuizJson("binary search has time complexity"));
        GenerateCourseQuizRequest req = new GenerateCourseQuizRequest();
        req.setSourceText(document); // controller nạp tài liệu gốc vào đây
        req.setFocusTopic("độ phức tạp thời gian");

        AiQuizResponse resp = h.service().generateCourseAwareQuiz(req);

        assertNotNull(resp);
        assertEquals(1, h.capturedPrompts().size(), "JSON hợp lệ → không retry");
        assertTrue(h.capturedPrompts().get(0).contains(FOCUSED_MARKER),
                "có tài liệu + có focus phải dùng FROM_DOCUMENT_FOCUSED");
    }

    @Test
    void noDocument_fallsBackToUserInputAsSource() {
        String focus = "Thuật toán tìm kiếm nhị phân hoạt động trên mảng đã sắp xếp.";
        Harness h = newService(validQuizJson("thuật toán tìm kiếm nhị phân"));
        GenerateCourseQuizRequest req = new GenerateCourseQuizRequest();
        req.setSourceText(null); // khóa học không có tài liệu gốc
        req.setFocusTopic(focus);

        AiQuizResponse resp = h.service().generateCourseAwareQuiz(req);

        assertNotNull(resp);
        String prompt = h.capturedPrompts().get(0);
        assertFalse(prompt.contains(FOCUSED_MARKER), "fallback không được dùng FROM_DOCUMENT_FOCUSED");
        assertTrue(prompt.contains("TÀI LIỆU GỐC"), "fallback dùng FROM_TEXT (ô nhập làm nguồn)");
    }

    @Test
    void documentWithoutFocus_usesGeneralTextPrompt() {
        String document = "Binary Search has time complexity O(log n) on sorted arrays.";
        Harness h = newService(validQuizJson("binary search has time complexity"));
        GenerateCourseQuizRequest req = new GenerateCourseQuizRequest();
        req.setSourceText(document);
        req.setFocusTopic(null); // không có chủ đề trọng tâm → ra đề tổng quát trên tài liệu

        AiQuizResponse resp = h.service().generateCourseAwareQuiz(req);

        assertNotNull(resp);
        assertFalse(h.capturedPrompts().get(0).contains(FOCUSED_MARKER),
                "không có focus → dùng FROM_TEXT tổng quát, không phải FROM_DOCUMENT_FOCUSED");
    }
}
