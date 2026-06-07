# Hold-out evaluation — IWF + Pedagogical (after D10 gate)

**Date:** 2026-06-07
**Mục tiêu:** Đo chất lượng câu hỏi trên **tài liệu CHƯA TỪNG dùng để tune** (chống overfitting),
lấy số "after" so với baseline D4 (68% flaw-free trên 10 tài liệu dev, pre-gate).

## Hold-out set (tiếp nối D01–D10)
| ID | Tài liệu | Lĩnh vực | Lang | Tika chars |
|----|----------|----------|------|-----------|
| D11 | Ch2 Data Streaming | CS / Data Eng | EN | 17,067 |
| D12 | Những khái niệm chung về Nhà nước | Luật / Chính trị | **VI** | 14,173 |
| D13 | Delta Lake | CS / Data Eng | EN | 95,433 |

18 câu (6/doc), sinh qua đúng đường production (outline → course → `generate-course-quiz`, difficulty=Mixed),
output là **DELIVERED** (sau gate + retry).

## ⚠️ Disclosure phương pháp
Chấm bằng **AI-assisted single-pass** (Claude, strict-leaning, flag khi có nghi ngờ hợp lý) — **KHÔNG phải human inter-rater**.
Cùng tinh thần với D4 (xem [D4_METHODOLOGY.md](D4_METHODOLOGY.md)). Khuyến nghị human verify trước khi đưa số vào bảo vệ.
Phần deterministic (TW-1, TW-5, ID-2, citation verbatim, schema, Bloom range) lấy từ `QuizSchemaValidator` (khách quan).

## Chấm từng câu

| Câu | Type | Bloom | IWF flaw | C1 | C2 | C3 | C4 | Ghi chú |
|-----|------|-------|----------|----|----|----|----|---------|
| D11_Q1 | MCQ_S | 2 | — | ✓ | ✓ | ✓ | ✓ | clean |
| D11_Q2 | MCQ_M | 3 | — | ✓ | **✗** | ✓ | **✗** | "Maintained by Confluence" gán đúng nhưng KHÔNG có trong quote nguồn + sai tên (Confluent) → lỗi *correctness/grounding* mà gate deterministic không bắt được |
| D11_Q3 | T/F | 1 | — | ✓ | ✓ | – | ✓ | clean |
| D11_Q4 | SHORT | 4 | — | ✓ | ✓ | – | ✓ | Bloom over-tag (recall sendfile() ≈ L1, gán 4) |
| D11_Q5 | FILL | 3 | — | ✓ | ✓ | – | ✓ | clean |
| D11_Q6 | MATCH | 3 | — | ✓ | – | – | ✓ | clean |
| D12_Q1 | T/F | 2 | — | ✓ | ✓ | – | ✓ | clean |
| D12_Q2 | MCQ_S | 3 | — | ✓ | ✓ | ✓ | ✓ | clean (4 cơ quan nhà nước đồng nhất) |
| D12_Q3 | FILL | 3 | — | ✓ | ✓ | – | ✓ | clean |
| D12_Q4 | MATCH | 3 | — | ✓ | – | – | ✓ | clean |
| D12_Q5 | MCQ_M | 4 | — | ✓ | ✓ | ✓ | ✓ | distractor huyết thống / sở hữu chung — hợp lý |
| D12_Q6 | SHORT | 4 | — | ✓ | ✓ | – | ✓ | Bloom over-tag (nêu tên nguyên tắc ≈ L1) |
| D13_Q1 | MCQ_S | 4 | **TW-1** | ✓ | ✓ | ✓ | ✓ | đáp án đúng dài (~15 từ) vs distractor (~9–10) → length cue lọt qua best-effort |
| D13_Q2 | MCQ_M | 3 | — | ✓ | ✓ | ✓ | ✓ | clean |
| D13_Q3 | T/F | 3 | — | ✓ | ✓ | – | ✓ | clean |
| D13_Q4 | SHORT | 3 | — | ✓ | ✓ | – | ✓ | clean |
| D13_Q5 | FILL | 2 | — | ✓ | ✓ | – | ✓ | clean (2 chỗ trống, grounded đủ) |
| D13_Q6 | MATCH | 3 | — | ✓ | – | – | ✓ | clean |

(C3 chỉ áp cho MCQ; "–" = không áp dụng cho loại đó.)

## Aggregate

### IWF (12-flaw)
| Metric | Hold-out (N=18, after) | Baseline D4 (N=50, before) |
|---|---|---|
| **Flaw-free rate** | **17/18 = 94.4%** | 68.0% |
| Mean flaws/item | **0.06** | 0.38 |
| Severely flawed (≥2) | 0% | 4.0% |
| Flaw xuất hiện | TW-1 ×1 | ID-6 12%, ID-1 10%, TW-1 8%, … |

ID-1 (top-2 baseline = FILL thiếu `prompt`) = **0** trên hold-out → đã bị schema gate chặn. ID-6 = 0 (N nhỏ).

### Pedagogical (4 tiêu chí)
| Tiêu chí | Pass |
|---|---|
| C1 Clarity | 18/18 (100%) |
| C2 Single-correct | 17/18 (94.4%) |
| C3 Plausible distractors | áp dụng 6 MCQ → 6/6 |
| C4 Source-grounded | 17/18 (94.4%) |
| **All-4-pass** | **17/18 (94.4%)** |

### Bloom (độ khó)
Present + đúng dải: **18/18**. Phân bố (Mixed): L1×1, L2×3, L3×10, L4×4 (mean 2.9 — tâm ở Apply).
⚠️ Self-tag có **over-rating ~2/18** (câu recall gán L4). Difficulty control đúng hướng (Easy↔Hard tách bạch, xem D10) nhưng độ chính xác mức/câu cần human check.

### Reliability & grounding (deterministic)
- Schema hợp lệ: 18/18. Citation verbatim grounded: **17/18** (1 lọt qua best-effort sau retry).
- Gate kích hoạt 3 lần trên 3 doc lạ (1 trả best-effort còn 2 lỗi) → gate generalize, retry không hoàn hảo.

## Kết luận trung thực
- **After 94.4% flaw-free vs before 68%** — cải thiện rõ, **trên tài liệu chưa từng thấy** (chống overfitting). Mean flaws 0.38 → 0.06.
- **Nhưng** so sánh không hoàn toàn apples-to-apples: N khác (50 vs 18), doc khác, và một phần cải thiện là **by construction** (gate ép TW-1/5/ID-2/schema; ID-1 do schema gate).
- **Lỗi gate KHÔNG bắt được:** D11_Q2 sai *correctness/grounding* ngữ nghĩa ("Maintained by Confluence") + 1 TW-1 lọt qua best-effort → minh chứng giới hạn của gate deterministic, cần LLM-judge/human cho factual correctness.
- Scoring là AI-assisted single-pass → nên human verify (đặc biệt C2/C4 của D11_Q2 và 2 ca Bloom over-tag).

## Files
- Output thô: `results/outputs/holdout/D1{1,2,3}_{outline,quiz}.json`
- Doc này.
