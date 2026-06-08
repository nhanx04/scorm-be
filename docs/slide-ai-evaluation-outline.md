# Dàn ý slide defense — Vì sao Gemini 2.5 Pro & Đánh giá định lượng module AI

> Dùng cho buổi bảo vệ ĐATN. Khớp phong cách Canva hiện tại (card 3 cột, bảng, khối số 01/02/03).
> Số liệu lấy từ `src/test/resources/ai-eval/` (D2–D11 + hold-out + cross-check).

---

## 0. Bối cảnh & vị trí chèn

Deck hiện tại có **mất cân đối chiến lược**: AI là 1 trong 3 điểm bán chính (slide 8, 28, 43) nhưng **không có slide đánh giá AI nào**, trong khi kiểm thử SCORM/LMS rất dày (slide 37–40). Phần AI eval là thứ học thuật/tinh vi nhất trong đồ án — thêm vào sẽ cân bằng lại deck và là vũ khí mạnh nhất.

**Bản đồ chèn:**

| Chèn ở đâu | Nội dung |
|---|---|
| **Nâng cấp slide 27** (đang giới thiệu Gemini 2.5 Pro nhưng không giải thích) | + 1 slide **"Vì sao Gemini 2.5 Pro"** |
| **Sau slide 40** (kết thúc kiểm thử SCORM/LMS), trước Kết luận (41) | Cụm **"Đánh giá định lượng module AI"** (5 slide chính + 3 phụ lục) |

Lý do đặt AI eval trong Mục 4 (Hiện thực–Kiểm thử): hiện Mục 4 chỉ "kiểm thử đóng gói SCORM" — thêm AI eval mới hoàn chỉnh câu chuyện "kiểm thử", và dẫn thẳng vào slide 43 (đang khẳng định "Tích hợp LLM" là ưu điểm) — giờ có số liệu chống lưng.

---

# PHẦN A — Vì sao dùng Gemini 2.5 Pro

> **Thông điệp lõi (phải nói ra miệng):** "Em không chọn model vì nó *nổi tiếng*, mà vì nó *khớp yêu cầu bài toán* — đặc biệt là grounding tài liệu dài — và em đã **đo lường** để chứng minh nó đạt ngưỡng tin cậy."

### Slide A1 — "Vì sao Gemini 2.5 Pro?" (đặt ngay sau slide 27)

**Layout:** tiêu đề + 4 card (2×2) theo phong cách slide 22/28, mỗi card 1 icon + 1 lý do gắn với yêu cầu bài toán.

| Card | Tiêu chí (gắn yêu cầu) | Nội dung trên slide |
|---|---|---|
| 01 | **Context window cực lớn (~1M token)** | Cho phép *nhồi toàn bộ tài liệu nguồn* để bám sát — tài liệu test có cuốn **360 trang**. Model context nhỏ không grounding nổi. ⭐ Lý do quyết định |
| 02 | **Đa phương thức (multimodal) gốc** | Đọc & hiểu trực tiếp **PDF / DOCX / PPT** — đúng định dạng học liệu giảng viên tải lên |
| 03 | **Structured output (JSON) ổn định** | Mọi tính năng parse JSON → đã đo **100% JSON/schema hợp lệ** (60 lượt gọi) |
| 04 | **Tiếng Việt tốt + hệ sinh thái thống nhất** | Corpus nhiều tài liệu Việt; cùng nhà cung cấp có **embedding (`gemini-embedding-001`) cho RAG**; quota/chi phí hợp lý cho sinh viên |

**Dải băng dưới slide (1 dòng):** *"Kiến trúc tích hợp qua Spring AI `ChatClient` → trừu tượng hoá nhà cung cấp, có thể thay model mà không sửa nghiệp vụ (không khoá cứng vendor)."*

**Talk-track (~40s):** "Bài toán cốt lõi của em là *sinh nội dung bám sát tài liệu giảng viên*. Yêu cầu số một vì thế là **cửa sổ ngữ cảnh lớn** — Gemini 2.5 Pro cho phép đưa cả tài liệu hàng trăm trang vào prompt. Thứ hai là đọc tài liệu đa định dạng. Thứ ba, vì em ép đầu ra JSON có schema, model phải tuân thủ cấu trúc tốt — em đã đo được 100%. Cuối cùng, hệ sinh thái Google còn cung cấp luôn embedding cho RAG. Quan trọng: em tích hợp qua Spring AI nên model có thể thay được."

> ⚠️ **Câu hỏi gần như chắc chắn bị hỏi:** *"Sao không dùng GPT-4o / Claude?"*
> **Trả lời trung thực:** "Em **không** chạy bake-off head-to-head — đó là một hạn chế và là future work. Em chọn theo **tiêu chí khớp yêu cầu** (4 điểm trên), và thay vì khẳng định 'Gemini tốt nhất', em **chứng minh bằng định lượng** rằng nó đạt ngưỡng reliability/faithfulness/chi phí cần thiết cho production. Vì kiến trúc abstract qua Spring AI, việc so sánh/đổi model sau này rất dễ."

*(Tùy chọn) Slide A2 phụ lục — bảng so sánh nhanh tiêu chí Gemini 2.5 Pro vs GPT-4o vs Claude theo 4 trục (context, multimodal, JSON, giá) để bật lên khi bị hỏi. Chỉ làm nếu tự tin về số liệu cập nhật.*

---

# PHẦN B — Đánh giá định lượng module AI

> **Spine/khẩu hiệu xuyên suốt (slide mở đầu + nhắc lại ở kết):**
> *"Hệ thống giao việc sinh nội dung cho AI — nhưng AI có đủ tin cậy để giảng viên dùng không? Em chọn cách **ĐO LƯỜNG**, không **KHẲNG ĐỊNH**."*

Cụm này gồm **1 slide divider phụ + 5 slide chính + 3 slide phụ lục (Q&A)**.

### Slide B0 — Mở đầu: Khung đánh giá

**Layout:** trên là 1 câu hỏi nghiên cứu lớn; dưới là dải 5 nhóm metric (badge ngang) + hộp "Tập dữ liệu".

- **Câu hỏi:** *"Module AI có đáng tin, hiệu quả và sư phạm không?"*
- **5 trục đo:** `A. Độ tin cậy` · `B. Độ trễ & Chi phí` · `C. Độ trung thực (chống bịa)` · `D/E. Chất lượng sư phạm & lỗi soạn đề (IWF)` · `F. RAG vs Nhồi tài liệu`
- **Hộp Tập dữ liệu:** *10 tài liệu đa lĩnh vực (CNTT, KHTN, KHXH, tiếng Anh, tiếng Việt) + 3 tài liệu hold-out (chưa từng dùng tune). Model pin `gemini-2.5-pro-preview-05-06`, temp 0.2 → tái lập được.*

**Talk-track:** "Em xây một bộ đánh giá 5 trục, chạy trên 10 tài liệu thật nhiều lĩnh vực, cộng 3 tài liệu lạ để chống overfitting, model pin cố định để tái lập."

### Slide B1 — A + B: Độ tin cậy, Độ trễ & Chi phí

**Layout:** 3 ô số lớn (big-number) trên đầu + 1 bảng nhỏ + 1 "insight box".

**3 big numbers:**
- **100%** JSON parse & schema hợp lệ (60 lượt gọi)
- **~20–25s** độ trễ p50/lượt · **~$0.02–0.06**/lượt
- **0%** trường rỗng *(sau khi sửa)* — phát hiện ban đầu **35%**

**Bảng độ trễ/chi phí:**

| Tính năng | Latency p50 | Cost/lượt |
|---|---|---|
| Sinh dàn ý | ~21s | ~$0.06 |
| Sinh nội dung | ~25s | ~$0.024 |
| Sinh quiz | ~20s | ~$0.017 |

**Insight box (thể hiện độ sắc):** *"100% đầu ra bị bọc markdown fence dù prompt cấm → cần lớp hậu xử lý `stripJsonFence`. Phản chứng định lượng cho giả định 'ép JSON là loại bỏ hết lỗi cú pháp'."*

**Talk-track (~35s):** "Về độ tin cậy: 100% lượt gọi parse JSON và đúng schema. Em phát hiện model **luôn** bọc JSON trong code fence nên phải có lớp bóc tách — đây là một quan sát định lượng, không phải giả định. Em cũng từng đo 35% câu quiz rỗng trường, truy ra do loại Điền-khuyết, đã sửa về 0%. Chi phí ~2 cent/lượt, độ trễ ~20 giây — chấp nhận được cho thao tác soạn bài."

### Slide B2 — C: Độ trung thực & "trần hallucination"

**Layout:** biểu đồ đường/cột "hành trình GT" bên trái + "phân rã 50%" + so sánh literature bên phải.

**Hành trình mở rộng ground-truth (5 → 15 → 25 facts/tài liệu):**

| | 5-fact | 15-fact | 25-fact |
|---|---|---|---|
| Faithfulness TB | 1.70 | 3.10 | **3.20** |
| Tỉ lệ hallucination | 100% | 50% | **50% (trần)** |

**Phân rã 50% còn lại (đọc tay 5 ca):** *100% là **inference-extension** (model dùng kiến thức nền đúng) — **0 ca mâu thuẫn nguồn** → tỉ lệ bịa sai sự thật thực tế ước tính **0–10%**.*

**Đặt cạnh literature:** ChatGPT MCQ 30–40% (Kıyak 2024) · RAG-SOTA 5–10% (Lewis 2020) · **Mình (pure-gen, narrow-GT) ~50% — tương đương baseline.**

**Talk-track:** "Em đo độ bịa bằng LLM-judge đối chiếu key-facts. Khi mở rộng ground-truth 5→15→25, tỉ lệ chạm **trần 50%** — và em **chứng minh** đây là giới hạn phương pháp chứ không phải hand-wave. Quan trọng: em đọc tay 5 ca còn lại, **không ca nào mâu thuẫn nguồn** — toàn là suy luận mở rộng đúng. Nên tỉ lệ bịa-sai thực tế chỉ 0–10%. Em định vị AI là **trợ lý cần giảng viên duyệt**, không phải tự động tuyệt đối."

### Slide B3 — D/E + Runtime gate: Chất lượng câu hỏi

**Layout:** bên trái "trước→sau" (2 big numbers); bên phải sơ đồ pipeline gate.

**Kết quả hold-out (3 tài liệu lạ, chuẩn IWF Haladyna 12 lỗi):**
- Tỉ lệ câu **không lỗi soạn đề: 68% → 94.4%** *(mean lỗi/câu 0.38 → 0.06)*
- Đạt cả 4 tiêu chí sư phạm: **17/18 (94.4%)**

**Sơ đồ "Runtime Quality Gate" (mũi tên):**
`Sinh quiz → Kiểm tra [Schema · Citation trích nguyên văn · Bloom 1–6 · IWF TW-1/TW-5/ID-2] → Lỗi? → Tự sửa (retry 1 lần) → Trả kết quả` *(đếm metric qua Micrometer)*

**Talk-track:** "Không chỉ đo offline, em đưa kiểm soát chất lượng vào **runtime**: mỗi câu quiz bị kiểm tra schema, trích dẫn nguyên văn từ nguồn, mức Bloom, và các lỗi soạn đề kinh điển; lỗi thì model tự sửa. Trên tài liệu **chưa từng thấy**, tỉ lệ câu không lỗi tăng từ 68% lên 94%."

> **Q&A thủ sẵn:** *"Ai chấm IWF?"* → "Do hạn chế thời gian, em dùng **AI-assisted scoring 2 góc nhìn** trên cùng model, **đã disclose rõ** đây không phải human inter-rater; phần lỗi khách quan (citation, schema, Bloom range) do validator tự động bắt. Human rescoring là future work." *(đừng nói '2 người chấm')*

### Slide B4 — F: RAG vs Nhồi tài liệu (đóng góp định lượng nổi bật nhất) ⭐

**Layout:** 2 big numbers thắng đậm + 1 biểu đồ "tiết kiệm tăng theo độ dài" + 1 dòng đánh đổi trung thực.

**Kết quả (page-content, 10 tài liệu × 3 lần = N=30, kiểm định Wilcoxon paired):**
- **−68.6% token đầu vào** (p = 0.004) · **−55.7% chi phí** (p = 0.002) — *có ý nghĩa thống kê*
- **Tiết kiệm tăng theo độ dài tài liệu:** −41% (≤40 trang) → **−83% (>100 trang)** → *khả năng mở rộng*

**Dòng đánh đổi (trung thực, in nghiêng):** *"Đổi lại: faithfulness giảm nhẹ (−17%, p=0.027) do top-k đôi khi bỏ sót ngữ cảnh — giảm được bằng tăng top-k/reranking. Đây là **đánh đổi hiệu quả↔độ phủ**, không giấu."*

**Talk-track (~45s):** "Đóng góp định lượng em tâm đắc nhất: so sánh **trong cặp** giữa nhồi cả tài liệu và truy xuất top-k đoạn (RAG). RAG giảm **gần 70% token và hơn nửa chi phí**, kiểm định Wilcoxon cho thấy **có ý nghĩa thống kê**, và mức tiết kiệm **càng lớn với tài liệu càng dài** — tức hệ thống mở rộng được. Em trung thực: chất lượng bám nguồn giảm nhẹ vì top-k đôi khi bỏ sót, và em chỉ ra cách khắc phục."

### Slide B5 — Tổng kết & tính liêm chính phương pháp (chốt)

**Layout:** 3 takeaway + 1 "integrity box" + 1 dòng future work.

**3 takeaway:**
1. **Tin cậy:** 100% schema; chất lượng câu hỏi 94% không lỗi trên tài liệu lạ; có gate tự sửa runtime.
2. **Trung thực:** tỉ lệ bịa-sai thực ~0–10% (0 ca mâu thuẫn nguồn); AI định vị là *trợ lý có giám sát*.
3. **Hiệu quả:** RAG −69% token / −56% chi phí (significant), mở rộng theo độ dài.

**Integrity box (điểm cộng lớn với hội đồng):** *"Faithfulness do AI-judge chấm → em **disclose rõ** không phải human, và **kiểm chứng chéo bằng model thứ hai**: đồng thuận cờ hallucination κ = 1.00, điểm 100% trong ±1 (QWK 0.66). Judge không phải outlier."*

**Future work (1 dòng):** *human rescoring · tăng top-k/reranking · bake-off đa model.*

**Talk-track chốt:** "Tóm lại: AI module **đáng tin, sư phạm, và hiệu quả** — và em đo lường thay vì khẳng định. Điều em tự hào là **sự trung thực phương pháp**: chỗ nào dùng AI chấm em nói rõ, và kiểm chứng chéo bằng model khác. Những hạn chế còn lại em đã xác định thành future work."

---

## Phụ lục (slide ẩn — bật khi Q&A)

| Slide | Nội dung | Dùng khi bị hỏi |
|---|---|---|
| P1 | **Bảng Wilcoxon đầy đủ** (token/cost/latency/faith, p-value, effect size, cả page-content & quiz) | "Có ý nghĩa thống kê không / latency thì sao?" (latency p=0.08 → *xu hướng, chưa significant* — nói thẳng) |
| P2 | **Bảng per-doc RAG** (10 tài liệu, token giảm + faith S→R) + ghi chú D04 lỗi đã loại | "Có tài liệu nào RAG thua không?" (D08 nhỏ +7% token; D05/D07 faith giảm) |
| P3 | **Bảng cross-check chéo judge** (10 mẫu, Gemini vs Claude, κ/QWK/±1) | "Sao tin LLM-judge?" |

---

## Gợi ý thời lượng & nhịp

- Phần A (Gemini): **~45s**, 1 slide.
- Phần B: **~3.5–4 phút**, 5 slide (B0 nhanh, B2 & B4 là 2 slide "đinh" dành nhiều thời gian nhất).
- **Quy tắc vàng:** mỗi slide chính **1 thông điệp + 1–2 con số lớn**, không nhồi bảng. Bảng chi tiết để ở phụ lục.
- **Hai slide phải kể thật hay:** B2 (trần hallucination → trung thực) và B4 (RAG significant → đóng góp). Đây là chỗ ghi điểm.

---

# PHẦN C — Dùng AI để design slide (hướng dẫn)

## C.1. Sự thật về Claude & slide

Claude (kể cả Claude Code này) **không render hình ảnh/slide trực tiếp**. Nhưng Claude rất mạnh ở 3 việc đầu nguồn — vốn quyết định 80% chất lượng deck:
1. **Nội dung & cấu trúc** (chính là file này).
2. **Script nói + speaker notes + Q&A.**
3. **Sinh "nguyên liệu" cho công cụ design:** text để dán vào Canva, **code slide** (Marp/Slidev/reveal.js), **script Python tạo .pptx**, hoặc **biểu đồ matplotlib từ CSV thật**.

## C.2. Ba luồng dùng Claude (chọn theo nhu cầu)

| Luồng | Cách làm | Hợp khi |
|---|---|---|
| **A. Claude → Canva (khuyến nghị cho bạn)** | Claude xuất text chuẩn từng card/bảng → dán vào Canva, nhân bản slide mẫu (vd slide 28) và đổ nội dung → giữ đồng bộ thương hiệu | Đã có deck Canva 46 slide, cần đồng bộ |
| **B. Claude sinh "deck as code"** | Claude viết **Marp** hoặc **Slidev** (markdown → slide), render ra PDF/PPTX/HTML | Muốn slide kỹ thuật, có code/diagram, kiểm soát hoàn toàn |
| **C. Claude sinh `.pptx` từ CSV** | Claude viết script `python-pptx` + `matplotlib` đọc thẳng CSV trong `ai-eval/` → tạo slide + biểu đồ số liệu thật | Muốn biểu đồ chính xác từ dữ liệu, file PPTX sửa được |

> 💡 **Mẹo ăn điểm:** nhờ Claude sinh **biểu đồ từ CSV thật** (matplotlib) cho slide B2 (hành trình GT) và B4 (RAG tiết kiệm theo độ dài), xuất PNG nền trong suốt rồi thả vào Canva — vừa đẹp vừa đúng số liệu, hơn hẳn biểu đồ vẽ tay.

## C.3. Công cụ AI design slide tốt nhất (xếp theo thẩm mỹ + phù hợp)

1. **Canva (Magic Design / Canva AI)** — ⭐ *nên dùng vì deck của bạn đã ở Canva.* Sinh slide theo brand kit, giữ font/màu đồng bộ 46 slide cũ. Dán dàn ý phần A/B vào, chọn template gần với slide hiện có.
2. **Gamma (gamma.app)** — đẹp nhất cho tốc độ: dán cả outline markdown này → ra deck chỉnh chu trong vài phút, export PPTX/PDF. Nhược: phong cách hơi khác Canva nên cần restyle cho khớp.
3. **Plus AI** (add-on Google Slides / PowerPoint) — sinh & sửa slide *ngay trong* Google Slides/PPT, giữ định dạng gốc. Hợp nếu chuyển deck sang Google Slides.
4. **Beautiful.ai** — auto-layout theo "design rules", luôn cân đối; hợp slide nhiều bảng/khối số như phần đánh giá.
5. **Microsoft Copilot in PowerPoint** — nếu có M365: sinh slide từ outline + Designer tự đẹp.
6. **Slidev / Marp** (mã nguồn mở, markdown→slide) — đẹp kiểu tech-talk, render code/diagram tốt; Claude viết được toàn bộ. Hợp dân CS.

## C.4. Khuyến nghị cụ thể cho trường hợp của bạn

- **Giữ deck chính ở Canva** (đồng bộ với 46 slide) → tạo các slide A/B bằng cách **nhân bản slide mẫu có sẵn** (slide 28 cho card 3 cột, slide 7/10/32 cho bảng, slide 22 cho khối 01–04) rồi đổ nội dung từ file này.
- **Biểu đồ:** để Claude sinh PNG từ CSV (B2, B4) → thả vào Canva.
- **Tránh:** để AI tự generate cả deck mới từ đầu (sẽ lệch brand, mất công chỉnh hơn là tự dựng từ template cũ).

## C.5. Prompt mẫu để nhờ AI (Gamma/Canva AI/Claude)

```
Tạo [N] slide thuyết trình bảo vệ đồ án, phong cách tối giản học thuật, tông màu
[màu deck hiện tại], mỗi slide 1 thông điệp + tối đa 2 con số lớn. Dựa trên dàn ý
sau (giữ nguyên số liệu, không bịa thêm): [dán nội dung slide A1 / B0–B5].
Ngôn ngữ: tiếng Việt. Ưu tiên card 3 cột và bảng gọn.
```
