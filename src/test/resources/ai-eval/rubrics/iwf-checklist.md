# Item Writing Flaws (IWF) Checklist — 12 flaws

> Rubric chấm lỗi viết câu hỏi trắc nghiệm.
> Adapted từ Haladyna, Downing & Rodriguez (2002) và Tarrant & Ware (2008).
>
> **Cách dùng:** Với mỗi câu hỏi trắc nghiệm, đánh dấu 0 (không mắc) hoặc 1 (có mắc)
> cho từng flaw. Chấm độc lập giữa 2 reviewer trước, sau đó họp consensus.

---

## Nhóm A — Testwiseness Flaws (gợi ý giúp đoán đáp án mà không cần kiến thức)

### TW-1. Length cue (Manh mối độ dài)
**Định nghĩa:** Đáp án đúng dài hơn rõ rệt so với các distractor.
**Operational definition:** Đáp án đúng có số từ ≥ 1.5× trung bình số từ của các distractor.
**Ví dụ mắc lỗi:**
- A. Đỏ
- B. Xanh
- C. Là màu được tạo ra khi ánh sáng có bước sóng dài nhất trong dải nhìn thấy được, thường ứng với cảm xúc nóng ✅ (đúng nhưng dài)
- D. Vàng

---

### TW-2. Grammatical cue (Manh mối ngữ pháp)
**Định nghĩa:** Cấu trúc ngữ pháp của stem chỉ khớp với 1 đáp án.
**Ví dụ mắc lỗi:**
- Stem: "Phần tử nào KHÔNG phải là một..."
- Chỉ có 1 đáp án ở dạng số ít, các đáp án khác số nhiều → người làm bài đoán ra.

---

### TW-3. Word repeat / Clang clue (Lặp từ khóa)
**Định nghĩa:** Từ khóa quan trọng trong stem lặp lại nguyên văn trong đáp án đúng.
**Ví dụ mắc lỗi:**
- Stem: "Tầng nào trong mô hình OSI chịu trách nhiệm **định tuyến**?"
- A. Tầng Transport
- B. Tầng Network (tầng **định tuyến**) ✅
- C. Tầng Session
- D. Tầng Application

---

### TW-4. Logical clue / Convergence (Hội tụ logic)
**Định nghĩa:** Đáp án đúng chứa các yếu tố xuất hiện ở nhiều distractor khác (như một "tổng" của các option).
**Ví dụ mắc lỗi:**
- A. Tốc độ
- B. Tính an toàn
- C. Khả năng mở rộng
- D. Cả tốc độ, an toàn và khả năng mở rộng ✅

---

### TW-5. Absolute terms in distractor (Từ tuyệt đối trong distractor)
**Định nghĩa:** Distractor chứa từ tuyệt đối như "luôn luôn", "không bao giờ", "tất cả", "duy nhất" → thường sai → dễ bị loại.
**Ví dụ mắc lỗi:**
- A. Đa hình **luôn luôn** yêu cầu kế thừa
- B. Đa hình **không bao giờ** sử dụng interface
- C. Đa hình **chỉ** xảy ra ở compile-time
- D. Đa hình là khả năng các đối tượng đáp ứng cùng một thông điệp theo cách khác nhau ✅

---

### TW-6. Vague terms in correct answer (Từ mơ hồ trong đáp án đúng)
**Định nghĩa:** Đáp án đúng dùng từ mơ hồ như "thường", "có thể", "đôi khi", "phần lớn" → an toàn nên dễ chọn.
**Ví dụ mắc lỗi:**
- A. Hash table luôn truy cập trong O(1)
- B. Hash table truy cập **thường** trong O(1) trung bình ✅
- C. Hash table truy cập trong O(n) mọi trường hợp
- D. Hash table không có giới hạn về tốc độ truy cập

---

## Nhóm B — Irrelevant Difficulty Flaws (làm câu hỏi khó vì lý do không liên quan kiến thức)

### ID-1. Stem không đặt câu hỏi rõ ràng
**Định nghĩa:** Stem là câu lửng, không phải câu hỏi hoàn chỉnh, người làm bài phải đọc cả 4 đáp án mới biết câu hỏi đang hỏi gì.
**Ví dụ mắc lỗi:**
- ❌ Stem: "Đa hình trong OOP..."
- ✅ Stem: "Đặc điểm nào sau đây mô tả đúng nhất về đa hình trong OOP?"

---

### ID-2. "All of the above" / "None of the above"
**Định nghĩa:** Sử dụng các đáp án dạng "Tất cả đều đúng" / "Không đáp án nào đúng".
**Lý do tránh:** Bị Haladyna khuyến cáo loại bỏ — biến câu hỏi từ "single best answer" thành câu trả lời partial-credit, và làm dễ đoán.

---

### ID-3. Negative stem không in đậm/viết hoa
**Định nghĩa:** Stem dạng phủ định ("Cái nào KHÔNG phải là...") nhưng không nhấn mạnh từ phủ định.
**Lý do tránh:** Người làm bài dễ đọc lướt qua, hiểu sai sang câu khẳng định.

---

### ID-4. Heterogeneous options (Các đáp án không đồng nhất)
**Định nghĩa:** Các đáp án không cùng loại (1 đáp án là khái niệm, 3 đáp án là ví dụ).
**Ví dụ mắc lỗi:**
- A. Kế thừa
- B. Đóng gói
- C. Class Animal kế thừa từ class LivingThing
- D. Đa hình

---

### ID-5. Implausible distractors (Distractor không hợp lý)
**Định nghĩa:** Có ít nhất 1 distractor sai quá lộ liễu, không tạo được nhiễu, người làm bài có thể loại ngay.
**Ví dụ mắc lỗi (câu hỏi về OOP):**
- A. Kế thừa
- B. Đa hình
- C. Pizza  ← rõ ràng không liên quan
- D. Đóng gói

---

### ID-6. Window dressing (Trang trí cửa sổ)
**Định nghĩa:** Stem chứa nhiều thông tin không cần thiết để trả lời câu hỏi, làm người làm bài lãng phí thời gian đọc.
**Ví dụ mắc lỗi:**
- "Trong một công ty phần mềm có 200 nhân viên ở Hà Nội, mỗi nhân viên dùng laptop Dell hoặc HP, kết nối qua mạng WiFi 5GHz, **giao thức TCP hoạt động ở tầng mấy trong mô hình OSI?**"

---

## Quy ước chấm

- **0 = Không mắc flaw này**
- **1 = Có mắc flaw này**
- Nếu phân vân (~50/50), chấm **1** rồi đánh dấu để thảo luận ở consensus session.
- Một câu có thể mắc nhiều flaw cùng lúc — đánh dấu tất cả.

## Metric tổng hợp (tính sau khi consensus)

- `flaw_count_per_item`: tổng flaw mỗi câu, ∈ [0, 12]
- `flaw_free_rate`: % câu không mắc flaw nào
- `severely_flawed_rate`: % câu mắc ≥ 2 flaw
- `flaw_frequency[i]`: tần suất từng flaw trong toàn bộ tập câu hỏi
- `cohens_kappa_per_flaw`: độ đồng thuận 2 reviewer cho mỗi flaw (mục tiêu ≥ 0.6)

## Tài liệu tham khảo

- Haladyna, T. M., Downing, S. M., & Rodriguez, M. C. (2002). A review of
  multiple-choice item-writing guidelines for classroom assessment.
  *Applied Measurement in Education*, 15(3), 309–333.
- Tarrant, M., & Ware, J. (2008). Impact of item-writing flaws in multiple-choice
  questions on student achievement in high-stakes nursing assessments.
  *Medical Education*, 42(2), 198–206.
