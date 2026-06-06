# Pedagogical Quality Rubric — 4 tiêu chí

> Đánh giá chất lượng tổng thể câu hỏi quiz (bổ sung cho IWF).
>
> **Cách dùng:** Với mỗi câu hỏi, chấm 0 (không đạt) hoặc 1 (đạt) cho từng tiêu chí.
> 2 reviewer chấm độc lập, sau đó họp consensus.

---

## C1. Clarity (Độ rõ nghĩa)

**Đạt (1):** Câu hỏi rõ ràng, không mơ hồ, người làm bài hiểu ngay mình cần làm gì.

**Không đạt (0):**
- Câu hỏi có ≥ 2 cách hiểu hợp lý
- Có lỗi chính tả/ngữ pháp nghiêm trọng làm khó hiểu
- Sử dụng thuật ngữ chuyên ngành mà không định nghĩa trong khi đó là khái niệm chính của câu hỏi

---

## C2. Single Correct Answer (Có duy nhất 1 đáp án đúng)

**Đạt (1):** Có đúng 1 đáp án rõ ràng đúng, các đáp án khác đều sai.

**Không đạt (0):**
- Có ≥ 2 đáp án đúng (cùng đúng theo lý thuyết)
- Có ≥ 2 đáp án có thể đúng tùy ngữ cảnh
- Đáp án "đúng" thực ra sai hoặc gây tranh cãi học thuật
- Tất cả đáp án đều sai

---

## C3. Plausible Distractors (Distractor hợp lý)

**Đạt (1):** ≥ 2 trong 3 distractor (giả định 4 lựa chọn) đáng tin, không lộ liễu.

**Không đạt (0):**
- Có > 1 distractor sai quá lộ liễu (không liên quan hoặc trùng nội dung)
- Distractor có dạng troll/hài hước
- Distractor không nằm trong cùng chủ đề với đáp án đúng

> **Lưu ý phân biệt với IWF ID-5:** ID-5 đo "có ≥ 1 distractor lộ liễu";
> C3 đo "có ≥ 2 distractor hợp lý". Một câu có thể đạt C3 mà vẫn vướng ID-5.

---

## C4. Source Grounded (Bám sát tài liệu nguồn)

**Đạt (1):** Đáp án đúng có thể tra ngược về tài liệu nguồn — sự thật được nêu rõ
hoặc suy luận trực tiếp từ tài liệu.

**Không đạt (0):**
- Đáp án đúng cần kiến thức bên ngoài tài liệu nguồn
- Câu hỏi về thông tin không có trong tài liệu (hallucination)
- Đáp án đúng mâu thuẫn với tài liệu nguồn

---

## Metric tổng hợp

- `clarity_pass_rate`: % câu đạt C1
- `single_correct_pass_rate`: % câu đạt C2
- `plausible_distractors_pass_rate`: % câu đạt C3
- `source_grounded_pass_rate`: % câu đạt C4
- `all_criteria_pass_rate`: % câu đạt **cả 4** tiêu chí — mục tiêu ≥ 70%

## Bonus (nếu kịp): Gán mức Bloom

Sau khi chấm 4 tiêu chí, có thể gán thêm mức Bloom Taxonomy cho mỗi câu để vẽ
biểu đồ phân bố:

| Mức | Tên | Đặc điểm câu hỏi |
|-----|-----|--------------------|
| 1 | Remember | Nhớ định nghĩa, năm, tên |
| 2 | Understand | Giải thích, so sánh, phân loại |
| 3 | Apply | Dùng kiến thức vào tình huống cụ thể |
| 4 | Analyze | Phân tích thành phần, mối quan hệ |
| 5 | Evaluate | Đánh giá, phê phán |
| 6 | Create | Tạo ra cái mới |

→ Liên hệ ngược về Chương 1 báo cáo (đã trích Bloom Taxonomy).
