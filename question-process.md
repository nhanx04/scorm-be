# Question process (Option A - Autosave/Incremental)

Tài liệu này mô tả cách backend xử lý **Question** trong quy trình soạn thảo Course theo **Option A (autosave/incremental)**.
Bạn đã có CRUD cho `course`, `section`, `page`, `contentPage`, `quizPage`, `contentBlock`, `thumbnailOfCourse`.
Phần còn lại cần hoàn thiện là **Question authoring + attach/detach vào quizPage** + **tree read** để render editor và phục vụ export.

---

## 1) Mục tiêu & nguyên tắc thiết kế

### 1.1 Aggregate boundaries

- **Aggregate root**: `question`
- **Child tables theo type** (không expose CRUD độc lập):
  - `question_choice_option`
  - `question_true_false` (optional)
  - `question_fill_blank`, `question_blank`, `question_blank_answer`
  - `question_matching`, `question_matching_left`, `question_matching_right`, `question_matching_pair`
  - `question_short_answer`, `question_short_answer_expected`
  - `question_grouping`, `question_group`, `question_group_item`, `question_group_item_answer`
- **Quan hệ gắn vào quiz**: `question_of_quiz (quiz_questionid, quiz_pageid)`
- **Media**:
  - `media_of_question (questionid, mediaid)`
  - `image_of_choice_option (optionid, mediaid)`

### 1.2 Không cần CRUD riêng cho mọi bảng con

Bạn chỉ cần:

- CRUD ở mức **Question** (base + details theo type)
- API **attach/detach** để gắn question vào `quiz_page`

Các bảng con (options/blanks/pairs/expected/...) được quản lý **thông qua Question** (sub-resource), đảm bảo transaction và validation đồng bộ.

---

## 2) Luồng API chuẩn cho Editor (Option A)

Giả định `quizPage` đã tồn tại cho `pageId`.

### 2.1 Tạo câu hỏi và gắn vào quiz

- **B1 - Create Question**
  - `POST /api/questions`

- **B2 - Attach vào quiz**
  - `POST /api/quiz-pages/{pageId}/questions/{questionId}`
  - Backend insert vào `question_of_quiz`

- **B3 - Autosave khi chỉnh sửa**
  - Sửa field chung: `PATCH /api/questions/{questionId}`
  - Sửa details theo type: `PUT /api/questions/{questionId}/details`

### 2.2 Gỡ câu hỏi khỏi quiz

- `DELETE /api/quiz-pages/{pageId}/questions/{questionId}`
- Chỉ **detatch** khỏi quiz (xóa row trong `question_of_quiz`).
- Question vẫn có thể tồn tại để tái sử dụng (tuỳ policy).

### 2.3 Xóa Question (tuỳ policy)

- `DELETE /api/questions/{questionId}`
- Khuyến nghị enforce rule:
  - Nếu question đang được gắn ở bất kỳ quiz nào (`question_of_quiz` còn row) thì **không cho xóa**, hoặc **soft-delete**.

---

## 3) Thiết kế API đề xuất

### 3.1 Question endpoints (khuyến nghị tối thiểu)

- `POST /api/questions`
- `GET /api/questions/{questionId}` (phục vụ editor load detail nhanh)
- `PATCH /api/questions/{questionId}` (update base fields)
- `PUT /api/questions/{questionId}/details` (**replace toàn bộ details theo type**, idempotent)
- `DELETE /api/questions/{questionId}` (tuỳ)

### 3.2 Attach/detach question vào quizPage

- `POST /api/quiz-pages/{pageId}/questions/{questionId}`
- `DELETE /api/quiz-pages/{pageId}/questions/{questionId}`

### 3.3 Thứ tự câu hỏi trong quiz (open point)

Nếu editor cần reorder câu hỏi, hiện tại bảng `question_of_quiz` trong DB (Create_Database.sql) **không có** `order_index`.

Bạn cần chọn 1 trong các hướng:

- **Cách A (khuyến nghị)**: thêm `order_index` vào `question_of_quiz`
- **Cách B**: lưu order trong `quiz_page.extra_config` (JSON)

---

## 4) DTO / Payload gợi ý

### 4.1 Base fields (map vào bảng `question`)

- `title`
- `promptHtml`
- `questionType`
- `points`
- `shuffleOptions`
- `caseSensitive`
- `extraConfig` (JSON)

### 4.2 Payload đề xuất cho `PUT /api/questions/{id}/details`

Khuyến nghị structure:

- `question`: phần base (optional nếu bạn tách riêng PATCH)
- `details`: phần theo `questionType`

Ví dụ khung:

```json
{
  "question": {
    "title": "...",
    "promptHtml": "<p>...</p>",
    "questionType": "MULTIPLE_CHOICE",
    "points": 1,
    "shuffleOptions": true,
    "caseSensitive": false,
    "extraConfig": {}
  },
  "details": {}
}
```

### 4.3 Gợi ý `details` theo type

- **MULTIPLE_CHOICE / CHOICE**
  - `options: [{ orderIndex, contentHtml, isCorrect, feedbackHtml?, mediaIds? }]`

- **TRUE_FALSE**
  - Cách 1: biểu diễn bằng 2 options trong `question_choice_option`
  - Cách 2: dùng `question_true_false.correct_value`

- **FILL_BLANK**
  - `orderedBlanks`
  - `blanks: [{ blankKey, orderIndex, answers: [{ answerText, matchRule }] }]`

- **MATCHING**
  - `leftItems: [{ orderIndex, contentHtml }]`
  - `rightItems: [{ orderIndex, contentHtml }]`
  - `pairs: [{ leftIndex, rightIndex }]` hoặc `{ leftId, rightId }`

- **SHORT_ANSWER**
  - `minLength`, `maxLength`
  - `expectedAnswers: [{ answerText, matchRule }]`

- **GROUPING**
  - `groups: [{ orderIndex, title }]`
  - `items: [{ orderIndex, contentHtml }]`
  - `itemAnswers: [{ itemIndex, groupIndex }]` hoặc `{ itemId, groupId }`

---

## 5) Quy tắc validation bắt buộc

Bạn nên có 1 tầng validate theo `questionType` trước khi ghi DB.

### 5.1 Base validation

- `questionType` bắt buộc
- `points >= 0` (hoặc theo rule business)
- `title`/`promptHtml` theo policy (có thể cho phép null nhưng nên thống nhất)

### 5.2 Type-specific validation (gợi ý)

- **CHOICE**
  - `options.length >= 2`
  - có ít nhất 1 `isCorrect=true`

- **TRUE_FALSE**
  - phải có đúng 1 đáp án đúng (true hoặc false)

- **FILL_BLANK**
  - `blanks.length >= 1`
  - mỗi blank có `answers.length >= 1`

- **MATCHING**
  - left/right đều >= 1
  - `pairs.length >= 1`
  - pairs trỏ tới left/right hợp lệ

- **SHORT_ANSWER**
  - expectedAnswers >= 1 (nếu cần chấm tự động)

- **GROUPING**
  - groups >= 1, items >= 1
  - mỗi item phải map tới đúng 1 group hợp lệ

---

## 6) Chiến lược persist dữ liệu (quan trọng nhất)

### 6.1 Quy tắc vàng: Replace details theo type trong 1 transaction

Trong `PUT /api/questions/{id}/details`:

- `BEGIN`
- Update `question` (nếu payload có base fields)
- Cleanup details hiện có
- Insert lại details mới theo payload
- `COMMIT`

Lợi ích:

- Tránh dữ liệu cũ “sót” khi user chỉnh sửa nhiều lần
- Dễ triển khai và dễ test
- Phù hợp autosave vì FE gửi “trạng thái cuối”

### 6.2 Cleanup khi đổi `questionType`

Khi đổi `question.question_type` (ví dụ MCQ -> MATCHING):

- Phải xóa sạch details cũ của type trước
- Chỉ giữ base fields của `question`

Nếu bạn dùng endpoint `PUT .../details` kiểu replace, việc cleanup sẽ tự nhiên xảy ra miễn bạn xóa đúng bảng liên quan.

### 6.3 Media handling

- **Media chung của question** (`media_of_question`): replace list `mediaIds`
- **Ảnh của option** (`image_of_choice_option`): vì phụ thuộc `optionid` được sinh mới khi replace,
  - insert options trước
  - lấy `optionid`
  - insert mapping ảnh tương ứng

---

## 7) Lấy dữ liệu để render editor

### 7.1 Khuyến nghị dùng tree endpoint

Bạn đã có (hoặc sẽ có) endpoint:

- `GET /api/courses/{courseId}/tree`

Tree cần trả về:

- sections/pages
- quizPage
- questions attached (qua `question_of_quiz`)
- question details theo type (options/blanks/matching/...)
- media relations (`media_of_question`, `image_of_choice_option`)

Cách này giảm số request và là nền tảng cho SCORM export.

---

## 8) Checklist triển khai

### P0 (đủ chạy end-to-end authoring)

- `POST /api/questions`
- `PUT /api/questions/{id}/details`
- `POST /api/quiz-pages/{pageId}/questions/{questionId}`
- `DELETE /api/quiz-pages/{pageId}/questions/{questionId}`
- `GET /api/courses/{courseId}/tree` trả kèm quiz/questions/details

### P1 (hoàn thiện UX)

- `PATCH /api/questions/{id}` (update nhanh base fields)
- Error message validate rõ ràng theo type
- Reorder questions trong quiz (cần chốt cách lưu order)

### P2 (thư viện câu hỏi)

- List/search questions theo user/org
- Policy reuse + delete/soft-delete
