# D10 — Runtime quality gate cho câu hỏi quiz (IWF + Bloom)

**Date:** 2026-06-07
**Mục tiêu:** Đưa kiểm soát chất lượng câu hỏi từ chỗ *chỉ đo offline* (D2–D9) sang
*chặn + tự sửa ngay lúc sinh* (runtime), cho hai khía cạnh: **Item Writing Flaws (IWF)**
và **độ khó theo thang Bloom**.

---

## Bối cảnh — khoảng trống trước D10

| Khía cạnh | Trước D10 | Cơ chế |
|---|---|---|
| JSON schema | ✅ gate runtime | `QuizSchemaValidator` + retry 1 lần |
| Faithfulness (citation) | ✅ gate runtime | substring verbatim check + retry |
| **IWF (12 flaw)** | ❌ chỉ prompt + đo offline (human + AI scorer) | — |
| **Bloom / độ khó** | ❌ chỉ prompt rubric + Bloom tag thủ công trong scorer | — |

→ Prompt *yêu cầu* tránh IWF và bám Bloom, nhưng **không có gì kiểm tra/ép** output. D10 lấp khoảng trống đó.

## Thay đổi

### 1. Bloom level là first-class trong output
- `AiQuizResponse.AiQuestion` thêm trường `bloomLevel` (1–6).
- Prompt (`QuizPrompts.SCHEMA_CONVENTIONS`) bắt buộc xuất `bloomLevel` khớp độ khó:
  Dễ/Easy→1-2, Trung bình/Medium→3, Khó/Hard→4-5.
- `QuizSchemaValidator.validateBloom`: gate hiện diện + dải [1,6] → vi phạm vào retry.
- ⇒ độ khó giờ **đo được trên từng câu**, không chỉ suy đoán.

### 2. Gate IWF runtime (tập con high-precision của Haladyna 2002)
`QuizSchemaValidator.validateItemWritingFlaws` (lexicon đồng bộ với `ITEM_WRITING_STANDARDS`):

| Flaw | Cách phát hiện (deterministic) |
|---|---|
| **TW-1** Length cue | đáp án đúng ≥ 1.5× độ dài TB distractor (chỉ khi ≥ 6 từ) |
| **TW-5** Absolute terms | distractor chứa "always/never/only/luôn luôn/không bao giờ/tất cả/duy nhất" (bỏ qua đáp án đúng) |
| **ID-2** All/None of the above | option khớp mẫu AOTA (EN+VI) |

Các flaw chủ quan/đa nghĩa (ID-1, ID-3 phủ định, ID-4/5/6, TW-2/3/4/6) **giữ ở prompt + chấm offline**
để tránh false-positive gây retry thừa (đặc biệt "không" trong tiếng Việt rất phổ biến).

### 3. Tích hợp vào vòng retry sẵn có
Mọi issue (schema + citation + Bloom + IWF) gộp vào cùng list → `callQuizWithRetry` đưa vào
addendum và gọi lại model 1 lần để tự sửa; còn lỗi sau retry thì trả best-effort + đếm metric
`ai.quiz.schema_issues`.

## Kiểm chứng (live, Gemini 2.5 Pro, tài liệu Bubble Sort)

| Phép thử | Kết quả |
|---|---|
| `difficulty=Hard` | bloomLevels = [4,4,4,5,4,4], **avg 4.2** (đúng dải 4-5) |
| `difficulty=Easy` (cùng tài liệu) | bloomLevels = [1,2,1,1,1,2], **avg 1.3** (đúng dải 1-2) |
| Bloom hiện diện + đúng dải | 6/6 câu |
| Gate + retry | log: "Quiz schema/citation issues on first call (1); retrying once" → output cuối sạch |

Unit test: `QuizSchemaValidatorTest` 22 test (thêm 6 test IWF/Bloom), `AiGeneratorServiceQuizTest` 4 test — pass.

## Vị trí trong bức tranh đo lường tổng thể
- **Offline (D2–D9):** đo Reliability/Latency/Cost/Faithfulness/IWF/Pedagogical trên 10 tài liệu (human 2 reviewer + κ, AI-assisted scorer). Floor hallucination ~50% (inference extension, 0 contradiction).
- **Runtime (D10):** gate schema + citation + **Bloom + IWF (TW-1/TW-5/ID-2)** + retry — ngăn lỗi tới tay người dùng.

→ Offline = *đánh giá hệ thống*; Runtime = *kiểm soát từng request*. Hai lớp bổ trợ nhau.

## Hạn chế / future work
- Gate IWF mới phủ 3/12 flaw (chọn loại deterministic, ít false-positive). Phần còn lại vẫn dựa prompt + chấm tay.
- Bloom level là **model tự gán** (self-report) — đã ép đúng dải nhưng độ chính xác mức Bloom cần human spot-check; chưa gate "lệch dải so với difficulty" để tránh retry tốn kém.
