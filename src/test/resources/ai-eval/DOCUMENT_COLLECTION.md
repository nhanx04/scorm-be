# Checklist thu thập tài liệu test

> Hôm nay (D1, Thứ 7 23/05/2026): cần thu đủ 10 tài liệu trước cuối ngày.

## Tiêu chí chọn tài liệu

- **Định dạng:** PDF hoặc DOCX (Tika có thể đọc, không phải file scan ảnh)
- **Độ dài:** 5–20 trang (giữ chi phí token thấp, vẫn đủ ngữ cảnh)
- **Nội dung:** Giáo trình/slide bài giảng có cấu trúc, có khái niệm rõ ràng
- **Tránh:**
  - File scan ảnh (Tika không OCR được mặc định)
  - Tài liệu < 3 trang (quá ít context)
  - Tài liệu > 30 trang (vượt context window dễ tốn token)
  - File có DRM/password
  - Tài liệu nhạy cảm (đề thi, tài liệu nội bộ)

## Phân công

| Người | Tài liệu phụ trách |
|------|--------------------|
| Người A (em) | D01, D02, D03, D04, D05 |
| Người B (thành viên) | D06, D07, D08, D09, D10 |

## Checklist 10 tài liệu

### Nhóm CNTT (3 tài liệu)

- [ ] **D01 — `cs-oop.pdf`** — Lập trình hướng đối tượng (tiếng Việt)
  - Gợi ý nguồn: Slide chương "Kế thừa & Đa hình" môn CO2003, hoặc tải từ kho học liệu HCMUT
  - Mục tiêu: chứa định nghĩa OOP, 4 trụ cột (encapsulation, inheritance, polymorphism, abstraction)

- [ ] **D02 — `cs-database.pdf`** — Cơ sở dữ liệu (tiếng Việt)
  - Gợi ý nguồn: Chương "Mô hình ER" hoặc "Chuẩn hóa" môn CO2013
  - Mục tiêu: chứa khái niệm thực thể, thuộc tính, quan hệ, khóa chính

- [ ] **D03 — `cs-network.pdf`** — Mạng máy tính (tiếng Việt)
  - Gợi ý nguồn: Chương "Mô hình OSI/TCP-IP" môn CO3093
  - Mục tiêu: chứa 7 tầng OSI, vai trò từng tầng

### Nhóm cơ bản (3 tài liệu)

- [ ] **D04 — `math-calculus.pdf`** — Giải tích (tiếng Việt)
  - Gợi ý nguồn: Chương "Đạo hàm và vi phân" giáo trình Toán cao cấp
  - Mục tiêu: định nghĩa đạo hàm, quy tắc đạo hàm cơ bản

- [ ] **D05 — `physics-general.pdf`** — Vật lý đại cương (tiếng Việt)
  - Gợi ý nguồn: Chương "Động lực học chất điểm" - 3 định luật Newton
  - Mục tiêu: 3 định luật Newton + công thức

- [ ] **D06 — `philosophy-marxism.pdf`** — Triết học Mác-Lênin (tiếng Việt)
  - Gợi ý nguồn: Chương "Phép biện chứng duy vật" - giáo trình môn Mác Lênin
  - Mục tiêu: 2 nguyên lý, 3 quy luật, 6 cặp phạm trù

### Nhóm KHXH (2 tài liệu)

- [ ] **D07 — `econ-microeconomics.pdf`** — Kinh tế vi mô (tiếng Việt)
  - Gợi ý nguồn: Chương "Cung và cầu" giáo trình Mankiw bản dịch tiếng Việt
  - Mục tiêu: luật cung, luật cầu, giá cân bằng

- [ ] **D08 — `history-vietnam.pdf`** — Lịch sử Việt Nam (tiếng Việt)
  - Gợi ý nguồn: Một chương cụ thể về giai đoạn 1858-1945 hoặc 1945-1975
  - Mục tiêu: mốc thời gian, nhân vật chính, sự kiện chính

### Nhóm tiếng Anh (2 tài liệu — test multilingual)

- [ ] **D09 — `en-ml-intro.pdf`** — Machine Learning (English)
  - Gợi ý nguồn: Stanford CS229 Note 1 (Supervised Learning intro) hoặc 1 chương từ ESL/ISL
  - Link: https://cs229.stanford.edu/main_notes.pdf (lấy 10 trang đầu)
  - Mục tiêu: định nghĩa supervised/unsupervised learning, hypothesis function

- [ ] **D10 — `en-software-eng.pdf`** — Software Engineering (English)
  - Gợi ý nguồn: MIT 6.005 reading hoặc 1 chương từ "Software Engineering" của Sommerville
  - Link MIT: https://web.mit.edu/6.005/www/fa15/general/readings.html
  - Mục tiêu: khái niệm SDLC, requirements engineering, hoặc testing

## Quy trình lưu

1. Rename file đúng quy ước trong `eval-config.yaml`
2. Copy vào `src/test/resources/ai-eval/documents/`
3. Tick checkbox tương ứng ở file này
4. **Lưu ý quan trọng:** file PDF/DOCX KHÔNG được commit (đã có rule trong `.gitignore`).
   Chỉ commit:
   - File này (đã tick các checkbox)
   - File `ground_truth.json` đã điền

## Sau khi có đủ tài liệu

→ Mở `ground-truth/ground_truth.json`, đọc tài liệu và bóc 5 key facts/doc.
   Xem `examples_good` và `examples_bad` trong file để biết fact tốt là gì.

→ Lưu lại số trang thực tế của mỗi tài liệu (cập nhật field `expected_pages` trong
   `eval-config.yaml`) — để báo cáo trong Bảng 5.x.1 "Tổng quan tập dữ liệu".

## Gate cuối ngày D1

- [ ] Đủ 10 file trong `documents/`
- [ ] `ground_truth.json` có 50 fact đã điền (không còn TODO)
- [ ] `eval-config.yaml` `expected_pages` khớp với số trang thực tế
- [ ] 2 người review chéo ground truth của nhau
- [ ] Commit + push (chỉ commit checklist này + ground_truth, không commit PDF)
