# D4 — AI-Assisted Scoring Methodology (Disclosure)

> **Đây là tài liệu disclosure cần thiết cho thesis.** Phải tóm tắt vào mục
> 5.4.7 (Threats to Validity) — không được phép trình bày D4 results như
> "human inter-rater agreement" trong thesis.

---

## Bối cảnh

D4 ban đầu được thiết kế là **human evaluation day**: 2 reviewer thật (em +
thành viên nhóm) chấm độc lập 50 quiz × 16 flag + 30 mẫu faithfulness, sau
đó tính Cohen's κ.

Do hạn chế thời gian trước bảo vệ, công đoạn này được chuyển sang
**AI-assisted scoring** với 2 perspective khác nhau, áp dụng cho cả IWF và
faithfulness verification.

## Methodology áp dụng

### 1. Hai perspective được pre-coded

Cùng 1 entity (Claude Opus, model `claude-opus-4-7`) đóng vai trò cả 2 reviewer
với **2 hệ thống đánh giá khác nhau**:

| Perspective | Tinh thần | Áp dụng cho |
|---|---|---|
| **Reviewer A — Strict** | Conservative academic rubric application; flag bất kỳ nghi vấn nào | IWF flag: tighter threshold; Pedagogical: stricter pass |
| **Reviewer B — Lenient** | Pragmatic educational view; benefit-of-doubt | IWF flag: wider threshold; Pedagogical: more forgiving |

Hai loại variance:

#### A. Programmatic threshold variance (cho 6 objective flags)
| Flag | Strict | Lenient |
|---|---|---|
| TW-1 length cue | ratio ≥ 1.5 → flag | ratio ≥ 2.0 → flag |
| TW-3 word repeat | 1+ shared word | 2+ shared words |
| TW-5 absolute terms | 1+ absolute term | 2+ absolute terms |
| TW-6 vague terms | 1+ vague term | 2+ vague terms |
| ID-4 heterogeneous options | wc ratio ≥ 2.0 | wc ratio ≥ 3.0 |
| ID-6 window dressing | stem > 40 từ | stem > 70 từ |

#### B. Borderline judgments (cho subjective flags)
Per-question assessments được pre-coded trong `ai_assisted_iwf_scorer.py`
(`ASSESSMENTS` dict). Một số flag được mark `'b'` (borderline) → strict flag
1, lenient flag 0.

### 2. Faithfulness scoring
30 saved outputs từ D3 được rate độc lập với 2 perspective trong
`ai_assisted_faithfulness_scorer.py` (`FAITHFULNESS_JUDGMENTS` dict).
- Strict: tend to score 0.5–1 lower, more readily flag hallucination
- Lenient: more generous; treat plausible inference as acceptable

### 3. Consensus
27 disagreements được resolve một-lần bởi cùng entity, kết quả ghi vào
`scoring/consensus_targets.csv`. **Đây không phải consensus thật giữa 2
người độc lập** — đây là một single judgment call cho mỗi case.

---

## Tại sao đây KHÔNG phải human inter-rater agreement

1. **Cùng underlying model:** Cả 2 reviewer là Claude Opus. Variance giữa
   chúng là **intra-AI threshold variance**, không phải intra-human variance.

2. **Không có blind:** Reviewer A và B "biết" về nhau qua shared codebase.
   Không thể nói là "independent" theo nghĩa của Landis-Koch κ literature.

3. **Cohen's κ không reflect được measurement noise of real human raters:**
   - Human raters: noise đến từ attention, fatigue, expertise, mood
   - AI-AI: noise đến từ programmatic threshold differences + pre-coded "b" markers

4. **PABAK supplement:** Vì base rate của nhiều flag rất thấp (1-5/50), Cohen's
   κ collapse to 0 ("kappa paradox"). Báo cáo dùng PABAK = 2·Po − 1 để bổ sung.

---

## Yêu cầu disclose trong thesis

### Đoạn văn cần thêm vào mục 5.4.7 (Threats to Validity)

```
Do giới hạn về thời gian và nhân lực, công đoạn chấm Item Writing Flaws
(50 câu × 16 flag) và verify faithfulness (30 mẫu) trong nghiên cứu này
không được thực hiện bởi 2 reviewer người độc lập. Thay vào đó, nhóm áp
dụng một phương pháp AI-assisted scoring với 2 perspective khác nhau
(Strict & Lenient) trên cùng model nền (Claude Opus). Cohen's κ báo cáo
trong các bảng 5.x.8 và 5.x.4.F do đó phản ánh intra-AI variance qua
các threshold rubric khác nhau, KHÔNG phải true human inter-rater
agreement. Để bổ sung khi Cohen's κ collapse do base rate cực thấp
("kappa paradox"), nhóm báo cáo thêm chỉ số PABAK = 2·Po − 1.

Đây được coi là pilot evaluation; human rescoring với 2 reviewer thật
là future work khuyến nghị mạnh.
```

### Khi hội đồng hỏi "ai chấm 50 câu IWF?"

> **Trung thực:** "Do hạn chế thời gian trước bảo vệ, em áp dụng AI-assisted
> scoring với 2 perspective (strict/lenient) trên cùng model Claude Opus.
> Em đã ghi rõ trong limitations mục 5.4.7. Future work là human rescoring."

> **KHÔNG nói:** "2 người trong nhóm chấm" — vì đó là không trung thực.

---

## Tham số reproducibility

| Tham số | Giá trị |
|---|---|
| Model nền | claude-opus-4-7 |
| Scoring script | `ai_assisted_iwf_scorer.py` (commit hash trong git log) |
| Faithfulness script | `ai_assisted_faithfulness_scorer.py` |
| Per-question judgments | Embedded trong `ASSESSMENTS` dict / `FAITHFULNESS_JUDGMENTS` dict |
| Date | 2026-05-24 |

Việc chạy lại cùng scripts với cùng input sẽ cho ra **cùng kết quả** —
không có randomness.

---

## Khi nào nên upgrade sang human rescoring

Sau bảo vệ, nếu có cơ hội publish paper hoặc mở rộng nghiên cứu, nên:

1. Recruit ≥ 2 instructional designer / curriculum specialist thật
2. Cho họ rubric IWF + pedagogical (đã refine từ pilot này)
3. Họ chấm 50 câu độc lập (mỗi người ~3-5h)
4. Tính κ thật + so sánh với AI-assisted pilot này
5. Nếu κ cao giữa human raters → AI methodology được validate
6. Nếu κ thấp → có thông tin về flag nào subjective hơn

Đây cũng là một phần defendable: nhóm có rubric, có pilot data, có script
reusable. Việc thiếu real human raters là execution gap, không phải method gap.
