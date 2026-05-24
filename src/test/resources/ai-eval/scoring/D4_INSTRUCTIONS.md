# D4 — Hướng dẫn chấm thủ công (cho 2 reviewer)

> Tài liệu này hướng dẫn em + thành viên (2 reviewer) thực hiện đánh giá thủ công
> cho mục 5.4.6 (IWF) và 5.4.5 (Faithfulness human verify) của thesis.
>
> **Quy tắc tối thượng:** 2 reviewer **CHẤM ĐỘC LẬP** — không chia sẻ kết quả
> cho đến khi cả 2 hoàn thành. Nếu thảo luận trước, mất tính khách quan của
> Cohen's κ.

---

## Workflow tổng quan

```
1. Calibration (30 phút)
   ↓
2. Independent scoring (mỗi người ~5h)
   ↓
3. Run compute_agreement.py → xem κ và list disagreements
   ↓
4. Consensus meeting (1-2h) → resolve disagreements
   ↓
5. Re-run compute_agreement.py → final stats cho thesis
```

---

## Phần 1: Calibration session (LÀM TRƯỚC)

**Mục đích:** Đảm bảo 2 reviewer hiểu rubric giống nhau trước khi chấm 50 câu thật.

### Bước 1.1 — Đọc rubric
- Mỗi reviewer đọc kỹ:
  - `../rubrics/iwf-checklist.md` — 12 IWF flaws + ví dụ
  - `../rubrics/pedagogical-rubric.md` — 4 tiêu chí + Bloom (optional)

### Bước 1.2 — Chấm thử 5 câu đầu tiên cùng nhau (D01_Q1 → D01_Q5)
- Mở **cùng lúc** `iwf_reviewerA.csv` và `iwf_reviewerB.csv`
- Reviewer A chấm 5 câu đầu của file A, Reviewer B chấm 5 câu đầu của file B
- **CHƯA chia sẻ kết quả**
- Sau khi cả 2 chấm xong 5 câu → so sánh:
  - Bất kỳ flaw nào 2 người chấm khác nhau → thảo luận
  - Nếu khác do hiểu khác định nghĩa → cập nhật `../rubrics/iwf-checklist.md` thêm note
  - Nếu khác do interpret content khác → ghi nhận, không cần update rubric
- Sau calibration, có thể **sửa lại 5 chấm đầu** dựa trên hiểu mới

### Bước 1.3 — Tiêu chí pass calibration
- ≥ 4/5 flaw có agreement (cùng đánh giá ≥ 4/5 câu)
- Nếu không pass → calibrate thêm 5 câu nữa
- Nếu vẫn không pass → giảm checklist từ 12 → 8 flaws dễ nhận diện nhất

---

## Phần 2: Chấm IWF + Pedagogical (mỗi người ~3h)

### File cần chấm
- Reviewer A: `iwf_reviewerA.csv` (50 dòng, dòng 1-5 đã làm ở calibration)
- Reviewer B: `iwf_reviewerB.csv` (cùng 50 câu, scores độc lập)

### Cách chấm từng câu

Đọc cột `prompt_text`, `options_text`, `correct_answer_text`, `explanation_text`.

**Với 12 IWF flag (cột TW-1 → ID-6):**
- Đánh **1** nếu câu hỏi MẮC flaw đó
- Đánh **0** nếu KHÔNG mắc
- Nếu phân vân (~50/50): đánh **1** rồi note "uncertain" vào cột `notes` — sẽ thảo luận ở consensus

**Với 4 pedagogical flag (cột C1-C4):**
- Đánh **1** nếu câu hỏi ĐẠT tiêu chí
- Đánh **0** nếu KHÔNG đạt
- (Đảo ngược logic so với IWF!)

**Với cột `bloom_level` (optional):**
- 1 = Remember, 2 = Understand, 3 = Apply, 4 = Analyze, 5 = Evaluate, 6 = Create
- Có thể để trống nếu không chắc — không bắt buộc

**Cột `notes`** (optional): ghi lý do nếu chấm khó, sẽ giúp consensus session

### Tips chấm nhanh
- Đặt timer 3 phút/câu → 50 câu × 3 phút = 2.5h
- Đừng over-think — trực giác chuyên môn thường đúng
- Có thể dùng Google Sheets để import CSV, dễ thao tác hơn (Insert → Import → upload CSV)
- Khi import vào Google Sheets, các cột text dài sẽ tự wrap — thuận tiện đọc

---

## Phần 3: Chấm faithfulness human verify (mỗi người ~2h)

### File cần chấm
- Reviewer A: `faithfulness_human_reviewerA.csv` (30 mẫu)
- Reviewer B: `faithfulness_human_reviewerB.csv` (cùng 30 mẫu)

### Cách chấm

Mỗi dòng có:
- `ground_truth_facts`: 5-10 fact chính từ tài liệu nguồn
- `generated_content`: nội dung AI sinh ra (outline / page-content / quiz)

**Cột `human_faithfulness_score` (1-5):**

| Score | Định nghĩa |
|-------|-----------|
| **5** | Mọi claim cốt lõi trong output đều ĐƯỢC HỖ TRỢ bởi GT facts hoặc HIỂN NHIÊN đúng với tài liệu nguồn. |
| **4** | Hầu hết faithful; tối đa 1 claim không được hỗ trợ nhưng plausible. |
| **3** | Một vài unsupported claims, nhưng KHÔNG có claim mâu thuẫn với GT. |
| **2** | Nhiều unsupported claims HOẶC 1 claim rõ ràng mâu thuẫn. |
| **1** | Hallucination nặng — claim mâu thuẫn trực tiếp với GT. |

**Cột `human_hallucination`:**
- `yes` nếu output có ≥ 1 claim **mâu thuẫn** với GT facts
- `no` nếu chỉ "không có trong GT" mà không mâu thuẫn

> Quan trọng: phân biệt "không có trong GT" (≠ hallucination) vs "mâu thuẫn với GT" (= hallucination).
> GT chỉ 5-10 fact/doc → outline cover nhiều thứ hơn là expected, KHÔNG phải hallucination.

**Cột `notes`** (optional): ví dụ cụ thể claim nào sai, sẽ dùng làm case study trong thesis

---

## Phần 4: Consensus meeting

Khi cả 2 reviewer hoàn thành chấm độc lập:

### Bước 4.1
```bash
python3 ../compute_agreement.py
```

Output sẽ in:
- Cohen's κ cho mỗi flag/criterion
- Số disagreements
- Bảng tổng hợp tạm (loại bỏ disagreements)
- File `consensus_targets.csv` với mỗi dòng = 1 disagreement

### Bước 4.2 — Resolve từng disagreement
- Mở `consensus_targets.csv`
- Mỗi dòng cho biết: qid, flag bị bất đồng, score của A, score của B
- 2 reviewer thảo luận, điền cột `consensus` với 0 hoặc 1
- Nếu thực sự không thống nhất được → để trống → sẽ loại khỏi tính toán

### Bước 4.3 — Final stats
```bash
python3 ../compute_agreement.py
```

Lần này sẽ in:
- Cohen's κ (final, dùng cho Bảng 5.x.8)
- Bảng 5.x.6 (flaw_free_rate aggregate)
- Bảng 5.x.7 (frequency của 12 flaws)
- Faithfulness human vs LLM judge agreement

→ **Copy 4 bảng này vào mục 5.4.6 + 5.4.5 của thesis.**

---

## Mục tiêu / Gate

| Metric | Target |
|--------|--------|
| Cohen's κ mean across 16 flags | ≥ 0.60 (substantial) |
| Số flag đạt κ ≥ 0.60 | ≥ 12/16 |
| Số mẫu faithfulness được chấm đủ | 30/30 |
| Hallucination κ A vs B | ≥ 0.60 |
| Faithfulness human vs LLM-judge Pearson r | ≥ 0.60 (cho phép report LLM-judge số liệu) |

Nếu κ thấp ở một flag cụ thể (< 0.40) → ghi vào limitations 5.4.7:
*"Flag X có κ thấp do định nghĩa subjective; loại khỏi báo cáo aggregate hoặc gộp với flag tương tự."*

---

## Time budget realistic

| Hoạt động | Người A | Người B |
|-----------|---------|---------|
| Đọc rubric | 30 phút | 30 phút |
| Calibration 5 câu | 30 phút (cùng nhau) | |
| IWF + Ped chấm 50 câu | 2.5h | 2.5h |
| Faithfulness chấm 30 mẫu | 1.5h | 1.5h |
| Consensus meeting | 1.5h (cùng nhau) | |
| **Tổng** | **~6h** | **~6h** |

→ Có thể chia 2 ngày (ví dụ D4 sáng = IWF, chiều = Faithfulness).

---

## FAQ

**Q: Tôi không chắc 1 câu mắc flaw nào → chọn 0 hay 1?**
A: Default là 0 (không mắc). Chỉ đánh 1 nếu CHẮC CHẮN mắc. Nếu phân vân → đánh 0 + note vào `notes`.

**Q: 1 câu mắc nhiều flaw cùng lúc được không?**
A: Có. Một câu có thể mắc nhiều flaw. Chấm độc lập từng cột.

**Q: Cố ý chấm khác bạn để dễ thảo luận consensus không?**
A: KHÔNG. Mục đích của κ là đo agreement THẬT giữa 2 reviewer độc lập. Nếu chấm cố ý sẽ làm sai stats.

**Q: Có cần chấm bloom_level không?**
A: Optional. Bloom chỉ dùng để vẽ biểu đồ phân bố trong thesis (bonus). Nếu thời gian ít → skip.

**Q: 1 câu trong CSV có content quá dài, không thấy hết — sao đọc?**
A: Import CSV vào Google Sheets, click vào cell → text box hiện full nội dung.
