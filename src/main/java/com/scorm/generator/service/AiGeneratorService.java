package com.scorm.generator.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.http.HttpStatus;

import com.scorm.generator.exception.AppException;
import com.scorm.generator.dto.ai.AiCourseOutline;
import com.scorm.generator.dto.ai.GenerateCourseRequest;
import com.scorm.generator.dto.ai.AiPageContentResponse;
import com.scorm.generator.dto.ai.GeneratePageContentRequest;
import com.scorm.generator.dto.ai.AiKnowledgeAnswerResponse;
import com.scorm.generator.dto.ai.AiQuizResponse;
import com.scorm.generator.dto.ai.AskKnowledgeRequest;
import com.scorm.generator.dto.ai.GenerateCourseQuizRequest;
import com.scorm.generator.dto.ai.GenerateQuizRequest;
import com.scorm.generator.service.ai.PageContentExampleLoader;
import com.scorm.generator.service.ai.QuizExampleLoader;
import com.scorm.generator.service.ai.QuizSchemaValidator;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiGeneratorService {

        private static final Logger log = LoggerFactory.getLogger(AiGeneratorService.class);

        /**
         * Rough heuristic for the smallest source text that can plausibly yield N
         * faithful quiz questions without forcing the model to hallucinate.
         * 4 chars/token × 200 tokens/question ≈ 800 chars per question.
         */
        private static final int MIN_CHARS_PER_QUIZ_QUESTION = 800;

        private final ChatClient chatClient;
        private final QuizExampleLoader quizExampleLoader;
        private final PageContentExampleLoader pageContentExampleLoader;
        private final boolean quizFewShotEnabled;
        private final boolean pageContentFewShotEnabled;
        private final ChatOptions quizOptions;
        private final MeterRegistry meters;

        public AiGeneratorService(
                        ChatClient.Builder chatClientBuilder,
                        QuizExampleLoader quizExampleLoader,
                        PageContentExampleLoader pageContentExampleLoader,
                        @Value("${app.ai.quiz.few-shot-enabled:true}") boolean quizFewShotEnabled,
                        @Value("${app.ai.page-content.few-shot-enabled:true}") boolean pageContentFewShotEnabled,
                        @Value("${app.ai.quiz.temperature:0.3}") double quizTemperature,
                        ObjectProvider<MeterRegistry> meterRegistry) {
                this.chatClient = chatClientBuilder
                                .defaultSystem(
                                                "Bạn là một chuyên gia thiết kế giáo trình e-learning (Instructional Designer) với 10 năm kinh nghiệm. "
                                                                + "Nhiệm vụ của bạn là xây dựng cấu trúc khóa học chi tiết, logic và hấp dẫn.")
                                .build();
                this.quizExampleLoader = quizExampleLoader;
                this.pageContentExampleLoader = pageContentExampleLoader;
                this.quizFewShotEnabled = quizFewShotEnabled;
                this.pageContentFewShotEnabled = pageContentFewShotEnabled;
                // Quiz needs to stick close to the source — lower temperature than
                // the chat-client default (0.7) which is tuned for outline creativity.
                this.quizOptions = ChatOptions.builder().temperature(quizTemperature).build();
                // Fall back to an in-process registry when no actuator/observability
                // stack is wired up — keeps counters working in any environment.
                this.meters = meterRegistry.getIfAvailable(SimpleMeterRegistry::new);
        }

        private static String stripJsonFence(String raw) {
                return raw.replace("```json", "").replace("```", "").trim();
        }

        /**
         * Warn (don't fail) when the source text is likely too short to support
         * the requested question count without hallucination. The prompt already
         * tells the model "tạo ít câu hơn thay vì bịa", so we just observe.
         */
        private static int previewQuizCoverage(String sourceText, int requestedQuestions) {
                int chars = sourceText == null ? 0 : sourceText.length();
                int feasible = Math.max(1, chars / MIN_CHARS_PER_QUIZ_QUESTION);
                if (feasible < requestedQuestions) {
                        log.warn("Quiz preflight: source text {} chars supports ~{} questions"
                                        + " but {} were requested. Model is instructed to scale down"
                                        + " rather than hallucinate.",
                                        chars, feasible, requestedQuestions);
                }
                return feasible;
        }

        /**
         * Trích xuất văn bản từ File (PDF/Word) và tạo Dàn ý khóa học
         */
        public AiCourseOutline generateCourseOutlineFromFile(MultipartFile file, GenerateCourseRequest request)
                        throws Exception {
                // 1. GIAI ĐOẠN ĐỌC FILE VÀ TRÍCH XUẤT TEXT
                InputStream inputStream = file.getInputStream();
                InputStreamResource resource = new InputStreamResource(inputStream);

                // Khởi tạo TikaDocumentReader để đọc nội dung file
                TikaDocumentReader documentReader = new TikaDocumentReader(resource);
                List<Document> documents = documentReader.get();

                // Ghép tất cả văn bản lấy được thành một chuỗi context
                String documentContext = documents.stream()
                                .map(Document::getText)
                                .collect(Collectors.joining("\n"));

                // 2. Cấu hình Converter để hướng dẫn AI trả về đúng định dạng JSON.
                // Dùng Draft (không có sourceDocumentText) để LLM không echo lại tài liệu.
                BeanOutputConverter<AiCourseOutline.Draft> converter = new BeanOutputConverter<>(
                                AiCourseOutline.Draft.class);
                String formatInstructions = converter.getFormat();

                // 3. Soạn Prompt dành riêng cho bài toán bám sát nội dung từ tài liệu
                String userPrompt = """
                                Hãy đọc, phân tích toàn bộ tài liệu chuyên môn dưới đây và biến nó thành một dàn ý khóa học e-learning chi tiết, có cấu trúc tốt.

                                NỘI DUNG TÀI LIỆU GỐC (Dùng làm nguồn kiến thức chính):
                                =================================
                                {documentContext}
                                =================================

                                THÔNG TIN CẤU HÌNH KHÓA HỌC:
                                - Tên khóa học mong muốn (Title): {courseTitle}
                                - Đối tượng học viên (Target Audience): {targetAudience}
                                - Trình độ hiện tại (Proficiency Level): {audienceProficiencyLevel}
                                - Thời lượng dự kiến (Duration): {duration}
                                - Ngôn ngữ đầu ra (Language): {language}
                                - Yêu cầu thêm: {additionalInstructions}

                                YÊU CẦU CHUYÊN MÔN:
                                1. Dựa trên nội dung tài liệu gốc, hãy tự động phân bổ số lượng chương (sections) và các bài học (topics) sao cho bao quát hết kiến thức, logic và khoa học.
                                2. Các ý chính, khái niệm quan trọng trong tài liệu phải được tách thành các bài học riêng biệt.
                                3. Đảm bảo luồng kiến thức đi từ cơ bản đến nâng cao.

                                YÊU CẦU ĐỊNH DẠNG:
                                1. Trả về kết quả CHÍNH XÁC theo định dạng JSON được yêu cầu.
                                2. KHÔNG thêm bất kỳ lời dẫn hay giải thích nào bên ngoài JSON.

                                {formatInstructions}
                                """;

                // 4. Gọi AI và truyền Data vào Prompt
                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("documentContext", documentContext)
                                                .param("courseTitle",
                                                                request.getCourseTitle() != null
                                                                                ? request.getCourseTitle()
                                                                                : "Tự động trích xuất từ tài liệu")
                                                .param("duration",
                                                                request.getDuration() != null ? request.getDuration()
                                                                                : "Tự động phân bổ")
                                                .param("language",
                                                                request.getLanguage() != null ? request.getLanguage()
                                                                                : "Vietnamese")
                                                .param("targetAudience",
                                                                request.getTargetAudience() != null
                                                                                ? request.getTargetAudience()
                                                                                : "Mọi đối tượng")
                                                .param("audienceProficiencyLevel",
                                                                request.getAudienceProficiencyLevel() != null
                                                                                ? request.getAudienceProficiencyLevel()
                                                                                : "Không xác định")
                                                .param("additionalInstructions",
                                                                request.getAdditionalInstructions() != null
                                                                                ? request.getAdditionalInstructions()
                                                                                : "Bám sát tài liệu gốc")
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                // 5. Xử lý chuỗi và Convert thành Object, đính kèm văn bản gốc để persist
                String jsonContent = stripJsonFence(rawResponse);
                AiCourseOutline.Draft draft = converter.convert(jsonContent);
                return new AiCourseOutline(draft.title(), draft.description(), draft.sections(), documentContext);
        }

        /**
         * Gửi yêu cầu sang Gemini và nhận về Object Java (AiCourseOutline)
         */
        public AiCourseOutline generateCourseOutline(GenerateCourseRequest request) {
                // 1. Cấu hình Converter để hướng dẫn AI trả về đúng định dạng JSON của Class
                // Java (dùng Draft — không tạo từ file nên không có văn bản gốc).
                BeanOutputConverter<AiCourseOutline.Draft> converter = new BeanOutputConverter<>(
                                AiCourseOutline.Draft.class);
                String formatInstructions = converter.getFormat();

                // 2. Soạn Prompt (kịch bản nhắc) cập nhật theo cấu trúc mới
                String userPrompt = """
                                Hãy tạo một dàn ý khóa học chi tiết dựa trên các thông tin và ngữ cảnh sau:

                                THÔNG TIN KHÓA HỌC:
                                - Tên khóa học (Title): {courseTitle}
                                - Mô tả tổng quan (Description): {courseDescription}
                                - Thời lượng dự kiến (Duration): {duration}
                                - Ngôn ngữ đầu ra (Language): {language}

                                ĐỐI TƯỢNG & MỤC TIÊU:
                                - Đối tượng học viên (Target Audience): {targetAudience}
                                - Trình độ hiện tại (Proficiency Level): {audienceProficiencyLevel}
                                - Yêu cầu đầu vào (Prerequisites): {prerequisites}
                                - Mục tiêu đầu ra (Learning Outcomes): {learningOutcomes}

                                Yêu cầu thêm: {additionalInstructions}

                                YÊU CẦU CHUYÊN MÔN:
                                Dựa vào thời lượng dự kiến là "{duration}", hãy tự động phân bổ số lượng chương (sections) và nội dung bài học sao cho logic, khoa học và đáp ứng đúng mục tiêu đầu ra. Các chương cần phải liên kết chặt chẽ với nhau.

                                YÊU CẦU QUAN TRỌNG VỀ ĐỊNH DẠNG:
                                1. Trả về kết quả CHÍNH XÁC theo định dạng JSON được yêu cầu dưới đây.
                                2. KHÔNG thêm bất kỳ lời dẫn hay giải thích nào bên ngoài JSON.

                                {formatInstructions}
                                """;

                // 3. Gọi AI và lấy về chuỗi String thô (raw content)
                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("courseTitle",
                                                                request.getCourseTitle() != null
                                                                                ? request.getCourseTitle()
                                                                                : "Chưa xác định")
                                                .param("courseDescription",
                                                                request.getCourseDescription() != null
                                                                                ? request.getCourseDescription()
                                                                                : "Chưa xác định")
                                                .param("duration",
                                                                request.getDuration() != null ? request.getDuration()
                                                                                : "Tự động phân bổ")
                                                .param("language",
                                                                request.getLanguage() != null ? request.getLanguage()
                                                                                : "Vietnamese")
                                                .param("targetAudience",
                                                                request.getTargetAudience() != null
                                                                                ? request.getTargetAudience()
                                                                                : "Chưa xác định")
                                                .param("audienceProficiencyLevel",
                                                                request.getAudienceProficiencyLevel() != null
                                                                                ? request.getAudienceProficiencyLevel()
                                                                                : "Chưa xác định")
                                                .param("prerequisites",
                                                                request.getPrerequisites() != null
                                                                                ? request.getPrerequisites()
                                                                                : "Không có")
                                                .param("learningOutcomes",
                                                                request.getLearningOutcomes() != null
                                                                                ? request.getLearningOutcomes()
                                                                                : "Nắm vững kiến thức cơ bản")
                                                .param("additionalInstructions",
                                                                request.getAdditionalInstructions() != null
                                                                                ? request.getAdditionalInstructions()
                                                                                : "Không có")
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                // 4. Xử lý chuỗi JSON (Làm sạch Markdown nếu có)
                String jsonContent = stripJsonFence(rawResponse);

                // 5. Convert String sạch thành Object Java (không có văn bản gốc từ file)
                AiCourseOutline.Draft draft = converter.convert(jsonContent);
                return new AiCourseOutline(draft.title(), draft.description(), draft.sections(), null);
        }

        /**
         * API sinh nội dung chi tiết cho trang bài học (Page Content Generation)
         */
        public AiPageContentResponse generatePageContent(GeneratePageContentRequest request) {
                // 1. Cấu hình Converter
                BeanOutputConverter<AiPageContentResponse> converter = new BeanOutputConverter<>(
                                AiPageContentResponse.class);
                String formatInstructions = converter.getFormat();

                // 2. Soạn Prompt
                String userPrompt = """
                                Bạn là một chuyên gia thiết kế nội dung e-learning (Instructional Designer).
                                Hãy viết nội dung giảng dạy chi tiết cho một bài học (Page) dựa trên ngữ cảnh sau:
                                - Tên toàn bộ khóa học: {courseTopic}
                                - Thuộc chương (Section): {sectionTitle}
                                - Chủ đề bài học này (Topic): {pageTopic}
                                - Ngôn ngữ: {language}
                                - Yêu cầu thêm: {additionalInstructions}

                                YÊU CẦU VỀ NỘI DUNG (Trường htmlContent):
                                1. Trình bày bài học một cách sư phạm: Có đoạn mở đầu dẫn dắt, giải thích chi tiết các khái niệm, đưa ra ví dụ minh họa và tóm tắt ngắn ở cuối bài.
                                2. Giá trị của trường `htmlContent` PHẢI là chuỗi mã HTML hợp lệ để hiển thị trực tiếp trên trình duyệt.
                                3. CHỈ SỬ DỤNG các thẻ HTML cơ bản để định dạng: <h2>, <h3>, <p>, <ul>, <ol>, <li>, <strong>, <em>, blockquote.
                                4. TUYỆT ĐỐI KHÔNG sử dụng các thẻ <html>, <head>, <body>, <script>, <style>.

                                {fewShotExamples}

                                YÊU CẦU VỀ ĐỊNH DẠNG ĐẦU RA:
                                1. Trả về kết quả CHÍNH XÁC theo định dạng JSON được yêu cầu.
                                2. KHÔNG thêm bất kỳ lời dẫn hay giải thích nào bên ngoài block JSON.

                                {formatInstructions}
                                """;

                // 3. Gọi AI
                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("courseTopic",
                                                                request.getCourseTopic() != null
                                                                                ? request.getCourseTopic()
                                                                                : "Không xác định")
                                                .param("sectionTitle",
                                                                request.getSectionTitle() != null
                                                                                ? request.getSectionTitle()
                                                                                : "Không xác định")
                                                .param("pageTopic", request.getPageTopic())
                                                .param("language", request.getLanguage())
                                                .param("additionalInstructions",
                                                                request.getAdditionalInstructions() != null
                                                                                ? request.getAdditionalInstructions()
                                                                                : "Không có")
                                                .param("fewShotExamples", buildPageContentFewShotBlock())
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                // 4. Xử lý chuỗi và Convert
                String jsonContent = stripJsonFence(rawResponse);
                return converter.convert(jsonContent);
        }

        private String buildPageContentFewShotBlock() {
                if (!pageContentFewShotEnabled) {
                        return "";
                }
                String positive = pageContentExampleLoader.getPositiveExample();
                String negative = pageContentExampleLoader.getNegativeExample();
                if ((positive == null || positive.isBlank()) && (negative == null || negative.isBlank())) {
                        return "";
                }
                return """
                                VÍ DỤ THAM KHẢO (FEW-SHOT):
                                ----------------------------------------
                                %s
                                ----------------------------------------
                                %s
                                ----------------------------------------
                                """.formatted(positive == null ? "" : positive, negative == null ? "" : negative);
        }

        /**
         * API tạo bộ câu hỏi từ ngữ cảnh khóa học (đủ 6 loại câu hỏi)
         */
        public AiQuizResponse generateQuizFromText(GenerateQuizRequest request) {
                BeanOutputConverter<AiQuizResponse> converter = new BeanOutputConverter<>(AiQuizResponse.class);
                String formatInstructions = converter.getFormat();
                previewQuizCoverage(request.getSourceText(), request.getNumberOfQuestions());

                String difficulty = request.getDifficulty() != null ? request.getDifficulty() : "Trung bình";
                String language = request.getLanguage() != null ? request.getLanguage() : "Vietnamese";

                return callQuizWithRetry(converter, request.getSourceText(),
                                addendum -> chatClient.prompt()
                                                .options(quizOptions)
                                                .user(u -> u.text(QuizPrompts.FROM_TEXT
                                                                + (addendum.isEmpty() ? "" : "\n\n" + addendum))
                                                                .param("sourceText", request.getSourceText())
                                                                .param("numberOfQuestions",
                                                                                request.getNumberOfQuestions())
                                                                .param("difficulty", difficulty)
                                                                .param("language", language)
                                                                .param("fewShotExamples", buildQuizFewShotBlock())
                                                                .param("formatInstructions", formatInstructions))
                                                .call()
                                                .content());
        }

        private String buildQuizFewShotBlock() {
                if (!quizFewShotEnabled) {
                        return "";
                }
                String all = quizExampleLoader.formatAllExamples();
                if (all == null || all.isBlank()) {
                        return "";
                }
                return """
                                DƯỚI ĐÂY LÀ CÁC VÍ DỤ MẪU CHO TỪNG LOẠI CÂU HỎI (FEW-SHOT). HÃY HỌC ĐÚNG SHAPE CỦA correctAnswer THEO TỪNG LOẠI:
                                ========================================================
                                %s
                                ========================================================
                                """.formatted(all);
        }

        /**
         * Issue a quiz call, validate the parsed response, and retry ONCE with a
         * corrective addendum if the schema is incomplete. Anything still wrong
         * after the second call is returned as-is — the caller decides whether
         * to surface a 400 or accept the best-effort payload.
         */
        private AiQuizResponse callQuizWithRetry(
                        BeanOutputConverter<AiQuizResponse> converter,
                        String sourceText,
                        java.util.function.Function<String, String> invokeWithAddendum) {
                String raw = invokeWithAddendum.apply("");
                AiQuizResponse first = converter.convert(stripJsonFence(raw));
                List<String> issues = QuizSchemaValidator.validate(first, sourceText);
                meters.counter("ai.quiz.schema_issues", "attempt", "first").increment(issues.size());
                if (issues.isEmpty()) {
                        return first;
                }
                log.warn("Quiz schema/citation issues on first call ({}); retrying once.", issues.size());
                String addendum = "LẦN TRƯỚC EM ĐÃ TRẢ VỀ JSON CÓ VẤN ĐỀ. HÃY SỬA:\n  - "
                                + String.join("\n  - ", issues)
                                + "\nĐặc biệt với citation.verbatimQuote — phải trích NGUYÊN VĂN từ tài liệu,"
                                + " không paraphrase. Trả về lại JSON đầy đủ.";
                String rawRetry = invokeWithAddendum.apply(addendum);
                AiQuizResponse second = converter.convert(stripJsonFence(rawRetry));
                List<String> residual = QuizSchemaValidator.validate(second, sourceText);
                meters.counter("ai.quiz.schema_issues", "attempt", "retry").increment(residual.size());
                if (!residual.isEmpty()) {
                        log.warn("Quiz schema STILL has {} issue(s) after retry; returning best effort.",
                                        residual.size());
                }
                return second;
        }

        /**
         * Tạo quiz cho một khóa học. Nguồn dữ kiện (ground) là TÀI LIỆU GỐC của
         * khóa học (đã được nạp vào {@code request.sourceText} bởi controller);
         * {@code focusTopic} là nội dung người dùng nhập để khoanh vùng ra đề.
         *
         * <p>Fallback: nếu khóa học không có tài liệu gốc, dùng chính ô nhập của
         * người dùng làm nguồn (grounded-from-text). Cả hai nhánh đều giữ ràng
         * buộc anti-hallucination + validate citation theo nguồn thực tế.
         */
        public AiQuizResponse generateCourseAwareQuiz(GenerateCourseQuizRequest request) {
                BeanOutputConverter<AiQuizResponse> converter = new BeanOutputConverter<>(AiQuizResponse.class);
                String formatInstructions = converter.getFormat();

                String documentText = orEmpty(request.getSourceText()).trim();
                String focusTopic = orEmpty(request.getFocusTopic()).trim();
                String difficulty = request.getDifficulty() != null ? request.getDifficulty() : "Trung bình";
                String language = request.getLanguage() != null ? request.getLanguage() : "Vietnamese";

                // Không có cả tài liệu lẫn nội dung người dùng nhập → không thể ground.
                if (documentText.isEmpty() && focusTopic.isEmpty()) {
                        throw new AppException(HttpStatus.BAD_REQUEST,
                                        "Cần có tài liệu nguồn của khóa học hoặc nội dung bạn nhập để tạo quiz.");
                }

                // Fallback C: khóa học không có tài liệu gốc → dùng ô nhập làm nguồn.
                if (documentText.isEmpty()) {
                        previewQuizCoverage(focusTopic, request.getNumberOfQuestions());
                        return callQuizWithRetry(converter, focusTopic,
                                        addendum -> chatClient.prompt()
                                                        .options(quizOptions)
                                                        .user(u -> u.text(QuizPrompts.FROM_TEXT
                                                                        + (addendum.isEmpty() ? "" : "\n\n" + addendum))
                                                                        .param("sourceText", focusTopic)
                                                                        .param("numberOfQuestions",
                                                                                        request.getNumberOfQuestions())
                                                                        .param("difficulty", difficulty)
                                                                        .param("language", language)
                                                                        .param("fewShotExamples", buildQuizFewShotBlock())
                                                                        .param("formatInstructions", formatInstructions))
                                                        .call()
                                                        .content());
                }

                // Không có chủ đề trọng tâm → ra đề tổng quát trên toàn bộ tài liệu.
                if (focusTopic.isEmpty()) {
                        previewQuizCoverage(documentText, request.getNumberOfQuestions());
                        return callQuizWithRetry(converter, documentText,
                                        addendum -> chatClient.prompt()
                                                        .options(quizOptions)
                                                        .user(u -> u.text(QuizPrompts.FROM_TEXT
                                                                        + (addendum.isEmpty() ? "" : "\n\n" + addendum))
                                                                        .param("sourceText", documentText)
                                                                        .param("numberOfQuestions",
                                                                                        request.getNumberOfQuestions())
                                                                        .param("difficulty", difficulty)
                                                                        .param("language", language)
                                                                        .param("fewShotExamples", buildQuizFewShotBlock())
                                                                        .param("formatInstructions", formatInstructions))
                                                        .call()
                                                        .content());
                }

                // Đầy đủ: tài liệu gốc + chủ đề trọng tâm người dùng nhập.
                previewQuizCoverage(documentText, request.getNumberOfQuestions());
                return callQuizWithRetry(converter, documentText,
                                addendum -> chatClient.prompt()
                                                .options(quizOptions)
                                                .user(u -> u.text(QuizPrompts.FROM_DOCUMENT_FOCUSED
                                                                + (addendum.isEmpty() ? "" : "\n\n" + addendum))
                                                                .param("focusTopic", focusTopic)
                                                                .param("sourceText", documentText)
                                                                .param("numberOfQuestions", request.getNumberOfQuestions())
                                                                .param("difficulty", difficulty)
                                                                .param("language", language)
                                                                .param("fewShotExamples", buildQuizFewShotBlock())
                                                                .param("formatInstructions", formatInstructions))
                                                .call()
                                                .content());
        }

        private static String orEmpty(String s) {
                return s != null ? s : "";
        }

        public AiKnowledgeAnswerResponse askCourseKnowledge(AskKnowledgeRequest request) {
                BeanOutputConverter<AiKnowledgeAnswerResponse> converter = new BeanOutputConverter<>(
                                AiKnowledgeAnswerResponse.class);
                String formatInstructions = converter.getFormat();

                String userPrompt = """
                                Bạn là trợ giảng AI cho khóa học. Chỉ được trả lời dựa trên ngữ cảnh khóa học và nội dung bài học cung cấp.

                                NGỮ CẢNH:
                                - courseTitle: {courseTitle}
                                - courseDescription: {courseDescription}
                                - sectionTitle: {sectionTitle}
                                - pageTitle: {pageTitle}
                                - pageContent: {pageContent}

                                CÂU HỎI NGƯỜI HỌC:
                                {question}

                                QUY TẮC:
                                1. Nếu đủ dữ liệu trong ngữ cảnh, trả lời ngắn gọn, rõ ràng, đúng trọng tâm.
                                2. Nếu không đủ dữ liệu, trả lời rằng chưa đủ thông tin trong nội dung khóa học hiện tại và gợi ý người dùng bổ sung nội dung.
                                3. Không bịa thông tin ngoài nội dung đã cung cấp.
                                4. Ngôn ngữ trả lời: {language}.

                                Trả về đúng JSON theo schema:
                                {formatInstructions}
                                """;

                String rawResponse = chatClient.prompt()
                                .user(u -> u.text(userPrompt)
                                                .param("courseTitle",
                                                                request.getCourseTitle() != null
                                                                                ? request.getCourseTitle()
                                                                                : "")
                                                .param("courseDescription",
                                                                request.getCourseDescription() != null
                                                                                ? request.getCourseDescription()
                                                                                : "")
                                                .param("sectionTitle",
                                                                request.getSectionTitle() != null
                                                                                ? request.getSectionTitle()
                                                                                : "")
                                                .param("pageTitle",
                                                                request.getPageTitle() != null ? request.getPageTitle()
                                                                                : "")
                                                .param("pageContent",
                                                                request.getPageContent() != null
                                                                                ? request.getPageContent()
                                                                                : "")
                                                .param("question",
                                                                request.getQuestion() != null ? request.getQuestion()
                                                                                : "")
                                                .param("language",
                                                                request.getLanguage() != null ? request.getLanguage()
                                                                                : "Vietnamese")
                                                .param("formatInstructions", formatInstructions))
                                .call()
                                .content();

                String jsonContent = stripJsonFence(rawResponse);
                return converter.convert(jsonContent);
        }
}