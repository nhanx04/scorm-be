# AI Evaluation Test Harness

Tài nguyên cho phần đánh giá định lượng module AI (mục **5.4 Đánh giá module AI**
trong báo cáo đồ án tốt nghiệp).

Mục tiêu: đo Reliability / Latency / Cost / Faithfulness / Pedagogical Quality
của ba tính năng AI (`generate-outline`, `generate-page-content`, `generate-quiz`)
chạy trên Gemini 2.5 Pro qua Spring AI.

## Cấu trúc thư mục

```
ai-eval/
├── README.md                     # File này
├── eval-config.yaml              # Cấu hình thí nghiệm (docs, model, runs, temperature)
├── documents/                    # 10 tài liệu giáo trình test (PDF/DOCX)
│   ├── cs-oop.pdf               # CNTT – Lập trình hướng đối tượng
│   ├── cs-database.pdf          # CNTT – Cơ sở dữ liệu
│   ├── cs-network.pdf           # CNTT – Mạng máy tính
│   ├── math-calculus.pdf        # Cơ bản – Giải tích
│   ├── physics-general.pdf      # Cơ bản – Vật lý đại cương
│   ├── philosophy-marxism.pdf   # Cơ bản – Triết học Mác-Lênin
│   ├── econ-microeconomics.pdf  # KHXH – Kinh tế vi mô
│   ├── history-vietnam.pdf      # KHXH – Lịch sử Việt Nam
│   ├── en-ml-intro.pdf          # English – Machine Learning intro
│   └── en-software-eng.pdf      # English – Software Engineering
├── ground-truth/
│   └── ground_truth.json        # 5 "key facts" cho mỗi tài liệu (phục vụ đo hallucination)
├── rubrics/
│   ├── iwf-checklist.md         # 12 Item Writing Flaws (Haladyna 2002)
│   └── pedagogical-rubric.md    # 4 tiêu chí chất lượng câu hỏi
├── results/                     # Output CSV của các test run (gitignored)
│   ├── reliability_<timestamp>.csv
│   ├── latency_cost_<timestamp>.csv
│   ├── faithfulness_auto_<timestamp>.csv
│   ├── human_scoring_<timestamp>.csv
│   └── iwf_scoring_<timestamp>.csv
└── DOCUMENT_COLLECTION.md       # Checklist thu thập 10 tài liệu
```

## Quy ước đặt tên tài liệu

`<domain>-<topic>.<ext>` với `domain ∈ {cs, math, physics, philosophy, econ, history, en}`.

## Quy ước phiên bản model

Toàn bộ thí nghiệm phải pin model về một version cố định để đảm bảo reproducibility.
Xem `eval-config.yaml` → `model.version`.

## Cách chạy

Sau khi đã có đầy đủ:

1. 10 tài liệu trong `documents/`
2. `ground_truth.json` đã điền key facts
3. `GOOGLE_GENAI_API_KEY` được set trong môi trường

Chạy:

```bash
mvn test -Dtest=AiQualityEvaluationTest -Dai-eval.enabled=true
```

Kết quả sẽ ghi ra `results/<metric>_<timestamp>.csv`.

> Lưu ý: test này tốn API credit thật (~5 USD cho 1 lượt full eval).
> Mặc định bị tắt qua property `ai-eval.enabled=false`.

## Kế hoạch evaluation

Xem file đính kèm trong báo cáo đồ án — mục 5.4. Các metric chính:

| Nhóm | Metric | Tự động? |
|------|--------|----------|
| A. Reliability | json_parse_success_rate, schema_validation_pass | ✅ |
| B. Latency & Cost | latency_p50/p95, tokens_in/out, cost_usd | ✅ |
| C. Faithfulness | faithfulness_score, hallucination_rate | LLM-judge + human verify |
| D. Pedagogical | clarity, single_correct, plausible_distractors, source_grounded | Human |
| E. IWF | 12 flaws (TW-1..6, ID-1..6) | Human (2 reviewer + κ) |

## Tài liệu tham khảo

- Haladyna, T. M., Downing, S. M., & Rodriguez, M. C. (2002). A review of
  multiple-choice item-writing guidelines for classroom assessment.
  *Applied Measurement in Education*, 15(3), 309–333.
- Tarrant, M., & Ware, J. (2008). Impact of item-writing flaws in multiple-choice
  questions on student achievement in high-stakes nursing assessments.
  *Medical Education*, 42(2), 198–206.
