# Document Collection — Status

**Completed on:** 2026-05-23 (D1)
**Status:** ✅ 10/10 documents collected, ground truth filled

---

## Final document set

10 tài liệu giáo trình thực tế đã thu thập (chủ yếu từ HCMUT). Tất cả đều
là slide bài giảng dạng PDF, đã được xác minh có thể đọc được bằng Apache Tika.

| ID  | File                                                   | Subject                                      | Lang | Pages |
|-----|--------------------------------------------------------|----------------------------------------------|------|-------|
| D01 | `1-Introduction-2023.pdf`                              | Introduction to Programming Languages        | EN   | 36    |
| D02 | `4-1_SQL.pdf`                                          | SQL (DDL/DML/DCL)                            | EN   | 114   |
| D03 | `201_DSA___Chapter_7__Hash___Search__new_.pdf`         | Searching & Hash Tables                      | EN   | 124   |
| D04 | `Chapter_5 - Transaction Processing.pdf`               | DB Transaction Processing                    | EN   | 65    |
| D05 | `ds10connectivity.pdf`                                 | Graph Connectivity (Discrete Math, Ch.9)     | EN   | 360   |
| D06 | `BayesianLearning.pdf`                                 | Bayesian Learning (ML)                       | EN   | 36    |
| D07 | `05_Design_thinking_v3.pdf`                            | Design Thinking (E-commerce, Ch.5)           | EN   | 72    |
| D08 | `NVT- POM-Ch1- DAI CUONG VE TIEP THI.pdf`              | Đại cương về Marketing                       | VI   | 26    |
| D09 | `Chương 6.pdf`                                         | Vật lý: Trường tĩnh điện                     | VI   | 25    |
| D10 | `Chuong2_CungCauDiemCanBangGiaTranGiaSanThue_Eng.pdf`  | Microeconomics: Supply & Demand              | EN   | 57    |

**Total:** 915 pages

## Phân bố

### Theo lĩnh vực

| Lĩnh vực | Số doc | IDs |
|----------|--------|-----|
| Computer Science | 5 | D01, D02, D03, D04, D06 |
| Mathematics (Graph theory) | 1 | D05 |
| Business / Marketing | 2 | D07, D08 |
| Physics | 1 | D09 |
| Economics | 1 | D10 |

### Theo ngôn ngữ

| Ngôn ngữ | Số doc | IDs |
|----------|--------|-----|
| English | 8 | D01, D02, D03, D04, D05, D06, D07, D10 |
| Vietnamese | 2 | D08, D09 |

### Theo độ dài (token cost bucketing)

| Bucket | Số doc | IDs |
|--------|--------|-----|
| Nhỏ (≤ 40 trang) | 4 | D01, D06, D08, D09 |
| Trung bình (40-100 trang) | 3 | D04, D07, D10 |
| Lớn (100-200 trang) | 2 | D02, D03 |
| Rất lớn (> 200 trang) | 1 | D05 (360 trang) |

## Threats to validity (ghi vào mục 5.4.7)

Phân bố không hoàn hảo cần được nêu rõ trong báo cáo:

- **Skew về CS (5/10):** Phản ánh nguồn tài liệu nhóm có sẵn (giáo trình HCMUT). Kết quả có thể không generalize tốt cho các lĩnh vực khác như y khoa, ngôn ngữ, nghệ thuật.
- **Skew về tiếng Anh (8/2):** Phần lớn slide ĐHBK là tiếng Anh. Kết quả cho tiếng Việt chỉ dựa trên 2 doc → cần ghi rõ là "sample size thấp".
- **D05 rất lớn (360 trang):** Có thể gây outlier trong latency/cost. Khi báo cáo nên tách kết quả theo bucket độ dài.
- **Không có file scan ảnh:** Toàn bộ là PDF text-based → chưa test được khả năng xử lý khi Tika không OCR được.

## Ground truth

✅ Đã bóc xong **55 key facts** trong `ground-truth/ground_truth.json`:
- 9 doc × 5 fact = 45 fact
- D05 × 10 fact = 10 fact (mở rộng vì doc dài 360 trang để giữ tỉ lệ phủ ngữ cảnh tương đương các doc khác — ghi rõ trong limitations)

Facts được viết bằng ngôn ngữ của tài liệu nguồn (8 EN + 2 VI).

## Gate D1 — Status

- [x] Đủ 10 file trong `documents/`
- [x] `ground_truth.json` có 50 fact đã điền (không còn TODO)
- [x] `eval-config.yaml.actual_pages` cập nhật đúng (lấy từ `pdfinfo`)
- [x] Phân bố lĩnh vực/ngôn ngữ được ghi nhận để báo cáo limitations
- [x] Commit + push

## Next: D2

Sang **D2 (Chủ nhật 24/05)**: viết `AiQualityEvaluationTest.java` để đo tự động:
- Nhóm A — Reliability (json_parse_success, schema_validation, etc.)
- Nhóm B — Latency & Cost (p50/p95, tokens, USD)

→ Chạy 60 API calls (10 docs × 3 features × 2 runs), xuất CSV.
→ Budget ước tính: ~$8-10 (sau khi tính lại với 915 trang).
