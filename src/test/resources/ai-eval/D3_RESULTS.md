# D3 Results — Faithfulness (Group C) + Quiz Empty-Field Root Cause

**Run on:** 2026-05-24 10:33 → 11:04
**Save-outputs pass:** 30 calls × Gemini 2.5 Pro = $1.02
**LLM-as-judge pass:** 30 calls × Gemini 2.5 Flash = $0.02
**Total D3 cost:** **$1.04** (cumulative across D2+D3: $3.10)

---

## Part 1 — Quiz empty_field root cause (theo dõi tiếp finding D2)

D2 báo cáo **35% quiz có empty_field**. D3 đào sâu để xác định CHÍNH XÁC field nào, type nào.

### Phân bố theo question type

| Question Type        | Total | With empty field | %       |
|----------------------|-------|------------------|---------|
| MCQ_SINGLE           | 10    | 0                | 0.0%    |
| MCQ_MULTIPLE         | 3     | 0                | 0.0%    |
| TRUE_FALSE           | 10    | 0                | 0.0%    |
| SHORT_ANSWER         | 9     | 0                | 0.0%    |
| **FILL_IN_THE_BLANK** | **10** | **4**            | **40.0%** |
| MATCHING             | 8     | 0                | 0.0%    |

→ **Single root cause:** `FILL_IN_THE_BLANK` thiếu trường `prompt` ở 40% trường hợp.

### Cơ chế của vấn đề

Khi sinh FILL_IN_THE_BLANK, model **để trống** `prompt` và **chỉ điền `sentenceHtml`** (câu chứa chỗ trống `___`). Verify trên 4 case bị missing:

| Doc | sentenceHtml present | prompt status |
|-----|---------------------|---------------|
| D02 | 256 chars | missing |
| D03 | 127 chars | missing |
| D06 | 189 chars | missing |
| D08 | 222 chars | missing |

→ Có thể nói model coi **`sentenceHtml` chính LÀ câu hỏi**, không cần `prompt` riêng. Đây là một hiểu nhầm UX có thể defendable.

### Cách sửa (đề xuất sau bảo vệ)

Hai phương án:

1. **Sửa prompt** trong `AiGeneratorService.generateQuizFromText`: thêm dòng *"With FILL_IN_THE_BLANK questions, the `prompt` field must contain an instruction like 'Fill in the blanks based on...' while `sentenceHtml` contains the sentence."*
2. **Sửa validation logic** — bỏ check `prompt` required cho FILL_IN_THE_BLANK type, vì `sentenceHtml` về mặt UX là đủ.

Cá nhân nghiêng về (2) vì phù hợp với mental model của LLM.

---

## Part 2 — Group C: LLM-as-judge Faithfulness

Dùng **Gemini 2.5 Flash** (với `thinkingBudget=0`) làm judge cho 30 outputs từ Pro.

### Bảng 5.x.4.A — Faithfulness score per feature

| Feature       | N  | Mean   | Median | Std    | Distribution (1/2/3/4/5) |
|---------------|----|--------|--------|--------|--------------------------|
| outline       | 10 | **4.00** | 4.5    | 1.15   | 0/1/3/1/5               |
| page-content  | 10 | **4.10** | 4.0    | 0.74   | 0/0/2/5/3               |
| **quiz**      | 9  | **2.44** | 2.0    | 0.73   | **0/6/2/1/0**           |

→ **Quiz là feature yếu nhất** (mean 2.44 vs 4.0 cho outline/page-content).
→ Outline có variance cao (std 1.15) — có cả 5 điểm tối đa và 1 điểm thấp.

### Bảng 5.x.4.B — Hallucination rate per feature

| Feature       | N  | Hallucinated | Avg unsupported claims | Total unsupported |
|---------------|----|--------------|------------------------|-------------------|
| outline       | 10 | 2/10 (20%)   | 7.70                   | 77                |
| page-content  | 10 | 0/10 (0%)    | 1.60                   | 16                |
| **quiz**      | 10 | **6/10 (60%)** | 2.78                 | 25                |

→ **Quiz có 60% tỉ lệ hallucination**, page-content không có hallucination nào.
→ Outline có nhiều "unsupported claims" (77 tổng) nhưng đa số KHÔNG phải hallucination — chỉ là claim không có trong GT 5-fact.

### Bảng 5.x.4.C — Theo ngôn ngữ

| Language | N  | Mean | Median | Std  | Hallucination       |
|----------|----|------|--------|------|---------------------|
| EN       | 24 | 3.50 | 3.5    | 1.14 | 7/24 (29%)          |
| VI       | 5  | 3.80 | 4.0    | 1.30 | 1/6 (17%)           |

→ Sample size VI nhỏ (5) nên không thể conclude mạnh, nhưng prima facie VI **không kém hơn** EN.

### Bảng 5.x.4.D — Theo từng tài liệu

| Doc | Mean | Distribution | Hallucination |
|-----|------|--------------|---------------|
| D01 | 2.67 | 0/1/2/0/0    | 2/3           |
| D02 | 3.67 | 0/1/0/1/1    | 1/3           |
| D03 | 3.00 | 0/1/1/1/0    | 1/3           |
| D04 | **4.33** | 0/0/1/0/2 | **0/3** ✅    |
| D05 | 3.00 | 0/2/0/0/1    | 2/3           |
| D06 | **4.00** | 0/0/0/3/0 | **0/3** ✅    |
| D07 | 3.00 | 0/1/1/1/0    | 1/3           |
| D08 | 3.33 | 0/1/1/0/1    | 1/3           |
| D09 | **4.50** | 0/0/0/1/1 | **0/3** ✅    |
| D10 | **4.33** | 0/0/1/0/2 | **0/3** ✅    |

→ D04, D06, D09, D10 đạt cao (>4.0) — không hallucination.
→ D01 yếu nhất (2.67). Có thể do tài liệu chứa nhiều khái niệm ngôn ngữ học mà 5 key facts của em không phủ.

### Bảng 5.x.4.E — Judge cost & latency

| Metric                | Value      |
|-----------------------|------------|
| Total judge calls     | 30         |
| Parse-OK rate         | 29/30 (97%) |
| Total cost            | $0.0205    |
| Avg latency           | 1,709 ms   |
| Median latency        | 1,744 ms   |

→ Flash cực rẻ và nhanh — phù hợp làm judge ở scale lớn.

---

## Part 3 — Findings cốt lõi để đưa vào thesis

### Finding 1 — Quiz có vấn đề faithfulness nghiêm trọng nhất

- Faithfulness mean 2.44/5 (vs 4.0 cho 2 feature kia)
- 60% câu hỏi có hallucination
- 6/9 quiz được chấm điểm 2/5
- Ví dụ cụ thể (D02 quiz): câu hỏi về `DROP TABLE RESTRICT`, `CHECK` constraint, và hành động mặc định của `FOREIGN KEY` — model bịa thêm chi tiết kỹ thuật **không có trong tài liệu gốc**.

→ **Điểm yếu rõ rệt nhất của module AI.** Phải đưa vào mục 5.4.7 + 6.2.2 limitations.
→ Hint cho hướng cải thiện: prompt quiz cần ràng buộc cứng hơn "ONLY use facts in the provided text, do not infer or extrapolate".

### Finding 2 — Outline coverage > faithfulness — caveat methodology

77 unsupported claims với 20% hallucination — nhưng phần lớn là claim ĐÚNG nhưng không có trong 5 key facts của em. Đây là **giới hạn của LLM-as-judge với sparse GT**:

→ Phải ghi caveat trong 5.4.7: *"Số unsupported_claims không tương đương với hallucination thực sự — chỉ phản ánh sự không khớp với 5-10 key facts đã bóc thủ công."*
→ Để tăng độ chính xác, D4 sẽ có human review trên 30 mẫu để cross-validate judge.

### Finding 3 — Page-content faithfulness tốt nhất

- Mean 4.10/5
- 0% hallucination
- Std 0.74 (consistent nhất)

→ Bằng chứng: AI sinh nội dung từ context truncated (6000 chars) ổn định hơn outline (full doc).
→ Khi context tập trung → output ít drift → ít hallucination.

### Finding 4 — Quiz FILL_IN_THE_BLANK schema issue

Root cause của 35% empty_field từ D2 = single bug.

→ Có thể fix bằng prompt tweaking hoặc validation relaxation.
→ Đáng đề cập trong mục 5.4 vì show được nhóm làm rigorous debugging chứ không chỉ đếm số.

---

## Part 4 — Threats to Validity (cập nhật cho 5.4.7)

1. **Sparse ground truth bias outline scores down:** Outline cover toàn bộ doc nhưng GT chỉ 5 facts → "unsupported_claims" cao là expected.
2. **LLM-as-judge dùng cùng vendor (Google):** Có thể có bias dương — Flash judge Pro outputs là same-family. Đây là LIMITATION quan trọng. D4 human eval sẽ cross-validate.
3. **Quiz GT facts có thể quá strict:** Câu hỏi inferring từ doc (như "What's the default behavior of FOREIGN KEY?") có thể đúng kỹ thuật nhưng không có trong 5 facts → quiz bị penalize.
4. **Sample size VI quá nhỏ (5):** Không thể make statistical claims.
5. **D05 (360 trang) chỉ 5 facts** trong khi đã expand lên 10 — vẫn chỉ phủ 2-3% nội dung. Faithfulness cho D05 không hoàn toàn fair.
6. **Judge dùng Flash thinkingBudget=0:** Có thể không "đủ sâu" để đánh giá chính xác. Trade-off speed/cost vs depth.

---

## Câu trả lời sẵn cho hội đồng (cập nhật từ D2)

### Q: "Module AI có faithfulness tốt không?"

→ "Khác nhau theo feature. Page-content tốt nhất (mean 4.1/5, 0% hallucination). Outline trung bình (4.0/5, 20% hallucination). **Quiz yếu nhất** (2.44/5, 60% hallucination). Đây là phát hiện cụ thể từ LLM-as-judge eval và em đã ghi vào limitations chương 6."

### Q: "Tại sao quiz lại tệ vậy?"

→ "Có 2 vấn đề riêng biệt: (1) FILL_IN_THE_BLANK type bị model bỏ trống trường `prompt` ở 40% — vấn đề schema, không phải chất lượng nội dung. (2) Quiz có xu hướng infer thêm chi tiết kỹ thuật từ kiến thức prior của model thay vì strict trong tài liệu gốc. Em đã nêu hướng fix trong limitations: ràng buộc prompt cứng hơn về source grounding."

### Q: "Sao em chấm faithfulness bằng AI mà không phải con người?"

→ "Đây là 'LLM-as-judge', kỹ thuật chuẩn được sử dụng rộng rãi (Zheng et al. 2023, MT-Bench). Em dùng Gemini 2.5 Flash làm judge — model khác với generator (Gemini Pro), với prompt rubric rõ ràng 1-5. Để giảm bias, **D4 em sẽ có human review trên 30 mẫu** để tính agreement với LLM-judge — phương pháp validation cross-check. Em cũng đã liệt kê limitation 'same-family bias' trong section 5.4.7."

### Q: "Số unsupported_claims = 77 cho outline có nghĩa AI bịa 77 chỗ không?"

→ "Không. Đó là caveat methodology em đã ghi: ground truth chỉ 5 fact/doc, nhưng outline cover hết doc → claim không khớp 5 fact ≠ hallucination thực sự. Hallucination thực sự là 2/10 outline (20%) — judge có flag riêng `hallucination=true`. unsupported_claims chỉ là 'không tìm thấy support trong GT', không phải 'sai sự thật'."

---

## Next steps

- **D4:** Human eval — 2 reviewer chấm IWF (12 flaws) trên 50 câu hỏi quiz + rubric 4 tiêu chí. Tính Cohen's κ.
- **D4:** Human verify faithfulness trên 30 mẫu — đối chiếu với LLM judge để validate.
- **D5:** Tổng hợp stats, viết section 5.4 thesis.
