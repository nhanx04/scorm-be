# Hiểu toàn diện phần tích hợp LLM trong đồ án (dành cho người mới)

> Tài liệu này giải thích **từ đầu đến cuối** cách module AI trong đồ án hoạt động.
> Đọc theo thứ tự. Phần 0 dạy các khái niệm nền — đọc kỹ phần này thì các phần sau sẽ rất dễ.
> Mục tiêu: sau khi đọc xong, bạn tự kể lại được "AI trong đồ án làm gì, làm thế nào, và vì sao làm vậy".

---

## MỤC LỤC
- [Phần 0 — Các khái niệm nền tảng (đọc trước)](#phần-0--các-khái-niệm-nền-tảng-đọc-trước)
- [Phần 1 — Bức tranh tổng thể: module AI làm gì](#phần-1--bức-tranh-tổng-thể-module-ai-làm-gì)
- [Phần 2 — Cách gọi LLM trong code (nền tảng kỹ thuật)](#phần-2--cách-gọi-llm-trong-code-nền-tảng-kỹ-thuật)
- [Phần 3 — Tính năng 1: Sinh dàn ý khóa học](#phần-3--tính-năng-1-sinh-dàn-ý-khóa-học)
- [Phần 4 — Tính năng 2: Sinh nội dung bài học](#phần-4--tính-năng-2-sinh-nội-dung-bài-học)
- [Phần 5 — Tính năng 3: Sinh quiz (phức tạp & quan trọng nhất)](#phần-5--tính-năng-3-sinh-quiz-phức-tạp--quan-trọng-nhất)
- [Phần 6 — RAG: vì sao & cách hoạt động](#phần-6--rag-retrieval-augmented-generation)
- [Phần 7 — Luồng end-to-end: ghép tất cả lại](#phần-7--luồng-end-to-end-ghép-tất-cả-lại)
- [Phần 8 — Triết lý xuyên suốt: chống "bịa" (anti-hallucination)](#phần-8--triết-lý-xuyên-suốt-chống-bịa-anti-hallucination)
- [Phần 9 — Bảng tra cứu: file/class nào làm gì](#phần-9--bảng-tra-cứu-fileclass-nào-làm-gì)
- [Phần 10 — Cheat-sheet câu hỏi defense](#phần-10--cheat-sheet-câu-hỏi-defense)

---

## Phần 0 — Các khái niệm nền tảng (đọc trước)

Đừng bỏ qua phần này. Mỗi khái niệm có **1 câu định nghĩa dễ hiểu + analogy**.

| Khái niệm | Giải thích siêu dễ hiểu |
|---|---|
| **LLM (Large Language Model)** | Một "bộ não đọc-viết" khổng lồ đã học gần như cả internet. Nó cực giỏi hiểu và tạo văn bản, nhưng **không biết gì về tài liệu riêng của bạn** trừ khi bạn đưa cho nó. Ví dụ: **Gemini** (của Google), GPT (OpenAI), Claude (Anthropic). |
| **Gemini 2.5 Pro** | LLM cụ thể đồ án dùng. "Pro" = bản mạnh. Đồ án gọi nó qua mạng (API) như gọi một dịch vụ. |
| **Prompt** | Tờ "đề bài / chỉ thị" bạn gửi cho LLM. Prompt càng rõ → kết quả càng tốt. Phần lớn công sức tích hợp LLM nằm ở **viết prompt cho khéo**. |
| **Token** | LLM không đọc theo "từ" mà theo "mảnh chữ" gọi là token (≈ ¾ từ tiếng Anh). Quan trọng vì: **trả tiền theo token** và **giới hạn theo token**. |
| **Context window** | "Trí nhớ ngắn hạn" tối đa của model trong **một lần gọi** — tối đa bao nhiêu token nó đọc được cùng lúc. Gemini 2.5 Pro có cửa sổ **rất lớn (~1 triệu token)** → nhồi được cả tài liệu dài. |
| **Temperature** | Núm chỉnh "độ ngẫu nhiên/sáng tạo". **Thấp (0–0.3)** = bám sát, ổn định, ít bịa. **Cao (0.7+)** = bay bổng, đa dạng. Đồ án để **thấp** cho quiz (cần chính xác) và cao hơn cho dàn ý (cần sáng tạo). |
| **Structured output / JSON** | Bình thường LLM trả lời bằng **văn xuôi** (con người đọc). Nhưng code thì cần dữ liệu **có cấu trúc**. JSON là một "biểu mẫu" dạng `{"khóa": "giá trị"}`. Ta **bắt LLM trả lời đúng biểu mẫu JSON** để code đọc/lưu được. |
| **Hallucination (bịa)** | LLM đôi khi "bịa" thông tin nghe rất hợp lý nhưng **không có trong tài liệu nguồn** (hoặc sai). Đây là rủi ro lớn nhất khi dùng LLM cho giáo dục. |
| **Grounding (neo nguồn)** | Kỹ thuật "ép" câu trả lời **bám vào tài liệu nguồn** thay vì để model tự bịa từ kiến thức chung. Là vũ khí chính chống hallucination. |
| **Embedding** | Biến một đoạn văn thành một **dãy số** (gọi là vector) đại diện cho **ý nghĩa** của nó. Mấu chốt: **2 đoạn văn cùng nghĩa → 2 vector gần nhau**. Như "toạ độ ý nghĩa" trên bản đồ. |
| **Vector / pgvector** | Vector = dãy số đó (đồ án dùng 768 số/đoạn). **pgvector** = phần mở rộng của PostgreSQL để **lưu** các vector và **tìm nhanh** vector gần nhất. |
| **RAG (Retrieval-Augmented Generation)** | "Sinh có truy xuất". Thay vì nhồi **cả cuốn** tài liệu cho LLM, ta chỉ **tìm vài đoạn liên quan nhất** (bằng embedding) rồi đưa cho LLM. Tiết kiệm token/chi phí mà vẫn bám nguồn. |
| **Few-shot** | "Dạy bằng ví dụ". Đưa cho LLM **vài ví dụ mẫu** (input → output đúng) ngay trong prompt để nó **bắt chước đúng định dạng/chất lượng**. |
| **Citation verbatim** | "Trích dẫn nguyên văn". Bắt LLM **chép nguyên xi một câu** từ tài liệu làm bằng chứng cho mỗi câu hỏi. Sau đó server **kiểm tra câu đó có thật trong tài liệu không** → bắt được bịa một cách tự động. |
| **Bloom (thang Bloom)** | Thang phân loại **độ khó tư duy** của câu hỏi: 1-Nhớ → 2-Hiểu → 3-Áp dụng → 4-Phân tích → 5-Đánh giá → 6-Sáng tạo. |
| **IWF (Item Writing Flaws)** | Danh sách **12 lỗi kinh điển khi soạn câu trắc nghiệm** (theo Haladyna 2002), ví dụ: đáp án đúng dài hơn hẳn, dùng từ tuyệt đối ("luôn luôn"), có lựa chọn "tất cả đáp án trên"... |
| **Spring AI** | Một thư viện Java giúp gọi LLM dễ dàng, **che giấu chi tiết** của từng nhà cung cấp. Nhờ nó, đổi từ Gemini sang model khác chỉ cần đổi cấu hình, không sửa nghiệp vụ. |

> 💡 **Nếu chỉ nhớ 3 ý:** (1) LLM giỏi viết nhưng dễ **bịa**; (2) cả đồ án xoay quanh việc **ép nó bám tài liệu nguồn** và **trả về JSON đúng cấu trúc**; (3) **RAG** giúp làm điều đó rẻ hơn với tài liệu dài.

---

## Phần 1 — Bức tranh tổng thể: module AI làm gì

Đồ án là nền tảng soạn **bài giảng điện tử chuẩn SCORM**. Module AI **hỗ trợ giảng viên** bằng **3 tính năng sinh nội dung**:

| # | Tính năng | Đầu vào | Đầu ra |
|---|---|---|---|
| 1 | **Sinh dàn ý khóa học** | Mô tả ý tưởng **hoặc** file tài liệu (PDF/DOCX/PPT) | Cấu trúc khóa học: các chương (sections) + bài học (topics) |
| 2 | **Sinh nội dung bài học** | Chủ đề bài học + tài liệu nguồn | Nội dung giảng dạy dạng HTML |
| 3 | **Sinh quiz** | Tài liệu nguồn + chủ đề trọng tâm | Bộ câu hỏi trắc nghiệm (6 loại) |

**Nguyên tắc xuyên suốt:** AI là **"trợ lý có giám sát"** — nó soạn nháp, **giảng viên duyệt lại**. Đồ án **không** tuyên bố AI tự động chính xác 100%.

**Công nghệ:** Java + Spring Boot → **Spring AI ChatClient** → **Gemini 2.5 Pro** (qua API). Phần tìm-đoạn-liên-quan (RAG) dùng **PostgreSQL + pgvector**.

Sơ đồ rất gọn:
```
Người dùng → Backend (Spring) → [chuẩn bị prompt + nguồn] → Gemini 2.5 Pro
                                        ↑
                              (RAG: lấy đoạn liên quan từ pgvector)
Gemini trả JSON → Backend kiểm tra/làm sạch → lưu vào khóa học → đóng gói SCORM
```

---

## Phần 2 — Cách gọi LLM trong code (nền tảng kỹ thuật)

Tất cả nằm trong class **`AiGeneratorService`** ([src/main/java/com/scorm/generator/service/AiGeneratorService.java](../src/main/java/com/scorm/generator/service/AiGeneratorService.java)). Đây là "trái tim" của tích hợp.

### 2.1. ChatClient — cái "điện thoại" gọi Gemini
Khi service khởi tạo, nó dựng một `ChatClient` (của Spring AI) và gắn sẵn một **"vai trò mặc định" (system prompt)**:
> *"Bạn là một chuyên gia thiết kế giáo trình e-learning với 10 năm kinh nghiệm..."*

**System prompt** giống như nói với model "**em hãy nhập vai** một chuyên gia thiết kế khóa học" trước khi giao việc. Nó định hình **giọng văn và góc nhìn** cho mọi câu trả lời.

### 2.2. Ép trả về JSON — `BeanOutputConverter`
Code không muốn văn xuôi, mà muốn **một object Java**. Cách làm:
1. Tạo `BeanOutputConverter<AiQuizResponse>` (hoặc `AiCourseOutline`, `AiPageContentResponse`).
2. Gọi `converter.getFormat()` → nó **tự sinh ra một đoạn hướng dẫn JSON schema** và ta dán vào prompt ("hãy trả về đúng định dạng JSON này").
3. Model trả chuỗi → `converter.convert(chuỗi)` → ra object Java dùng được.

> Analogy: bạn đưa cho model **một tờ biểu mẫu trống** và bảo "điền vào đúng các ô này", thay vì để nó viết tự do.

### 2.3. `stripJsonFence` — chi tiết nhỏ nhưng bắt buộc
Thực tế đo được (trong phần đánh giá): **100% lần** model trả JSON nhưng **bọc trong dấu ```` ```json ... ``` ````** (gọi là markdown fence), dù prompt đã cấm. Nếu không bóc lớp này ra, bộ phân tích JSON sẽ **lỗi 100%**.
→ Hàm `stripJsonFence()` xoá ```` ```json ```` và ```` ``` ```` trước khi parse. **Đây là một bài học thực nghiệm** (không phải lý thuyết): "ép JSON" **không** loại bỏ hoàn toàn lỗi cú pháp, vẫn cần lớp hậu xử lý.

### 2.4. Temperature theo từng việc
- Quiz: temperature **thấp (~0.3)** vì cần **bám sát nguồn, ít bịa**.
- Dàn ý: dùng mặc định cao hơn vì cần **sáng tạo cấu trúc**.

### 2.5. Vì sao chọn Gemini 2.5 Pro (lý do thực dụng)
1. **Context window ~1 triệu token** → nhồi được **cả tài liệu dài** (có cuốn 360 trang) để bám nguồn — đây là lý do **quan trọng nhất**.
2. **Đa phương thức**: đọc được PDF/DOCX/PPT.
3. **Trả JSON tốt** (đo được 100% hợp lệ).
4. **Tiếng Việt tốt** + cùng hệ sinh thái Google có sẵn **embedding cho RAG**.
5. Tích hợp qua Spring AI nên **có thể đổi model** sau này mà không sửa nghiệp vụ.

---

## Phần 3 — Tính năng 1: Sinh dàn ý khóa học

Có **2 cửa** vào tính năng này:

### 3.1. Từ file (`generateCourseOutlineFromFile`)
1. **Đọc file**: dùng `TikaDocumentReader` (thư viện Apache Tika) để **trích text** từ PDF/DOCX/PPT → ghép thành một chuỗi `documentContext`.
2. **Soạn prompt**: "Hãy đọc tài liệu sau và biến thành dàn ý khóa học chi tiết..." + nhồi `documentContext` vào + yêu cầu trả JSON.
3. **Gọi Gemini** → nhận JSON → `convert` thành object `AiCourseOutline` (gồm `title`, `description`, danh sách `sections`).
4. **Đính kèm văn bản gốc** (`sourceDocumentText`) vào kết quả để **lưu lại** — về sau dùng làm nguồn cho việc sinh nội dung/quiz.

> Mẹo nhỏ trong code: dùng một class phụ `AiCourseOutline.Draft` (không chứa văn bản gốc) khi gọi model, để model **không echo (chép lại) cả tài liệu** vào JSON → tiết kiệm token.

### 3.2. Từ mô tả (`generateCourseOutline`)
Không có file — chỉ có thông tin người dùng nhập (tên khóa học, đối tượng, trình độ, thời lượng, mục tiêu...). Prompt yêu cầu model **tự phân bổ số chương/bài** cho logic. Đầu ra cũng là `AiCourseOutline` (nhưng `sourceDocumentText = null` vì không có tài liệu).

### 3.3. Lưu dàn ý
Endpoint `/ai/save-outline` gọi `CourseService.saveAiCourseOutline(...)` để **ghi dàn ý vào CSDL** thành khóa học thật. **Quan trọng:** lúc lưu, nếu có `sourceDocumentText`, hệ thống **bắt đầu index tài liệu cho RAG** (xem Phần 6).

---

## Phần 4 — Tính năng 2: Sinh nội dung bài học

Hàm `generatePageContent` ([AiGeneratorService.java](../src/main/java/com/scorm/generator/service/AiGeneratorService.java)).

**Mục tiêu:** với một bài học (vd "Chương 2 — Định lý Gauss"), sinh **nội dung giảng dạy** bám sát tài liệu khóa học.

**Cách làm:**
1. Prompt yêu cầu viết **có tính sư phạm**: mở đầu dẫn dắt → giải thích khái niệm → ví dụ → tóm tắt.
2. Bắt buộc trả `htmlContent` là **HTML hợp lệ** chỉ dùng thẻ cơ bản (`<h2>, <p>, <ul>, <strong>`...), **cấm** `<script>/<style>/<body>` (an toàn để hiển thị trên trình duyệt).
3. **Khối "neo nguồn"** (`buildPageContentGroundingBlock`): nếu khóa học có tài liệu, chèn vào prompt một khối:
   > *"TÀI LIỆU GỐC (nguồn dữ kiện chính — bám sát, KHÔNG bịa ngoài tài liệu): ... YÊU CẦU: chỉ trình bày kiến thức CÓ trong tài liệu; KHÔNG thêm khái niệm/số liệu ngoài tài liệu..."*

   → Đây chính là **grounding**: ép model viết từ tài liệu, không bịa từ kiến thức chung.
4. **Few-shot** (`buildPageContentFewShotBlock`): chèn 1 ví dụ **tốt** và 1 ví dụ **xấu** (do `PageContentExampleLoader` nạp) để model bắt chước văn phong đúng.

**Điểm tinh tế:** tài liệu nguồn ở đây **không phải lúc nào cũng là cả cuốn** — nó là phần ngữ cảnh do controller chọn (RAG hoặc full-document, xem Phần 6).

---

## Phần 5 — Tính năng 3: Sinh quiz (phức tạp & quan trọng nhất)

Đây là phần **được đầu tư kỹ nhất** vì quiz là nơi **dễ bịa nhất** và **chất lượng câu hỏi** rất quan trọng.

### 5.1. Sinh 6 loại câu hỏi
`AiQuizResponse` chứa danh sách `AiQuestion`, mỗi câu có `type` thuộc 6 loại: **MCQ_SINGLE** (một đáp án), **MCQ_MULTIPLE** (nhiều đáp án), **TRUE_FALSE**, **FILL_IN_THE_BLANK** (điền khuyết), **MATCHING** (ghép nối), **SHORT_ANSWER** (trả lời ngắn). Mỗi loại có "hình dạng" `correctAnswer` khác nhau.

### 5.2. Nguồn dữ kiện — 3 nhánh thông minh (`generateCourseAwareQuiz`)
Người dùng chỉ nhập **chủ đề trọng tâm** (`focusTopic`); **tài liệu nguồn** do backend tự lấy từ CSDL. Code xử lý 3 trường hợp:

| Trường hợp | Xử lý |
|---|---|
| Có **tài liệu** + có **chủ đề** | Dùng prompt `FROM_DOCUMENT_FOCUSED`: ra đề **bám tài liệu, khoanh vùng quanh chủ đề**. (Đầy đủ nhất) |
| Có **tài liệu**, **không** chủ đề | Ra đề **tổng quát** trên toàn tài liệu (prompt `FROM_TEXT`). |
| **Không** tài liệu, có chủ đề | Dùng **chính nội dung người dùng nhập** làm nguồn (`FROM_TEXT`). |
| Không cả hai | **Báo lỗi** — không thể ground → từ chối tạo (tránh bịa hoàn toàn). |

> Vì sao quan trọng: nó đảm bảo **luôn có một nguồn dữ kiện thật** để bám vào, không bao giờ để model "tự chế" từ không khí. (Đây là lý do của một bug cũ tên "Page 6" đã được sửa.)

### 5.3. Prompt quiz — `QuizPrompts` (các "khối" dùng chung)
File `QuizPrompts.java` chia prompt thành các **khối tái sử dụng**, ghép lại bằng template:
- **Anti-hallucination**: "chỉ ra đề từ nội dung có thật; thà ra ít câu còn hơn bịa".
- **Schema conventions**: quy ước JSON cho từng loại câu (+ bắt buộc có trường `bloomLevel`).
- **IWF standards**: luật tránh lỗi soạn đề (đáp án không dài bất thường, không dùng từ tuyệt đối, không "tất cả đáp án trên"...).
- **Difficulty rubric**: ánh xạ độ khó → mức Bloom (Dễ→1-2, Trung bình→3, Khó→4-5).
- **Concept diversity**: quy trình "xác định N khái niệm → mỗi khái niệm 1 câu" để câu hỏi không trùng lặp.
- **Negative examples**: 3 ví dụ **sai** điển hình để model biết đường tránh.

### 5.4. Chống bịa bằng "trích dẫn nguyên văn" (Citation) — ý tưởng hay nhất
Mỗi câu hỏi bắt buộc kèm một `citation` gồm 3 trường:
- `sourceLocation`: vị trí (vd "Slide 5"),
- **`verbatimQuote`: một câu CHÉP NGUYÊN VĂN từ tài liệu** làm bằng chứng,
- `reasoning`: lý do.

Sau đó server **tự kiểm tra**: câu `verbatimQuote` đó **có thật sự xuất hiện trong tài liệu nguồn không** (so khớp chuỗi sau khi chuẩn hoá khoảng trắng). Nếu không → đánh dấu là vấn đề.

> Đây là bước chuyển từ **"tin model"** sang **"verify được bằng máy"**. Người mới nên nhớ câu này khi defense: *"Em bắt model trích nguyên văn rồi server `contains()` kiểm tra — đó là cách phát hiện bịa một cách tất định."*

### 5.5. "Cổng kiểm soát chất lượng" + tự sửa — `callQuizWithRetry`
Đây là cơ chế chạy **ngay lúc sinh** (runtime), không phải đo offline:
1. Gọi Gemini lần 1 → parse JSON.
2. Đưa qua **`QuizSchemaValidator.validate(...)`** — kiểm 4 nhóm:
   - **Schema**: đủ trường bắt buộc cho từng loại câu chưa? (vd điền-khuyết phải có `prompt`).
   - **Citation**: `verbatimQuote` có trong nguồn không? có dùng "..." nối bậy không?
   - **Bloom**: có trường `bloomLevel` và nằm trong [1,6] không?
   - **IWF** (3 lỗi bắt được tự động chắc chắn): **TW-1** (đáp án đúng dài bất thường), **TW-5** (distractor chứa từ tuyệt đối), **ID-2** ("tất cả/không đáp án nào ở trên").
3. **Nếu có vấn đề** → gọi Gemini **lần 2** kèm một "phụ lục sửa lỗi" (liệt kê đúng các vấn đề + yêu cầu sửa). → model **tự sửa**.
4. Kiểm lại; nếu vẫn lỗi → trả "best effort" (bản tốt nhất có thể) và **ghi nhận số liệu** qua **Micrometer** (`ai.quiz.schema_issues`).

> Analogy: như một **biên tập viên tự động** đọc lại đề ngay sau khi AI viết, gạch lỗi, bắt viết lại 1 lần.

Các lỗi IWF **chủ quan** (vd phủ định khó nhận, "không" trong tiếng Việt rất phổ biến) thì **không** bắt tự động (tránh báo lỗi sai) — chỉ nhắc trong prompt + chấm tay khi đánh giá.

---

## Phần 6 — RAG (Retrieval-Augmented Generation)

### 6.1. Vấn đề RAG giải quyết
Tài liệu khóa học có thể **rất dài** (hàng trăm trang). Nhồi cả cuốn vào prompt mỗi lần sinh thì:
- **Tốn token → tốn tiền & chậm**, và có thể **vượt context window**.
- Model dễ bị "lạc giữa biển chữ" (lost-in-the-middle).

**Giải pháp RAG:** chỉ **tìm vài đoạn (chunk) liên quan nhất** tới chủ đề đang sinh, rồi đưa **các đoạn đó** cho model — thay vì cả cuốn.

### 6.2. RAG có 2 PHA (đây là phần dễ nhầm — nhớ kỹ)

**PHA A — Ingest/Index (chạy 1 lần, lúc LƯU khóa học, chạy nền `@Async`):**
```
Tài liệu nguồn → DocumentChunker → GeminiEmbeddingClient → ghi vào pgvector
   (cắt thành chunk ~1200 ký tự,    (mỗi chunk → 1 vector       (kho vector của
    chồng lấp 200)                    768 chiều)                  khóa học)
```
- **`DocumentChunker`**: cắt tài liệu thành các đoạn ~1200 ký tự, **chồng lấp 200** ký tự (để không cắt mất ngữ cảnh ở ranh giới).
- **`GeminiEmbeddingClient`**: gọi API embedding của Google (`gemini-embedding-001`, **768 chiều**) biến mỗi chunk thành 1 vector.
- **`DocumentChunkDao`**: lưu các vector vào bảng `document_chunk` trong PostgreSQL (có **pgvector**). Dùng `JdbcTemplate` (không dùng JPA vì JPA không hiểu kiểu dữ liệu `vector`).

**PHA B — Retrieve + Generate (chạy MỖI LẦN người dùng bấm sinh):**
```
Câu truy vấn (chủ đề bài/quiz) → embed thành vector → tìm top-k chunk gần nhất trong pgvector
                                                          (cosine similarity, toán tử <=>)
→ ghép các chunk đó thành "ngữ cảnh" → đưa vào prompt → Gemini sinh nội dung
```
- Chỉ **câu truy vấn** được embed lúc này; tài liệu đã được embed sẵn ở Pha A.
- `top-k` mặc định **k=6** (lấy 6 đoạn gần nhất).

> **Câu chốt khi defense:** *"Chunk + embed chạy MỘT LẦN lúc lưu khóa học; lúc sinh chỉ embed câu truy vấn rồi lấy 6 đoạn liên quan."*

### 6.3. `CourseRagService` — lớp điều phối RAG
- `ingest(courseId, text)`: làm Pha A (chunk → embed → xoá chunk cũ → ghi mới).
- `retrieveContext(courseId, query, topK)`: làm Pha B (embed query → tìm top-k → ghép văn bản).
- `isEnabled()` / `hasIndex()`: kiểm tra RAG có bật và khóa học đã được index chưa.

### 6.4. **Fail-soft** — triết lý "không bao giờ làm hỏng luồng"
Toàn bộ RAG thiết kế **fail-soft**: nếu RAG bị tắt, chưa index, hoặc lỗi (mất kết nối, API embedding lỗi...) → các hàm trả **kết quả rỗng**, và hệ thống **tự quay về cách cũ: nhồi nguyên tài liệu**. **Không bao giờ ném lỗi làm hỏng việc tạo khóa học.**

Nơi quyết định "dùng RAG hay nhồi cả tài liệu" là `resolveGrounding(...)` trong **`AiFeatureController`**:
```
1. Lấy tài liệu gốc của khóa học (đồng thời kiểm tra quyền sở hữu).
2. Nếu có câu truy vấn + khóa học đã index → thử RAG (top-k).
3. Nếu RAG trả về rỗng → fallback: dùng nguyên tài liệu gốc.
```

### 6.5. Tính năng nào dùng RAG?
- **Có dùng RAG:** sinh **nội dung bài học** (query = tên chương + chủ đề bài) và **quiz có chủ đề trọng tâm** (query = focusTopic).
- **Cố ý KHÔNG dùng RAG:** **sinh dàn ý** và **quiz tổng quát** — vì cần **bao quát toàn bộ** tài liệu, nên nhồi cả tài liệu hợp lý hơn. (Đây là **lựa chọn kiến trúc** có chủ đích, không phải thiếu sót — là một talking point tốt.)

---

## Phần 7 — Luồng end-to-end: ghép tất cả lại

Ví dụ một giảng viên dùng hệ thống từ đầu tới cuối:

1. **Tải tài liệu (PDF) + tạo dàn ý** → `POST /ai/generate-outline-from-file`
   → Tika đọc PDF → Gemini sinh dàn ý JSON → trả về giao diện.
2. **Lưu dàn ý** → `POST /ai/save-outline`
   → Ghi khóa học vào CSDL → **kích hoạt `ingestAsync`** (Pha A của RAG: tài liệu → vector → pgvector) chạy nền.
3. **Sinh nội dung 1 bài** → `POST /ai/generate-page-content`
   → `resolveGrounding` lấy **top-k đoạn liên quan** (RAG) tới "chương + bài" → nhồi vào prompt → Gemini viết HTML bám nguồn → trả về.
4. **Sinh quiz cho khóa học** → `POST /ai/generate-course-quiz`
   → `resolveGrounding` lấy đoạn liên quan tới `focusTopic` → `generateCourseAwareQuiz` → Gemini sinh 6 loại câu kèm citation/Bloom → **`callQuizWithRetry`** kiểm tra + tự sửa 1 lần → trả bộ câu hỏi.
5. Giảng viên **duyệt/chỉnh** → hệ thống **đóng gói SCORM**.

Các endpoint AI nằm trong **`AiFeatureController`** ([src/main/java/com/scorm/generator/controller/AiFeatureController.java](../src/main/java/com/scorm/generator/controller/AiFeatureController.java)).

---

## Phần 8 — Triết lý xuyên suốt: chống "bịa" (anti-hallucination)

Gom lại toàn bộ "vũ khí chống bịa" để bạn thấy bức tranh lớn — **đây là phần hồn của đồ án**:

| Lớp | Cơ chế | Ở đâu |
|---|---|---|
| 1. Prompt | Khối anti-hallucination + grounding ("chỉ dùng kiến thức trong tài liệu") | `QuizPrompts`, `buildPageContentGroundingBlock` |
| 2. Nguồn | Luôn ép có **nguồn dữ kiện thật**; không nguồn → từ chối tạo | `generateCourseAwareQuiz` (3 nhánh) |
| 3. Few-shot | Ví dụ tốt/xấu để model bắt chước đúng | `QuizExampleLoader`, `PageContentExampleLoader` |
| 4. Bằng chứng | **Trích dẫn nguyên văn** + server kiểm tra substring | `citation.verbatimQuote` + `QuizSchemaValidator` |
| 5. Gate runtime | Kiểm Schema + Citation + Bloom + IWF → **tự sửa 1 lần** | `callQuizWithRetry` + `QuizSchemaValidator` |
| 6. Nhiệt độ thấp | Quiz temperature ~0.3 để bám sát | `quizOptions` |
| 7. Định vị sản phẩm | AI là **trợ lý cần giảng viên duyệt** | (triết lý, nói khi defense) |

---

## Phần 9 — Bảng tra cứu: file/class nào làm gì

| File / class | Vai trò |
|---|---|
| `AiGeneratorService` | **Trái tim**: 3 tính năng sinh, gọi Gemini, ép JSON, retry quiz |
| `AiFeatureController` | Các API `/ai/...`; `resolveGrounding` chọn RAG vs nhồi tài liệu |
| `QuizPrompts` | Các khối prompt dùng chung cho quiz (anti-hallu, schema, IWF, Bloom, few-shot...) |
| `QuizSchemaValidator` | Kiểm tra quiz: schema + citation + Bloom + IWF |
| `QuizExampleLoader` / `PageContentExampleLoader` | Nạp ví dụ few-shot |
| `AiQuizResponse` / `AiCourseOutline` / `AiPageContentResponse` | "Biểu mẫu" JSON (DTO) cho từng tính năng |
| `CourseRagService` | Điều phối RAG: ingest (Pha A) + retrieve (Pha B), fail-soft |
| `DocumentChunker` | Cắt tài liệu thành chunk (1200/overlap 200) |
| `GeminiEmbeddingClient` | Gọi API embedding Gemini (768 chiều) |
| `DocumentChunkDao` | Lưu/tìm vector trong PostgreSQL + pgvector |
| `CourseServiceImpl` | Lưu khóa học; kích hoạt `ingestAsync` cho RAG |

---

## Phần 10 — Cheat-sheet câu hỏi defense

**H: Em tích hợp LLM nào, vì sao?**
> Gemini 2.5 Pro qua Spring AI. Lý do chính là **context window ~1M token** để nhồi/bám tài liệu dài; cộng đa phương thức (PDF/DOCX/PPT), trả JSON tốt, tiếng Việt tốt, có sẵn embedding cùng hệ sinh thái. Kiến trúc abstract qua Spring AI nên đổi model dễ.

**H: Làm sao đảm bảo AI không bịa?**
> Nhiều lớp: grounding trong prompt (chỉ dùng tài liệu), luôn ép có nguồn thật, **trích dẫn nguyên văn rồi server kiểm tra substring**, cổng kiểm soát runtime (schema + citation + Bloom + IWF) **tự sửa 1 lần**, và nhiệt độ thấp. Em định vị AI là trợ lý cần giảng viên duyệt.

**H: RAG hoạt động thế nào?**
> 2 pha. Pha A (lúc lưu khóa học, chạy nền): cắt tài liệu → embed 768 chiều → lưu pgvector. Pha B (lúc sinh): embed câu truy vấn → lấy top-6 đoạn gần nhất (cosine) → đưa vào prompt. Nếu RAG lỗi/chưa index → tự fallback nhồi cả tài liệu (fail-soft).

**H: Vì sao không phải tính năng nào cũng dùng RAG?**
> Sinh dàn ý và quiz tổng quát cần **bao quát toàn bộ** tài liệu nên nhồi cả tài liệu hợp lý hơn; nội dung bài học và quiz-theo-chủ-đề mới cần khoanh vùng nên dùng RAG. Đây là lựa chọn kiến trúc có chủ đích.

**H: Bloom và IWF dùng để làm gì?**
> Để **kiểm soát chất lượng câu hỏi**: Bloom đo cấp độ tư duy/độ khó (1–6), IWF là 12 lỗi soạn câu trắc nghiệm chuẩn (Haladyna 2002). Em ép model gắn Bloom và bắt 3 lỗi IWF chắc chắn ngay lúc sinh.

**H: `stripJsonFence` là gì?**
> Model luôn bọc JSON trong ```` ```json ```` dù prompt cấm — đo được 100%. Phải bóc lớp đó ra trước khi parse, nếu không sẽ lỗi 100%. Là bài học thực nghiệm.

**H: Nếu Gemini sập / hết quota thì sao?**
> Phần RAG fail-soft tự fallback. Phần sinh nội dung sẽ báo lỗi cho người dùng; do AI chỉ là **trợ lý**, hệ thống soạn thảo SCORM vẫn dùng được không cần AI.

---

> **Lời khuyên cuối:** khi trình bày, hãy kể theo mạch **"AI dễ bịa → nên em ép nó bám tài liệu (grounding + RAG) và verify được bằng máy (citation + gate) → và em đo lường để chứng minh"**. Đó là sợi chỉ đỏ nối toàn bộ phần tích hợp LLM của đồ án.
