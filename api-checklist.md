# API Checklist (SCORM-BE)

## 1) Đã có trong codebase (hiện trạng)

- **Auth**
  - **[x]** `POST /api/auth/register`
  - **[x]** `POST /api/auth/login`
- **Organization / Membership**
  - **[x]** Có `OrganizationController`, `OrganizationService`
  - **[x]** Có entity `Organization`, `Membership`, `User`
  - **[ ]** Rà soát/chuẩn hóa endpoint theo tài liệu (nếu chưa đầy đủ CRUD/mời thành viên)
- **Library / Media Assets**
  - **[x]** `LibraryController`, `MediaAssetController`
  - **[x]** Entities: `MyLibrary`, `MediaAsset`, `ImageAsset`, `AudioAsset`, `DocumentAsset`, `VideoAsset`
  - **[x]** Có `S3StorageService`

## 2) Database đã update (những phần ảnh hưởng kế hoạch)

- **Theme customization**
  - **[x]** `scorm_export_config.theme_config (JSONB)`
  - **[x]** `scorm_package.theme_snapshot (JSONB)`
  - **[x]** `section.theme_override (JSONB)`
  - **[x]** `page.theme_override (JSONB)`
- **Question model nâng cấp (universal)**
  - **[x]** `question` (type/points/shuffle/case_sensitive/extra_config)
  - **[x]** `question_choice_option`, `question_true_false`
  - **[x]** `question_fill_blank`, `question_blank`, `question_blank_answer`
  - **[x]** `question_matching_*`, `question_matching_pair`
  - **[x]** `question_short_answer`, `question_short_answer_expected`
  - **[x]** `question_grouping`, `question_group`, `question_group_item`, `question_group_item_answer`
  - **[x]** `image_of_choice_option` (thay `image_of_option`)
- **Video asset**
  - **[x]** `video_asset(mediaid, youtube_url)` đã được gộp vào `Create_Database.sql`

## 3) Entities cần làm tiếp theo (bắt buộc trước khi tạo gói SCORM)

### 3.1 Course authoring domain (chưa thấy trong code)

- **[ ]** Entity `Course` (map bảng `course`)
- **[ ]** Entity `Section` (map bảng `section` + `theme_override`)
- **[ ]** Entity `Page` (map bảng `page` + `theme_override`)
- **[ ]** Entity `ContentPage` (map bảng `content_page`)
- **[ ]** Entity `QuizPage` (map bảng `quiz_page`)
- **[ ]** Entity `ContentBlock` (map bảng `content_block`)
- **[ ]** Entity `ThumbnailOfCourse` (map bảng `thumbnail_of_course`)

### 3.2 Quiz / Question domain (cần refactor theo DB mới)

- **[ ]** Entity `Question` (map bảng `question` mới)
- **[ ]** Entity `QuestionChoiceOption` (map bảng `question_choice_option`)
- **[ ]** Entity `QuestionTrueFalse` (map bảng `question_true_false` - optional)
- **[ ]** Entity `QuestionFillBlank`, `QuestionBlank`, `QuestionBlankAnswer`
- **[ ]** Entity `QuestionMatchingLeft`, `QuestionMatchingRight`, `QuestionMatchingPair`
- **[ ]** Entity `QuestionShortAnswer`, `QuestionShortAnswerExpected`
- **[ ]** Entity `QuestionGrouping`, `QuestionGroup`, `QuestionGroupItem`, `QuestionGroupItemAnswer`
- **[ ]** Entity `QuestionOfQuiz` (map bảng `question_of_quiz`)
- **[ ]** Entity `MediaOfQuestion` (map bảng `media_of_question`)
- **[ ]** Entity `ImageOfChoiceOption` (map bảng `image_of_choice_option`)

### 3.3 SCORM export domain (cần để persist package)

- **[ ]** Entity `ScormExportConfig` (map bảng `scorm_export_config` + `theme_config`)
- **[ ]** Entity `ScormPackage` (map bảng `scorm_package` + `theme_snapshot`)
- **[ ]** Entity `ScormActivity` (map bảng `scorm_activity`)
- **[ ]** Entity `ScormResource` (map bảng `scorm_resource`)

## 4) APIs cần làm tiếp theo (theo thứ tự ưu tiên)

### 4.1 Course CRUD + tree read (đầu vào cho export)

- **[ ]** `POST /api/courses` tạo course
- **[ ]** `GET /api/courses` list course theo user/org
- **[ ]** `GET /api/courses/{courseId}` lấy chi tiết course
- **[ ]** `PATCH /api/courses/{courseId}` update meta (title, passing_score, attempt_limit, duration_min, status, extra_infor)
- **[ ]** `DELETE /api/courses/{courseId}` (tuỳ)
- **[ ]** `GET /api/courses/{courseId}/tree` trả về full structure: sections/pages/blocks/quiz/questions/options/assets

### 4.2 Section/Page CRUD + order

- **[ ]** `POST /api/courses/{courseId}/sections`
- **[ ]** `PATCH /api/sections/{sectionId}` (title, description, order_index, learning_objective, `theme_override`)
- **[ ]** `DELETE /api/sections/{sectionId}`
- **[ ]** `POST /api/sections/{sectionId}/pages`
- **[ ]** `PATCH /api/pages/{pageId}` (title, order_index, page_type, `theme_override`)
- **[ ]** `DELETE /api/pages/{pageId}`

### 4.3 Content authoring

- **[ ]** `POST /api/pages/{pageId}/content-page` tạo `content_page`
- **[ ]** `PATCH /api/content-pages/{pageId}` update `layout_type`
- **[ ]** `POST /api/content-pages/{pageId}/blocks`
- **[ ]** `PATCH /api/content-blocks/{blockId}` (order_index, text_html)
- **[ ]** `DELETE /api/content-blocks/{blockId}`

### 4.4 Quiz authoring

- **[ ]** `POST /api/pages/{pageId}/quiz-page` tạo `quiz_page`
- **[ ]** `PATCH /api/quiz-pages/{pageId}` (passing_score, attempt_allowed)
- **[ ]** `POST /api/questions` tạo question (universal)
- **[ ]** `PATCH /api/questions/{questionId}` update question
- **[ ]** `POST /api/questions/{questionId}/choice-options` (MCQ/TF)
- **[ ]** `POST /api/questions/{questionId}/fill-blanks` (blanks + answers)
- **[ ]** `POST /api/questions/{questionId}/matching` (left/right/pairs)
- **[ ]** `POST /api/questions/{questionId}/short-answers` (expected answers)
- **[ ]** `POST /api/questions/{questionId}/grouping` (groups/items/answers)
- **[ ]** `POST /api/quiz-pages/{pageId}/questions/{questionId}` gắn question vào quiz
- **[ ]** `DELETE /api/quiz-pages/{pageId}/questions/{questionId}`

### 4.5 Theme config APIs

- **[ ]** `PUT /api/courses/{courseId}/scorm-export-config/theme` lưu `theme_config`
- **[ ]** `PUT /api/sections/{sectionId}/theme` lưu `section.theme_override`
- **[ ]** `PUT /api/pages/{pageId}/theme` lưu `page.theme_override`

### 4.6 SCORM export/package APIs (làm sau khi có course tree)

- **[ ]** `POST /api/courses/{courseId}/scorm-packages` tạo package (generate zip + persist)
- **[ ]** `GET /api/scorm-packages` list
- **[ ]** `GET /api/scorm-packages/{packageId}` detail
- **[ ]** `GET /api/scorm-packages/{packageId}/download` tải zip
- **[ ]** `DELETE /api/scorm-packages/{packageId}`

## 5) Notes bắt buộc để export hoạt động

- **[ ]** Rule merge theme khi render page:
  - base: `scorm_package.theme_snapshot` (hoặc `scorm_export_config.theme_config` khi preview)
  - merge: `section.theme_override`
  - merge: `page.theme_override`
- **[ ]** File assets phải có URL/path ổn định để đóng gói vào zip (S3 hoặc local)
- **[ ]** Chuẩn hóa `question_type` enum + validation theo type
