# FE Flow gọi API khi tạo Course (Option A - Autosave/Incremental)

Tài liệu này mô tả **cách Frontend (FE)** gọi các API trong quá trình authoring Course theo flow:

- Tạo Course -> tạo Section -> tạo Page
- Nếu là Content page: tạo `content_page` + blocks
- Nếu là Quiz page: tạo `quiz_page` + tạo/sửa Question + attach/detach Question vào quiz
- Load lại course tree bằng `GET /courses/{courseId}` để render editor

> Lưu ý về path: codebase hiện dùng base path dạng **không có prefix `/api`**.
> Ví dụ: `POST /courses`, `POST /questions`, `POST /quiz-pages/{pageId}/questions/{questionId}`.

---

## 1) Quy ước chung cho FE

- **Auth**: các endpoint liên quan course/page/quiz attach yêu cầu user đăng nhập (JWT).
- **Autosave**:
  - FE nên gọi API ngay khi user thao tác (create/update)
  - Có debounce (ví dụ 300-800ms) cho các field text để tránh spam request
- **Tối ưu tải dữ liệu**:
  - `GET /courses/{courseId}` trả **tree** và với quiz page sẽ có `quizPage.questions` dạng **summary**
  - Khi user click edit 1 question, FE gọi `GET /questions/{questionId}` để lấy **full details**

---

## 2) Flow tổng: tạo course -> section -> page

### 2.1 Tạo course

- **Request**: `POST /courses`

```http
POST /courses
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Course A",
  "passingScore": 80.0,
  "attemptLimit": 3,
  "durationMin": 30,
  "status": "DRAFT",
  "extraInfor": {"level":"beginner"}
}
```

- **Response**: `CourseResponse` (có `courseId`)

### 2.2 Tạo section

- **Request**: `POST /courses/{courseId}/sections`

```http
POST /courses/1/sections
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Section 1",
  "description": "...",
  "orderIndex": 1,
  "learningObjective": "...",
  "themeOverride": null
}
```

### 2.3 Tạo page

- **Request**: `POST /sections/{sectionId}/pages`

```http
POST /sections/10/pages
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Quiz 1",
  "orderIndex": 1,
  "pageType": "QUIZ",
  "themeOverride": null
}
```

---

## 3) Flow cho Content Page

### 3.1 Tạo content_page

- `POST /pages/{pageId}/content-page`

### 3.2 Tạo blocks

- `POST /content-pages/{pageId}/blocks`

### 3.3 Update block

- `PATCH /content-blocks/{blockId}`

---

## 4) Flow cho Quiz Page + Questions (phần mới)

### 4.1 Tạo quiz_page cho page

- **Request**: `POST /pages/{pageId}/quiz-page`

```http
POST /pages/2/quiz-page
Content-Type: application/json
Authorization: Bearer <token>

{
  "passingScore": 80.0,
  "attemptAllowed": 3
}
```

### 4.2 Tạo Question (base)

- **Request**: `POST /questions`

```http
POST /questions
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Question 1",
  "instruction": null,
  "promptHtml": "<p>2 + 2 = ?</p>",
  "questionType": "MCQ_SINGLE",
  "points": 1,
  "shuffleOptions": true,
  "caseSensitive": false,
  "extraConfig": {}
}
```

- **Response**: `QuestionResponse` (có `question.questionId`)

### 4.3 Lưu details theo type (replace details)

- **Request**: `PUT /questions/{questionId}/details`

#### 4.3.1 MCQ_SINGLE / MCQ_MULTI

```http
PUT /questions/100/details
Content-Type: application/json
Authorization: Bearer <token>

{
  "question": {
    "promptHtml": "<p>2 + 2 = ?</p>",
    "questionType": "MCQ_SINGLE"
  },
  "details": {
    "options": [
      {"orderIndex": 1, "contentHtml": "<p>3</p>", "isCorrect": false, "scoreFraction": 0},
      {"orderIndex": 2, "contentHtml": "<p>4</p>", "isCorrect": true,  "scoreFraction": 1}
    ]
  }
}
```

#### 4.3.2 TRUE_FALSE

```http
PUT /questions/101/details
Content-Type: application/json
Authorization: Bearer <token>

{
  "question": {"questionType": "TRUE_FALSE"},
  "details": {"correctValue": true}
}
```

#### 4.3.3 FILL_BLANK

```http
PUT /questions/102/details
Content-Type: application/json
Authorization: Bearer <token>

{
  "question": {"questionType": "FILL_BLANK"},
  "details": {
    "orderedBlanks": false,
    "blanks": [
      {
        "blankKey": "b1",
        "orderIndex": 1,
        "answers": [
          {"answerText": "4", "matchRule": "EXACT"},
          {"answerText": "four", "matchRule": "CASE_INSENSITIVE"}
        ]
      }
    ]
  }
}
```

#### 4.3.4 MATCHING

```http
PUT /questions/103/details
Content-Type: application/json
Authorization: Bearer <token>

{
  "question": {"questionType": "MATCHING"},
  "details": {
    "leftItems": [
      {"orderIndex": 1, "contentHtml": "<p>Dog</p>"},
      {"orderIndex": 2, "contentHtml": "<p>Cat</p>"}
    ],
    "rightItems": [
      {"orderIndex": 1, "contentHtml": "<p>Chó</p>"},
      {"orderIndex": 2, "contentHtml": "<p>Mèo</p>"}
    ],
    "pairs": [
      {"leftId": 1, "rightId": 1},
      {"leftId": 2, "rightId": 2}
    ]
  }
}
```

#### 4.3.5 SHORT_ANSWER

```http
PUT /questions/104/details
Content-Type: application/json
Authorization: Bearer <token>

{
  "question": {"questionType": "SHORT_ANSWER"},
  "details": {
    "expectedAnswers": [
      {"answerText": "4", "matchRule": "EXACT"}
    ]
  }
}
```

#### 4.3.6 GROUPING

```http
PUT /questions/105/details
Content-Type: application/json
Authorization: Bearer <token>

{
  "question": {"questionType": "GROUPING"},
  "details": {
    "groups": [
      {"orderIndex": 1, "title": "Animals"},
      {"orderIndex": 2, "title": "Fruits"}
    ],
    "items": [
      {"orderIndex": 1, "contentHtml": "<p>Dog</p>"},
      {"orderIndex": 2, "contentHtml": "<p>Apple</p>"}
    ],
    "itemAnswers": [
      {"itemId": 1, "groupId": 1},
      {"itemId": 2, "groupId": 2}
    ]
  }
}
```

> Ghi chú: FE nên lưu `questionId` và call `GET /questions/{questionId}` khi cần load lại full details để edit.

### 4.4 Attach Question vào quizPage

- **Request**: `POST /quiz-pages/{pageId}/questions/{questionId}`

```http
POST /quiz-pages/2/questions/100
Authorization: Bearer <token>
```

- **Response**: `200 OK` (idempotent: attach lần 2 sẽ không lỗi)

### 4.5 Detach Question khỏi quizPage

- **Request**: `DELETE /quiz-pages/{pageId}/questions/{questionId}`

```http
DELETE /quiz-pages/2/questions/100
Authorization: Bearer <token>
```

### 4.6 Load tree course để render editor

- **Request**: `GET /courses/{courseId}`

```http
GET /courses/1
Authorization: Bearer <token>
```

- **Response**: trong `sections[].pages[].quizPage.questions` sẽ có danh sách:

```json
{
  "pageId": 2,
  "passingScore": 80.0,
  "attemptAllowed": 3,
  "questions": [
    {"questionId": 100, "title": "Question 1", "questionType": "MCQ_SINGLE"}
  ]
}
```

### 4.7 Load full details của 1 question để edit

- **Request**: `GET /questions/{questionId}`

```http
GET /questions/100
Authorization: Bearer <token>
```

- **Response**: `QuestionResponse` gồm:

- `question`: base
- `details`: object phụ thuộc type

---

## 5) Sequence gợi ý cho FE (Quiz authoring)

1. User tạo quiz page (nếu chưa có) -> `POST /pages/{pageId}/quiz-page`
2. User bấm "Add question"
   - `POST /questions` (create base)
   - `PUT /questions/{id}/details` (save details initial)
   - `POST /quiz-pages/{pageId}/questions/{questionId}` (attach)
3. User chỉnh sửa question
   - debounce -> `PUT /questions/{id}/details`
4. User xóa question khỏi quiz
   - `DELETE /quiz-pages/{pageId}/questions/{questionId}`
5. FE refresh tree (hoặc optimistic update)
   - `GET /courses/{courseId}`

---

## 6) Lưu ý quan trọng cho FE (runtime)

Hiện tại backend đang nhận `QuestionDetailsRequest.details` kiểu `Object`.
Để tránh tình trạng JSON bị deserialize thành `LinkedHashMap` không cast được, FE nên:

- Gửi đúng shape JSON theo `questionType`.
- Nếu gặp lỗi deserialize/cast khi chạy thực tế, cần refactor backend sang `JsonNode details` + `objectMapper.convertValue(...)` theo `questionType`.

Nếu bạn muốn, mình có thể refactor backend theo hướng này để đảm bảo chạy chắc chắn.

