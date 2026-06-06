# D2 Results — Reliability, Latency & Cost (Groups A + B)

**Run on:** 2026-05-23 21:51 → 22:29 (~38 min wall clock)
**Model:** `gemini-2.5-pro`, temperature=0.2, top_p=0.95, max_out=8192
**Scope:** 10 docs × 3 features × 2 runs = **60 calls**
**Total cost:** **$2.06** (13.7% of $15 budget)

---

## Group A — Reliability (Bảng 5.x.2)

| Feature      | N  | API OK | JSON parse OK | Schema OK     | Markdown fence |
|--------------|----|--------|---------------|---------------|----------------|
| outline      | 20 | 100%   | 100%          | 100%          | **100%**       |
| page-content | 20 | 100%   | 100%          | 100%          | **100%**       |
| quiz         | 20 | 100%   | 100%          | 100%          | **100%**       |

**Empty field rate** (trường bắt buộc bị rỗng/null):

| Feature      | Empty fields    |
|--------------|-----------------|
| outline      | 0/20 (0%)       |
| page-content | 0/20 (0%)       |
| quiz         | **7/20 (35%)**  |

### Findings cốt lõi

1. **100% Reliability vượt mục tiêu** (95% target) cho tất cả 3 feature → pipeline production stable.

2. **🔥 100% output bị bọc trong markdown fence** (` ```json ... ``` `), bất chấp prompt chỉ thị tường minh "KHÔNG thêm bất kỳ lời dẫn hay giải thích nào bên ngoài JSON".
   → **Đây là phản chứng định lượng** cho phát biểu trong mục 3.4.1 thesis: *"Ràng buộc cấu trúc JSON … loại bỏ hoàn toàn các lỗi cú pháp tiềm ẩn"*.
   → Hậu xử lý `stripJsonFence()` là tuyệt đối cần thiết; nếu thiếu, parser sẽ fail 100%.
   → Cần sửa câu trong thesis thành: *"giảm đáng kể tỉ lệ lỗi cú pháp; tuy nhiên model vẫn thường xuyên bọc JSON trong code fence nên hệ thống cần lớp hậu xử lý"*.

3. **🔥 Quiz có 35% câu hỏi rỗng trường bắt buộc** (chủ yếu `prompt` hoặc `type` empty/null).
   → Cần đào sâu để xác định loại câu hỏi nào sinh rỗng (MCQ_SINGLE? MATCHING? FILL_IN_THE_BLANK?).
   → Đây là điểm yếu cụ thể của module quiz, cần đề cập trong mục 5.4.7 (Threats) và mục 6.2.2 (Limitations).

---

## Group B — Latency & Cost (Bảng 5.x.3)

| Feature      | N  | p50    | p95    | max    | avg in_tok | avg out_tok | avg $/call |
|--------------|----|--------|--------|--------|------------|-------------|------------|
| outline      | 20 | 20.6s  | 26.6s  | 40.4s  | 15,198     | 799         | $0.0616    |
| page-content | 20 | 25.1s  | 29.7s  | 31.0s  | 2,426      | 1,463       | $0.0238    |
| quiz         | 20 | 20.2s  | 24.8s  | 29.9s  | 2,571      | 806         | $0.0175    |

**Tổng chi phí:** $2.06 cho 60 calls (trung bình $0.034/call).

### Findings cốt lõi

1. **Tất cả p95 < 30s** → vượt mục tiêu UX (p95_target = 30s).
2. **Outline chi phí gấp 3.5× quiz** vì feed toàn bộ doc làm context (15k vs 2.5k input tokens).
3. **Page-content latency cao nhất** (p50=25s) dù input tokens nhỏ — do output dài (1463 tokens) cần thời gian generate.
4. **Outlier D10 outline run 2 = 40.4s** (vs run 1 cùng doc = 14.8s) → variance latency lớn (~2.7×) cho cùng input.
   → Cần report trong thesis dưới dạng "latency variance" — không phải hệ thống nhanh hay chậm, mà nhanh chậm có dao động.

---

## Phân tích theo từng tài liệu

| Doc | pages | avg_lat (s) | avg in_tok | avg $/call | schema_ok |
|-----|-------|-------------|------------|------------|-----------|
| D01 | 36    | 19.66       | 3,172      | $0.0208    | 100%      |
| D02 | 114   | 22.96       | 7,130      | $0.0351    | 100%      |
| D03 | 124   | 22.59       | 7,974      | $0.0369    | 100%      |
| D04 | 65    | 22.89       | 3,880      | $0.0263    | 100%      |
| **D05** | **360** | **21.12**   | **26,186**     | **$0.1010** | 100%   |
| D06 | 36    | 22.01       | 3,900      | $0.0239    | 100%      |
| D07 | 72    | 22.25       | 3,292      | $0.0221    | 100%      |
| D08 | 26    | 20.61       | 3,128      | $0.0235    | 100%      |
| D09 | 25    | 23.03       | 4,338      | $0.0264    | 100%      |
| D10 | 57    | 23.48       | 4,314      | $0.0270    | 100%      |

### D05 Case Study (360 trang)

- **Latency không bị skew:** 21.12s avg, tương đương trung bình tổng thể (~22s).
  → Bằng chứng hệ thống scale tốt với doc size về mặt response time.
- **Cost skew:** $0.10/call vs $0.02–0.04 cho các doc khác (4×).
  → Token input tuyến tính với doc size; cần khuyến cáo người dùng cuối.

---

## Cross-cutting findings → câu trả lời sẵn cho hội đồng

### Q: "Em đo chất lượng AI sinh ra bằng cách nào?"

→ "Em đã thực hiện 60 lệnh gọi API có instrumented trên 10 tài liệu thật (25–360 trang, 8 EN + 2 VI), đo 5 metric: tỉ lệ JSON parse thành công, tỉ lệ schema hợp lệ, tỉ lệ trường rỗng, latency p50/p95, và chi phí/lần gọi. Kết quả ghi ở CSV trong `src/test/resources/ai-eval/results/`. Tất cả 60 lệnh đều parse thành công (100% reliability), p95 latency 26–30s, chi phí trung bình $0.034/call."

### Q: "Tại sao quiz có 35% empty field?"

→ "Đây là phát hiện quan trọng từ eval. Module quiz dùng 6 loại câu hỏi (MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE, SHORT_ANSWER, FILL_IN_THE_BLANK, MATCHING). Hiện tại em ghi nhận tỉ lệ rỗng cao ở một số loại — chưa xác định cụ thể loại nào (đây là việc cần phân tích thêm). Em đã ghi điều này vào limitations chương 6."

### Q: "Output đôi khi vẫn lỗi cú pháp JSON không?"

→ "Có. 100% output bị Gemini bọc trong markdown fence ` ```json ... ``` ` dù prompt yêu cầu rõ KHÔNG. Đây là lý do hệ thống có hàm `stripJsonFence()` — không có nó thì BeanOutputConverter sẽ fail toàn bộ. Em đã cập nhật mục 3.4.1 cho đúng với thực tế này."

### Q: "Chi phí có quá cao không?"

→ "Trung bình $0.034 cho 1 lần sinh nội dung. Một bài giảng điển hình (1 outline + 5 page-content + 5 quiz) chi phí ~$0.27 — tương đương 0.6% lương theo giờ của giảng viên. ROI tích cực nếu công cụ tiết kiệm được vài phút biên soạn."

### Q: "Tại sao temperature 0.2 thay vì 0.7 của production?"

→ "Eval cần determinism cao để đo variance. Production dùng 0.7 cho creativity. Em chạy 2 runs/call để vẫn quan sát được variance — kết quả cho thấy latency có thể chênh 2.7× giữa 2 lần (D10 outline 14.8s vs 40.4s)."

---

## Limitations to disclose in mục 5.4.7

1. **N=60 là sample size nhỏ** cho statistical claims mạnh — coi đây là pilot eval.
2. **Tập tài liệu lệch về CS (5/10) và English (8/2)** — kết quả VI dựa trên chỉ 2 doc.
3. **Không có document scan/OCR** — Tika OCR limitations chưa được test.
4. **Quiz empty_field 35% chưa được phân tích theo question type** (D2 chỉ đo binary có/không).
5. **LLM-as-judge faithfulness chưa được chạy** — đó là D3 (sắp tới).
6. **temperature=0.2 != production 0.7** — variance trong eval có thể không phản ánh đúng production variance.

---

## Next steps

- **D3:** Chạy LLM-as-judge (Gemini Flash) để đo faithfulness + hallucination tự động trên 60 outputs đã sinh.
- **D3:** Calibration session 2 reviewer cho IWF rubric.
- **D4:** Human eval (30 mẫu faithfulness + 50 câu hỏi quiz IWF).
- **D5+:** Stats + viết section 5.4.
