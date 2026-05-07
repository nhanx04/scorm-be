# AI Prompt Engineering Guide

Tài liệu này mô tả cách module AI của hệ thống được prompt cho LLM (Gemini 2.5 Pro qua Spring AI), cách bảo trì các ví dụ few-shot, và quy trình rollout thay đổi prompt an toàn.

## 1. Stack tổng quan

- **Framework:** Spring AI `1.1.0-M1` ([pom.xml](../pom.xml))
- **Provider:** Google GenAI — `spring-ai-starter-model-google-genai`
- **Model:** `gemini-2.5-pro`, `temperature=0.7` (xem [application.properties](../src/main/resources/application.properties))
- **Document reader:** `spring-ai-tika-document-reader` cho input PDF/DOCX/PPT
- **Service entry point:** [AiGeneratorService.java](../src/main/java/com/scorm/generator/service/AiGeneratorService.java)
- **Controller:** [AiFeatureController.java](../src/main/java/com/scorm/generator/controller/AiFeatureController.java) — prefix `/ai`

## 2. 6 endpoint AI

| Endpoint | Method service | Output type |
|---|---|---|
| `POST /ai/generate-outline` | `generateCourseOutline` | `AiCourseOutline` |
| `POST /ai/generate-outline-from-file` | `generateCourseOutlineFromFile` | `AiCourseOutline` |
| `POST /ai/save-outline` | `CourseService.saveAiCourseOutline` | `CourseResponse` (persist) |
| `POST /ai/generate-page-content` | `generatePageContent` | `AiPageContentResponse` |
| `POST /ai/generate-quiz` | `generateQuizFromText` | `AiQuizResponse` |
| `POST /ai/generate-course-quiz` | `generateCourseAwareQuiz` | `AiQuizResponse` |
| `POST /ai/ask-knowledge` | `askCourseKnowledge` | `AiKnowledgeAnswerResponse` |

DTO request/response: [src/main/java/com/scorm/generator/dto/ai/](../src/main/java/com/scorm/generator/dto/ai/)

## 3. Các kỹ thuật prompt engineering đang dùng

| Kỹ thuật | Áp dụng ở | Ghi chú |
|---|---|---|
| Persona system prompt | Tất cả method | Set một lần khi build `ChatClient` |
| Per-task persona | Quiz, ask-knowledge | Thêm role cụ thể trong user prompt |
| Structured output qua `BeanOutputConverter` | Tất cả method | Schema JSON sinh tự động từ Java record |
| Template + `.param()` binding | Tất cả method | Tránh nối chuỗi thô |
| Default fallback cho null | Tất cả request DTO | Tránh placeholder render thành `null` |
| Sectioned prompt (heading viết HOA) | Tất cả method | Phân tách rõ instruction vs context |
| Delimiter `=====` cô lập user data | Outline-from-file, quiz | Giảm rủi ro prompt-injection |
| Negative prompting ("TUYỆT ĐỐI KHÔNG") | Page content, quiz, ask-knowledge | Cấm hành vi cụ thể |
| Whitelist constraint | Page content (HTML tags), quiz (6 type) | Liệt kê positive |
| Self-evaluation flag | Ask-knowledge (`groundedInCourse`) | Model tự khai báo confidence |
| Output post-processing | Tất cả method | `stripJsonFence()` dọn markdown fence |
| **Few-shot examples** (positive + contrastive) | Quiz, page content | Xem mục 4 |

## 4. Few-shot examples — cách bảo trì

### 4.1 Vì sao có few-shot

- **Quiz**: `AiQuestion.correctAnswer` là `Object` polymorphic, schema tự sinh không ràng buộc shape theo `type`. Few-shot dạy model shape đúng cho từng loại trong 6 loại câu hỏi.
- **Page content**: ràng buộc HTML whitelist (`<h2>`, `<p>`, `<ul>`...) khó truyền tải bằng prose — ví dụ positive + negative thuyết phục hơn.

### 4.2 Vị trí file

```
src/main/resources/prompts/
├── quiz/
│   ├── examples-mcq-single.json
│   ├── examples-mcq-multiple.json
│   ├── examples-true-false.json
│   ├── examples-short-answer.json
│   ├── examples-fill-blank.json
│   └── examples-matching.json
└── page-content/
    ├── example-positive.json
    └── example-negative.json
```

### 4.3 Schema từng file

**Quiz example (`examples-*.json`):**

```json
{
  "type": "<TYPE>",
  "description": "Ghi chú giúp model hiểu shape của loại này",
  "sourceContext": "Đoạn văn nguồn giả định để model thấy mối quan hệ source → question",
  "expectedOutput": { ...JSON khớp với AiQuizResponse.AiQuestion... }
}
```

**Page content positive (`example-positive.json`):**

```json
{
  "label": "POSITIVE_EXAMPLE",
  "description": "...",
  "input": { "courseTopic": "...", "sectionTitle": "...", "pageTopic": "..." },
  "expectedOutput": { ...JSON khớp với AiPageContentResponse... },
  "rationale": ["bullet 1", "bullet 2"]
}
```

**Page content negative (`example-negative.json`):**

```json
{
  "label": "NEGATIVE_EXAMPLE",
  "description": "...",
  "wrongOutput": { ...HTML chứa anti-pattern (script, div, h1, inline style)... },
  "violations": [
    { "issue": "...", "reason": "..." }
  ]
}
```

### 4.4 Quy ước shape cho `correctAnswer` trong từng loại quiz

| Loại | `correctAnswer` shape | Trường khác |
|---|---|---|
| `MCQ_SINGLE` | `String` (1 đáp án) | `options` >= 2 |
| `MCQ_MULTIPLE` | `List<String>` (>=2 đáp án) | `options` >= 3 |
| `TRUE_FALSE` | `boolean` (không phải `"true"`/`"Đúng"`) | `options: null` |
| `SHORT_ANSWER` | `List<String>` (đáp án đồng nghĩa) | `options: null`, `sentenceHtml: null` |
| `FILL_IN_THE_BLANK` | `List<String>` (theo thứ tự `___`) | `sentenceHtml` chứa marker `___` |
| `MATCHING` | `null` | `pairs: List<{left,right}>` >=3 cặp |

### 4.5 Khi sửa DTO, làm gì

DTO `AiQuestion` / `AiPageContentResponse` thay đổi → **PHẢI** chạy lại test loader để verify ví dụ vẫn parse được:

```bash
mvn test -Dtest='QuizExampleLoaderTest,PageContentExampleLoaderTest'
```

Nếu fail: cập nhật JSON ví dụ ở `src/main/resources/prompts/...` cho khớp schema mới. Đây là safety-net duy nhất chống "example rot".

### 4.6 Khi thêm loại câu hỏi mới (vd `ORDERING`)

1. Thêm constant vào enum/code xử lý câu hỏi
2. Tạo file `src/main/resources/prompts/quiz/examples-ordering.json` theo schema 4.3
3. Cập nhật `EXPECTED_TYPES` trong [QuizExampleLoaderTest.java](../src/test/java/com/scorm/generator/service/ai/QuizExampleLoaderTest.java) thêm `"ORDERING"`
4. Thêm 1 test method `ordering_correctAnswerIs<Shape>()` cho shape mới
5. Cập nhật prompt template ở `generateQuizFromText` / `generateCourseAwareQuiz` (mục "BẮT BUỘC: 2." và "QUY ƯỚC DỮ LIỆU") để nhắc đến loại mới
6. Cập nhật bảng 4.4 trong tài liệu này

## 5. Feature flags

| Property | ENV | Default | Tác dụng |
|---|---|---|---|
| `app.ai.quiz.few-shot-enabled` | `AI_QUIZ_FEW_SHOT_ENABLED` | `true` | Tắt → quiz prompt không nhồi 6 ví dụ (về hành vi pre-few-shot) |
| `app.ai.page-content.few-shot-enabled` | `AI_PAGE_FEW_SHOT_ENABLED` | `true` | Tắt → page content prompt không có positive/negative example |

**Khi nào tắt:** phát hiện regression sau deploy (parse-fail tăng, latency tăng đột biến, hoặc model bị bias theo ví dụ). Tắt nhanh qua env, không cần rebuild.

## 6. Quy trình thay đổi prompt

### 6.1 Sửa prompt trong `AiGeneratorService`

1. Sửa text block trong method tương ứng
2. Nếu thêm placeholder mới → bind qua `.param()` cho **đủ tất cả** placeholder (Spring AI throw nếu thiếu)
3. Compile: `mvn clean compile`
4. Smoke test với API key thật (không có test tự động vì tốn token + non-deterministic)

### 6.2 Sửa file JSON ví dụ few-shot

1. Sửa file ở `src/main/resources/prompts/`
2. Chạy `mvn test -Dtest='QuizExampleLoaderTest,PageContentExampleLoaderTest'`
3. Nếu test fail → JSON sai schema, fix lại

### 6.3 Smoke test thủ công (sau merge)

Chạy 3 case minimum trong Postman/Swagger UI:

- [ ] `POST /ai/generate-quiz` với `numberOfQuestions=6, difficulty="Trung bình"` — verify response chứa đủ 6 loại, mỗi `correctAnswer` đúng shape ở mục 4.4
- [ ] `POST /ai/generate-page-content` với 1 topic Vietnamese — verify `htmlContent` chỉ chứa thẻ trong whitelist (regex check: không có `<script`, `<style`, `<div`, `<h1`)
- [ ] `POST /ai/ask-knowledge` với câu hỏi **NGOÀI** `pageContent` — verify `groundedInCourse: false` và `answer` từ chối lịch sự (không bịa)

### 6.4 Theo dõi sau deploy

Quan sát log:

```
ERROR ... AI returned invalid JSON for quiz. Raw: ...
```

Nếu xuất hiện thường xuyên (>1%) → ví dụ few-shot có thể đang gây bias hoặc model output đã drift. Cân nhắc tắt flag tạm thời và điều tra.

## 7. Các giới hạn đã biết

| Giới hạn | Workaround | Trạng thái |
|---|---|---|
| Không retry khi LLM trả JSON sai schema | Manual retry từ client; theo dõi log | Chấp nhận tạm |
| `documentContext` của outline-from-file nuốt nguyên file → có thể vượt context window | Cắt file trước khi upload (FE) | Chưa fix |
| `temperature=0.7` cố định toàn cục — không phù hợp cho tác vụ cần chính xác cao (quiz, ask-knowledge) | Có thể override per-call qua `ChatOptions` | Backlog |
| Few-shot inject all 6 ví dụ ngay cả khi `numberOfQuestions=3` | Conditional loading nếu cần tối ưu token | Backlog |
| Không stream response | Endpoint hiện đều blocking `.call()` | Chưa cần |
| Không cache prompt trùng | Spring AI 1.1.x chưa hỗ trợ Gemini prompt cache | Chờ provider |

## 8. File reference nhanh

| Khi cần | Đọc |
|---|---|
| Sửa prompt | [AiGeneratorService.java](../src/main/java/com/scorm/generator/service/AiGeneratorService.java) |
| Sửa ví dụ quiz | [src/main/resources/prompts/quiz/](../src/main/resources/prompts/quiz/) |
| Sửa ví dụ page content | [src/main/resources/prompts/page-content/](../src/main/resources/prompts/page-content/) |
| Sửa loader | [QuizExampleLoader.java](../src/main/java/com/scorm/generator/service/ai/QuizExampleLoader.java) · [PageContentExampleLoader.java](../src/main/java/com/scorm/generator/service/ai/PageContentExampleLoader.java) |
| Test ví dụ | [QuizExampleLoaderTest.java](../src/test/java/com/scorm/generator/service/ai/QuizExampleLoaderTest.java) · [PageContentExampleLoaderTest.java](../src/test/java/com/scorm/generator/service/ai/PageContentExampleLoaderTest.java) |
| Cấu hình model | [application.properties](../src/main/resources/application.properties) (mục `spring.ai.google.genai.*`) |
| Endpoint AI | [AiFeatureController.java](../src/main/java/com/scorm/generator/controller/AiFeatureController.java) |
