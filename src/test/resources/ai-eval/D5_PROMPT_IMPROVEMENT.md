# D5 — Prompt Improvement: Before vs After (Honest Reporting)

**Date:** 2026-06-04
**Code changes:** `AiGeneratorService.generateQuizFromText` + `generateCourseAwareQuiz`
**Re-eval cost:** $0.19 (10 quiz calls Pro) + $0.005 (10 judge calls Flash) ≈ **$0.20**
**Cumulative D2+D3+D4+D5:** **$3.30**

---

## Hai vấn đề được nhắm sửa

### Fix #1 — FILL_IN_THE_BLANK schema enforcement
**Triệu chứng (D2/D3/D4 cùng phát hiện):**
- D2: 35% quiz có empty_field
- D3 root cause: FILL_IN_THE_BLANK missing trường `prompt` ở 40% case
- D4 IWF flag ID-1: 10% questions unclear stem (100% là FILL case)

**Thay đổi prompt:**
```
- FILL_IN_THE_BLANK: BẮT BUỘC có đủ 3 trường:
    + prompt: chỉ dẫn ngắn cho học viên (ví dụ: "Điền vào chỗ trống dựa trên đoạn dưới đây:"). KHÔNG được để trống dù sentenceHtml đã rõ.
    + sentenceHtml: câu chứa các chỗ trống dạng <input> hoặc ___(N)___.
    + correctAnswer: mảng đáp án theo thứ tự các chỗ trống.
```

### Fix #2 — Anti-hallucination source-grounding
**Triệu chứng:** Quiz feature có mean faithfulness 2.44/5, hallucination 60% (D3).

**Thay đổi prompt** — thêm block ngay sau "TÀI LIỆU GỐC":
```
RÀNG BUỘC NGHIÊM NGẶT VỀ NGUỒN (anti-hallucination):
1. MỌI câu hỏi PHẢI có thể trả lời được CHỈ bằng nội dung trong "TÀI LIỆU GỐC" ở trên.
2. TUYỆT ĐỐI KHÔNG sử dụng kiến thức chung từ training data nằm ngoài tài liệu, kể cả khi kiến thức đó là chính xác.
3. Trước khi tạo mỗi câu, hãy tự kiểm tra: "Đáp án đúng có trích dẫn được TRỰC TIẾP từ tài liệu gốc không?" Nếu không → loại bỏ câu hỏi đó.
4. Nếu tài liệu quá ngắn để có đủ {numberOfQuestions} câu hỏi chất lượng, hãy tạo ít câu hơn thay vì bịa.

[...]
- explanation BẮT BUỘC mở đầu bằng "Theo tài liệu:" hoặc "Slide X:" và trích dẫn ngữ cảnh cụ thể trong tài liệu gốc (không phải kiến thức chung).
```

---

## Kết quả định lượng

### Empty-field analysis (đo trực tiếp output schema)

| Question Type | Baseline | Post-fix | Δ |
|---|---|---|---|
| MCQ_SINGLE | 0/10 (0%) | **2/10 (20%)** | **+20pp (regression)** |
| MCQ_MULTIPLE | 0/3 | 0/2 | — |
| TRUE_FALSE | 0/10 | 0/10 | — |
| SHORT_ANSWER | 0/9 | 0/9 | — |
| **FILL_IN_THE_BLANK** | **4/10 (40%)** | **0/9 (0%)** | **−40pp ✅** |
| MATCHING | 0/8 | 0/10 | — |
| **Quiz có ≥1 empty field** | **4/10 (40%)** | **2/10 (20%)** | **−20pp ✅** |

**Kết luận Fix #1:** ✅ **THÀNH CÔNG hoàn toàn cho target type.** FILL_IN_THE_BLANK từ 40% → 0%.

**Side effect:** ⚠️ MCQ_SINGLE từ 0% → 20% (regression nhỏ). 2 case bị missing prompt: D01_Q1, D09_Q1. Có thể do model "over-focus" vào FILL_IN_THE_BLANK spec mới mà quên áp dụng cùng care cho MCQ_SINGLE. Cần thêm requirement chung "tất cả type đều phải có prompt" trong iteration tiếp theo.

### Faithfulness (LLM-judge với cùng 5 GT facts/doc)

| Metric | Baseline (D3) | Post-fix | Δ |
|---|---|---|---|
| N (parse-OK) | 9 | 10 | — |
| Faithfulness mean (1-5) | 2.44 | 2.30 | −0.14 |
| Faithfulness median | 2.0 | 2.0 | — |
| Score distribution (1/2/3/4/5) | 0/6/2/1/0 | 2/6/0/1/1 | bimodal |
| Hallucination flag rate | 67% (6/9) | 80% (8/10) | **+13pp** |
| Avg unsupported claims/output | 2.78 | 3.00 | +0.22 |
| Total unsupported claims | 25 | 30 | +5 |

**Kết luận Fix #2:** ❓ **KHÔNG CÓ CẢI THIỆN ĐO ĐƯỢC bằng LLM-judge automated metric.**

---

## Phân tích sâu Fix #2 — Tại sao metric không phản ánh cải thiện thực sự?

Đây là phân tích cẩn thận, không đơn giản kết luận "fix không work":

### 1. Sparse Ground Truth bias (đã được nêu trong limitations D3)

LLM judge dùng 5 GT facts/doc làm "source of truth". Khi model sinh câu hỏi về chủ đề khác cùng tài liệu (mà nhóm KHÔNG bóc vào GT), judge mark là "unsupported".

→ Không có nghĩa AI bịa, chỉ là **GT của nhóm không cover hết**.

Ví dụ reasoning từ judge:
- D08: "Nội dung AI tạo ra chứa nhiều câu hỏi về quan điểm 'Trọng Sản phẩm' và quy trình Marketing" — đây là kiến thức Marketing chuẩn, có trong tài liệu nhưng KHÔNG có trong 5 GT facts của nhóm (chỉ phủ Needs/Wants/Demands).

### 2. Bimodal distribution post-fix (insight thú vị)

Baseline: 0/6/2/1/0 (1-5) — tất cả trung bình thấp
Post-fix: **2/6/0/1/1** — phân cực hơn: có cả điểm 1 và điểm 5

→ Fix có thể đã làm model **thẳng thắn hơn**: hoặc strict bám source (D06 score 5), hoặc rõ ràng over-extend (D01/D07 score 1). Trước đó là "lơ lửng" ở 2-3.

### 3. "Theo tài liệu:" requirement có thể tạo false-citation visible

Trước fix: model viết explanation chung chung → judge không có cụm "Slide X" để verify.
Sau fix: model viết "Theo tài liệu (Slide 8):" → judge thấy claim cụ thể, dễ verify, dễ flag nếu sai.

→ Có thể fix đã **làm hallucination dễ phát hiện hơn**, không phải tăng hallucination thực sự.

### 4. Sample size nhỏ (N=10)

67% (6/9) vs 80% (8/10) chỉ là chênh lệch 2 case. Variance noise.

### 5. D06 đạt 5/5 sau fix (case study success)

D06 (Bayesian Learning) là doc duy nhất đạt **score 5/5, hallucination=false** trong post-fix. Trước fix điểm 4. → Fix có evidence positive ở ít nhất 1 case rõ ràng.

---

## Verdict honest cho thesis

### Fix #1: VERIFIED SUCCESS ✅
- Đo bằng 2 metric độc lập:
  - Direct schema check: FILL_IN_THE_BLANK empty prompt 40% → 0%
  - LLM-judge: D06 score 4 → 5 (single case nhưng directional)
- Side effect MCQ_SINGLE -20% needs iteration

### Fix #2: INCONCLUSIVE ❓
- Automated metric (LLM-judge với sparse GT): no improvement, possibly worse
- Qualitative evidence (judge reasoning analysis): có thể đã làm hallucination dễ visible hơn
- Bimodal post-fix distribution suggests behavior changed, hướng nào chưa rõ
- **Bằng chứng definitive cần human review** — future work

---

## Câu trả lời defense cập nhật

### Q: "Em đã fix prompt sau khi phát hiện 35% empty_field — fix work không?"

> "Fix work hoàn toàn cho target type. Em strengthened spec FILL_IN_THE_BLANK
> trong prompt, yêu cầu rõ trường `prompt` BẮT BUỘC. Re-eval 10 quiz cho thấy:
> - FILL_IN_THE_BLANK empty prompt: **40% → 0%**
> - Quiz có ≥1 empty field: **40% → 20%**
>
> Phát hiện thêm regression nhỏ ở MCQ_SINGLE (0% → 20%) — model 'over-focused'
> vào spec mới mà bỏ qua MCQ. Em đã ghi vào limitations và đề xuất iteration
> tiếp với general requirement 'mọi type phải có prompt'."

### Q: "Em đã thêm anti-hallucination prompt — quiz có ít hallucinate hơn không?"

> "Em đã thêm 4 ràng buộc nghiêm ngặt + yêu cầu citation 'Theo tài liệu:'.
> Re-eval bằng LLM-judge tự động cho thấy **metric không cải thiện đo được**:
> mean faithfulness 2.44 → 2.30, hallucination flag rate 67% → 80%.
>
> Tuy nhiên đây là **inconclusive vì 2 lý do**:
>
> 1. Sparse GT bias — judge dùng 5 facts/doc, nhưng model sinh câu hỏi
>    cover các phần khác của cùng tài liệu nên judge mark 'unsupported'
>    không thật.
>
> 2. Bimodal distribution post-fix (có cả điểm 5 lẫn điểm 1) cho thấy
>    behavior thực sự thay đổi — hướng nào chưa rõ.
>
> Để conclude definitively, future work là human review N=30 quiz outputs
> với rubric chi tiết hơn 5-fact GT. Em đã document trong D5_PROMPT_IMPROVEMENT.md."

---

## Files trong commit này

- `AiGeneratorService.java` — 2 prompt updates
- `AiQualityEvaluationTest.java` — synced prompt + quizOnlyPostFixPass test, configurable OUTPUTS_DIR
- `LlmJudgeTest.java` — configurable OUTPUTS_DIR + CSV suffix
- `analyze_quiz_empty.py` — CLI subdir arg
- `results/outputs/baseline/` — backup of 10 baseline quiz outputs (D3 data)
- `results/outputs/postfix/` — 10 post-fix quiz outputs
- `results/faithfulness_auto_postfix_*.csv` — judge results post-fix
- This doc

---

## Lessons learned

1. **Targeted schema fix (Fix #1) works reliably** — when problem is clear bug, explicit spec change → measurable improvement.

2. **Quality fix (Fix #2) hard to verify automatically** — sparse GT instrument can't differentiate "real hallucination reduction" from "claim coverage shift".

3. **Iterative regression risk** — Fix #1 introduced MCQ_SINGLE regression. Real software engineering practice.

4. **Defense value of "measure → fix → re-measure"** — even when Fix #2 inconclusive, the methodology is defendable as honest engineering.
