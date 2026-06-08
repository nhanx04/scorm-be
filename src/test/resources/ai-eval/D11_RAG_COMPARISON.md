# D11 — Đánh giá định lượng: RAG vs Full-document Stuffing

Bổ sung cho mục **5.4 Đánh giá module AI** sau khi tích hợp kỹ thuật **RAG
(Retrieval-Augmented Generation)** cho chức năng sinh nội dung bài học.

## 1. Câu hỏi nghiên cứu

> Khi sinh nội dung bài học bám theo tài liệu nguồn, việc **truy xuất top-k đoạn
> liên quan (RAG)** so với **nhồi toàn bộ tài liệu vào prompt (stuffing)** có
> giảm chi phí/độ trễ mà KHÔNG làm giảm độ trung thực (faithfulness) không?

## 2. Thiết kế thí nghiệm (paired, cùng tài liệu – cùng chủ đề)

Với mỗi tài liệu trong tập test (10 tài liệu, mục 5.4), sinh nội dung cho **cùng
một chủ đề** bằng hai chiến lược grounding, **chỉ khác nhau phần ngữ cảnh** đưa
vào prompt (prompt, model, tham số giữ nguyên):

| Chiến lược | Ngữ cảnh đưa vào prompt |
|---|---|
| **STUFF** (baseline) | Toàn bộ văn bản tài liệu, cắt ở `baseline-max-chars` (mặc định 50.000 ký tự để tránh vượt context window và giới hạn chi phí). Đúng hành vi trước khi có RAG. |
| **RAG** | Cắt tài liệu bằng `DocumentChunker` (production) → nhúng bằng `GeminiEmbeddingClient` (production, `gemini-embedding-001` @ 768 chiều) → truy xuất **top-k** chunk gần nhất theo cosine similarity. Mặc định k=6, chunk 1200/overlap 200 (khớp production). |

- **Retrieval trong eval** chạy in-memory (cosine) — cho **thứ hạng tương đương**
  truy vấn pgvector `<=>` trong production, nhưng không cần DB để chạy đánh giá.
- **Generation**: Gemini 2.5 Pro (pin theo `eval-config.yaml`).
- **Faithfulness**: LLM-judge (Gemini 2.5 Flash) chấm điểm 1–5 + cờ hallucination,
  đối chiếu với **key facts** trong `ground_truth.json` (cùng phương pháp mục 5.4.5).

### Chỉ số ghi nhận (mỗi lần sinh)
`context_chars`, `prompt_tokens` (input), `completion_tokens`, `total_tokens`,
`latency_ms`, `gen_cost_usd` (hiệu quả) và `faithfulness_score`, `hallucination`,
`unsupported_claims_count` (chất lượng). Ghi ra `results/rag_comparison_*.csv`.

## 3. Cách chạy

```bash
# Smoke (1 tài liệu nhỏ nhất, ~$0.1) — kiểm tra pipeline
mvn test -Dtest='RagComparisonEvaluationTest#smokeRagComparison' -Dai-eval.enabled=true

# Full (toàn bộ tài liệu) — có cơ chế dừng theo budget
mvn test -Dtest='RagComparisonEvaluationTest#fullRagComparison' -Dai-eval.enabled=true

# Phân tích + bảng so sánh đưa vào báo cáo
python3 src/test/resources/ai-eval/analyze_rag_comparison.py
```

Yêu cầu: `GOOGLE_GENAI_API_KEY` trong môi trường (test đọc qua `System.getenv`).
Các tham số có thể override: `-Dragcmp.baseline-max-chars`, `-Dragcmp.top-k`,
`-Dragcmp.chunk-size`, `-Dragcmp.chunk-overlap`.

## 4. Kết quả

### 4.1. Smoke (validation pipeline) — D09 "Trường tĩnh điện", 25 trang, tiếng Việt

| Chỉ số | STUFF | RAG | Δ |
|---|---:|---:|---:|
| Context (ký tự) | 12.276 | 7.081 | −42,3% |
| Prompt tokens (input) | 6.058 | 4.026 | **−33,5%** |
| Completion tokens | 2.166 | 2.228 | +2,9% |
| Latency (ms) | 32.280 | 51.502 | +59,5% |
| Chi phí sinh (USD) | 0,0439 | 0,0375 | **−14,7%** |
| Faithfulness (1–5) | 3 | 3 | bằng |
| Hallucination | không | không | bằng |

Nhận xét sơ bộ (1 mẫu, chưa kết luận thống kê): RAG giảm rõ input token và chi
phí trong khi faithfulness không đổi. Latency một-lần-gọi nhiễu cao (Pro), cần
lấy trung bình nhiều tài liệu ở bản full.

> ⚠️ Đây mới là **smoke 1 tài liệu**. Tài liệu này nhỏ (12k ký tự) nên baseline
> chưa chạm trần 50k → mức tiết kiệm còn khiêm tốn. **Mức tiết kiệm token/chi phí
> sẽ lớn hơn nhiều trên tài liệu dài** (D02 114 trang, D05 360 trang) vì STUFF
> chạm trần ~50k ký tự (~12k+ token) còn RAG luôn giữ ~4k token.

### 4.2. Full — 10 tài liệu, **3 lần lặp/tài liệu** (N=30 cặp sinh; 2026-06-07, Gemini 2.5 Pro)

> **Lưu ý reproducibility.** Ban đầu chạy 1 lần/tài liệu (single-pass), nhưng chỉ
> số faithfulness do AI-judge dao động mạnh giữa các lần (xem 4.3 + Threats). Bản
> báo cáo dưới đây dùng **bộ 3 lần lặp/tài liệu** (`rag_comparison_20260607_205709.csv`,
> N=30 cặp) vì ổn định hơn và đủ cỡ mẫu để kiểm định Wilcoxon paired. Một dòng lỗi
> (D04 RAG run2, `RuntimeException`/`schema_ok=false`) được loại khỏi trung bình
> faithfulness như dữ liệu thiếu — không tính là điểm 0.

**Trung bình toàn bộ 10 tài liệu (paired, N=30):**

| Chỉ số | STUFF | RAG | Δ |
|---|---:|---:|---:|
| Context (ký tự) | 25.993 | 7.026 | −73,0% |
| **Prompt tokens (input)** | **8.868** | **2.783** | **−68,6%** |
| Completion tokens | 2.100 | 1.313 | −37,5% |
| Latency (ms) | 34.126 | 27.223 | −20,2% |
| **Chi phí sinh (USD/lần)** | 0,0531 | 0,0235 | **−55,7%** |
| Faithfulness (1–5, ↑) | 4,03 | 3,34 | **−17,1%** |
| Hallucination rate (↓) | 6,7% | 20,0% | **+13,3 pp** |

⚠️ **Đảo chiều so với single-pass:** bản chạy 1-lần trước đó tình cờ cho faithfulness
bằng nhau (3,60) và RAG khử hallucination ở D05. Bản 3-lần ổn định hơn cho thấy
RAG **kém hơn nhẹ** ở cả hai chỉ số chất lượng — đây là **đánh đổi hiệu quả ↔ độ
phủ ngữ cảnh**, không phải lợi thế chất lượng. Xem phân tích ở 4.3–4.4.

**Chi tiết theo từng tài liệu (faith/hallu = trung bình 3 lần):**

| Doc | Trang | in(STUFF) | in(RAG) | Δtok | faith S→R | hallu S→R |
|---|---:|---:|---:|---:|:--:|:--:|
| D01 | 36 | 5.152 | 2.161 | −58% | 3,67→3,00 | 33%→33% |
| D02 | 114 | 15.632 | 3.310 | −79% | 4,67→3,33 | – |
| D03 | 124 | 17.114 | 1.995 | −88% | 2,67→2,67 | 33%→33% |
| D04 | 65 | 7.412 | 2.196 | −70% | 4,00→**4,50** | – |
| **D05** | **360** | 14.576 | 2.522 | −83% | **3,67→2,00** | **0%→100%** |
| D06 | 36 | 6.632 | 2.738 | −59% | 4,00→**4,33** | – |
| D07 | 72 | 5.473 | 2.421 | −56% | **5,00→3,33** | – |
| D08 | 26 | 3.173 | 3.403 | **+7%** | 5,00→4,33 | – |
| D09 | 25 | 6.058 | 4.026 | −34% | 3,00→2,67 | 0%→33% |
| D10 | 57 | 7.461 | 3.059 | −59% | 5,00→3,67 | – |

> Token/độ dài tài liệu mang tính tất định nên cột input token **trùng khít** bản
> single-pass. Faithfulness/hallucination thì khác hẳn — minh hoạ độ nhiễu của
> AI-judge. D05 (360 trang) đảo chiều rõ nhất: 3-lần cho RAG **xấu hơn** (hallu
> 0%→100%), trái với kết luận single-pass ban đầu.

**Token tiết kiệm tăng theo độ dài tài liệu (luận điểm chính — vững):**

| Nhóm độ dài | #doc | input(STUFF) tb | input(RAG) tb | Giảm token |
|---|---:|---:|---:|---:|
| Ngắn (≤40 trang) | 4 | 5.254 | 3.082 | −41% |
| Trung bình (41–100 trang) | 3 | 6.782 | 2.559 | −62% |
| Dài (>100 trang) | 3 | 15.774 | 2.609 | **−83%** |

### 4.3. Kiểm định ý nghĩa thống kê (Wilcoxon signed-rank, paired)

Mỗi tài liệu là một cặp (STUFF, RAG); gộp các lần lặp theo trung bình per-doc
trước khi so cặp. p-value **exact** (liệt kê, n≤10), bỏ cặp delta=0 (tie).
Chạy: `python3 wilcoxon_rag_comparison.py results/rag_comparison_20260607_205709.csv`.

**Page-content (3 lần/tài liệu):**

| Chỉ số | n | median Δ (RAG−STUFF) | p (2-phía) | effect r | Kết luận |
|---|---:|---:|---:|---:|---|
| Prompt tokens (↓ tốt) | 10 | −4.148 | **0,0039** | −0,96 | RAG giảm, **có ý nghĩa** |
| Completion tokens (↓) | 10 | −658 | **0,0059** | −0,93 | RAG giảm, **có ý nghĩa** |
| Chi phí sinh (↓) | 10 | −0,025 | **0,0020** | −1,00 | RAG giảm, **có ý nghĩa** |
| Latency (↓) | 10 | −8.726 | 0,084 | −0,64 | xu hướng giảm, **chưa có ý nghĩa** |
| Faithfulness (↑ tốt) | 9 | **−0,67** | **0,0273** | −0,82 | RAG **thấp hơn**, có ý nghĩa |

**Focused-quiz (1 lần/tài liệu — xem cảnh báo metric ở 4.5):**

| Chỉ số | n | median Δ (RAG−STUFF) | p (2-phía) | effect r | Kết luận |
|---|---:|---:|---:|---:|---|
| Prompt tokens (↓ tốt) | 10 | −4.150 | **0,0039** | −0,96 | RAG giảm, **có ý nghĩa** |
| Chi phí sinh (↓) | 10 | −0,015 | **0,0039** | −0,96 | RAG giảm, **có ý nghĩa** |
| Completion tokens (↓) | 10 | −33 | 0,232 | −0,45 | chưa có ý nghĩa |
| Latency (↓) | 10 | −2.075 | 0,065 | −0,67 | chưa có ý nghĩa |
| Faithfulness (↑) | 4 | 0 | 0,500 | +0,60 | n quá nhỏ — không kết luận |

→ **Hiệu quả (token, chi phí) có ý nghĩa thống kê mạnh ở cả hai chức năng**
(p≤0,004, effect ~−1,0). **Latency chỉ là xu hướng** (chưa có ý nghĩa). **Faithfulness
của page-content giảm có ý nghĩa** (p=0,027); của quiz không kết luận được do cỡ
mẫu/metric (xem 4.5).

### 4.4. Nhận xét

1. **Hiệu quả (đóng góp chính, có ý nghĩa thống kê): RAG giảm ~69% input token
   và ~56% chi phí** (p≤0,004), mức giảm **tăng dần theo độ dài tài liệu** (−41%
   ngắn → −83% dài). RAG giữ ngữ cảnh gần như cố định (~2.700 token) bất kể tài
   liệu lớn cỡ nào — tức **khả năng mở rộng**; stuffing tăng tuyến tính tới khi
   chạm trần/ vượt context window. Đây là kết quả vững và đáng tin nhất của D11.
2. **Chất lượng: RAG kém hơn nhẹ theo AI-judge — đây là một đánh đổi, không phải
   lợi thế.** Trên bộ 3-lần (N=30), faithfulness RAG thấp hơn STUFF (3,34 vs 4,03;
   −17%, **p=0,027**) và hallucination cao hơn (20% vs 6,7%). Nguyên nhân nhất
   quán với cơ chế: **top-k=6 đôi khi bỏ sót đoạn ngữ cảnh mà key-fact judge cần**
   (D05 360 trang đảo chiều mạnh nhất: hallu 0%→100%; D07 5,0→3,3; D10 5,0→3,7).
   ⇒ RAG đổi một phần độ phủ ngữ cảnh để lấy hiệu quả lớn. Hướng giảm đánh đổi:
   **tăng top-k, thêm ngưỡng tương đồng, hoặc reranking.**
3. **Vì sao tin được kết luận "RAG thấp hơn" (không phải chỉ nhiễu judge).**
   Cross-check chéo bằng model thứ hai (mục 4.7) cho thấy **judge khá nhất quán
   theo từng output** (κ hallucination = 1,00; 100% trong ±1 điểm). Do đó độ
   chênh giữa single-pass ("bằng nhau 3,60") và 3-lần ("RAG thấp hơn, p=0,027")
   chủ yếu đến từ **tính ngẫu nhiên của khâu SINH** (mỗi lần model sinh văn bản
   khác nhau) + cỡ mẫu nhỏ, **không phải judge random**. Bộ 3-lần lấy trung bình
   nhiều hơn nên đáng tin hơn ⇒ **RAG thật sự đánh đổi một phần độ bám nguồn.**
   ⚠️ Nhưng GT chỉ 5 fact/doc nên điểm tuyệt đối **lẫn lộn "mở rộng suy luận"
   với "bịa"**: theo D9 (đọc tay), phần lớn câu bị trừ điểm là **inference
   extension, 0 ca mâu thuẫn nguồn**. Vậy faithfulness RAG thấp hơn ≈ RAG **bỏ
   sót/diễn giải khác** (do top-k) chứ không phải sai sự thật. Cần human verify
   để chốt mức độ.
4. **Trung thực về giới hạn áp dụng (đưa vào Threats to Validity):**
   - **Tài liệu nhỏ không lợi**: D08 (26 trang) RAG dùng token nhỉnh hơn (+7%) vì
     top-6 chunk đã lớn hơn cả tài liệu. ⇒ RAG nên áp dụng có điều kiện theo độ
     dài; tài liệu nhỏ có thể nhồi thẳng.
   - Faithfulness do AI-judge (Flash) chấm — proxy, đã cross-check chéo ở 4.7 + D4.

### 4.5. Mở rộng: chức năng RAG thứ hai — `focused-quiz`

Chạy tương tự cho sinh quiz có chủ đề trọng tâm (prompt production
`QuizPrompts.FROM_DOCUMENT_FOCUSED`, chỉ khác ngữ cảnh nguồn). 10 tài liệu,
2026-06-07, chi phí $0.76.

| Chỉ số | STUFF | RAG | Δ |
|---|---:|---:|---:|
| Context (ký tự) | 25.993 | 7.026 | −73,0% |
| **Prompt tokens (input)** | **10.941** | **4.854** | **−55,6%** |
| Latency (ms) | 22.406 | 20.525 | −8,4% |
| **Chi phí sinh (USD/lần)** | 0,0482 | 0,0266 | **−44,9%** |
| Faithfulness (1–5)\* | 1,50 | 1,80 | (không tin cậy — xem ghi chú) |

Wilcoxon (bảng 4.3): token **p=0,0039**, chi phí **p=0,0039** (đều có ý nghĩa);
latency/completion chưa có ý nghĩa; faithfulness n=4, p=0,50 — không kết luận.

**Token tiết kiệm theo độ dài (giống xu hướng page-content):**

| Nhóm độ dài | #doc | input(STUFF) tb | input(RAG) tb | Giảm token |
|---|---:|---:|---:|---:|
| Ngắn (≤40 trang) | 4 | 7.330 | 5.156 | −30% |
| Trung bình (41–100 trang) | 3 | 8.854 | 4.629 | −48% |
| Dài (>100 trang) | 3 | 17.843 | 4.677 | **−74%** |

> \* **Cảnh báo metric**: faithfulness của quiz được chấm bằng LLM-judge đối
> chiếu **chỉ 5 key facts/tài liệu** — quá khắt khe cho quiz vì câu hỏi thường
> hỏi các chi tiết NẰM NGOÀI 5 facts đó → judge gắn cờ "unsupported" cho cả hai
> chiến lược (điểm 1–2, hallucination ~100% ở cả STUFF lẫn RAG). Do đó **metric
> faithfulness ở đây KHÔNG phân biệt được STUFF vs RAG cho quiz** và không nên
> dùng để kết luận chất lượng. Kết quả quiz cần đọc ở **hai chỉ số hiệu quả
> (token, chi phí)** là chính. Chất lượng quiz đã được đo riêng và chặt chẽ hơn
> bằng **runtime quality gate (IWF + Bloom + citation, mục D10)** áp dụng trên
> nguồn thực tế model nhận — phù hợp hơn key-fact judge.

### 4.6. Tổng kết hai chức năng dùng RAG

| Chức năng | Giảm input token | Giảm chi phí | Ý nghĩa TK | Ghi chú chất lượng |
|---|---:|---:|:--:|---|
| page-content | **−68,6%** | −55,7% | p≤0,004 | faithfulness thấp hơn nhẹ (3,34 vs 4,03; p=0,027) — đánh đổi do top-k bỏ sót, judge nhiễu |
| focused-quiz | **−55,6%** | −44,9% | p≤0,004 | chất lượng đo bằng IWF/citation gate (D10), không phải key-fact judge |

→ Cả hai chức năng tích hợp RAG đều cho **giảm token/chi phí lớn và có ý nghĩa
thống kê, tăng theo độ dài tài liệu** (đóng góp chính). Đổi lại, theo AI-judge
RAG **đánh đổi một phần độ bám nguồn** cho page-content — giảm được bằng tăng
top-k/reranking; với quiz thì metric key-fact không phân biệt được.

### 4.7. Kiểm chứng chéo LLM-judge bằng model thứ hai

Để kiểm tra LLM-judge (Gemini 2.5 Flash) có đáng tin không, lấy **10 mẫu RAG
page-content** (1 mẫu/tài liệu, run1) cho một **model thứ hai (Claude)** chấm
**độc lập** trên CÙNG `key_facts` (faithfulness 1–5 + cờ hallucination), rồi tính
độ đồng thuận. Đây là **cross-check chéo model**, KHÔNG phải human inter-rater
(cùng tinh thần disclosure D4 — xem `D4_METHODOLOGY.md`); không blind vì reviewer
2 thấy điểm Gemini.

Chạy: `python3 make_crosscheck_template.py` → điền `claude_*` →
`python3 compute_crosscheck_agreement.py`. Dữ liệu: `scoring/rag_faithfulness_crosscheck.csv`.

| Chỉ số đồng thuận | Giá trị | Diễn giải |
|---|---:|---|
| Điểm TB Gemini / Claude | 3,40 / 3,80 | Claude lenient hơn ~0,4 điểm |
| Đồng thuận tuyệt đối | 40% | — |
| **Đồng thuận trong ±1 điểm** | **100%** | không cặp nào lệch quá 1 điểm |
| MAD (thang 1–5) | 0,60 | sai khác trung bình nhỏ |
| **Quadratic-weighted κ (điểm)** | **0,66** | khá (substantial) |
| **Cohen's κ (cờ hallucination)** | **1,00** | trùng khớp hoàn toàn (2/10 ca True: D03, D05) |

**Ý nghĩa:** judge Gemini **không phải outlier** — một model khác họ (Claude)
đồng thuận **hoàn hảo về cờ hallucination** và **luôn trong ±1 điểm** về faithfulness.
⇒ judge **nhất quán theo từng output**, nên kết luận "RAG faithfulness thấp hơn"
(mục 4.4) là **đáng tin** chứ không phải nhiễu chấm; phần dao động single-pass vs
3-lần chủ yếu do **khâu sinh** ngẫu nhiên. Lệch hệ thống nhỏ (Claude +0,4) là điều
bình thường giữa hai rubric/model. **Hạn chế:** vẫn là 2 model AI, n=10, không blind,
chỉ page-content run1 → human rescoring vẫn là future work mạnh.

## 5. Mối đe dọa tới tính hợp lệ (Threats to Validity)

- **Faithfulness judge là AI** (Gemini Flash), không phải human — dùng làm proxy.
  Đã cross-check chéo bằng model thứ hai (mục 4.7: κ hallu = 1,00; QWK = 0,66;
  100% trong ±1) + disclosure D4 (xem `D4_METHODOLOGY.md`). Judge nhất quán theo
  output, nhưng vẫn là AI proxy với GT thưa → human rescoring là future work.
- **Faithfulness dao động giữa các lần chạy (do khâu SINH, không phải judge).**
  Cùng cấu hình, single-pass cho STUFF≈RAG (3,60) còn 3-lần (N=30) cho RAG thấp
  hơn có ý nghĩa (3,34 vs 4,03; p=0,027). Vì judge nhất quán (4.7), nguyên nhân
  chính là **mỗi lần model sinh văn bản khác nhau** + GT chỉ 5 fact/doc + n nhỏ.
  ⇒ **không kết luận chất lượng từ một lần chạy**; bộ nhiều lần lặp đáng tin hơn.
  Đóng góp định lượng tin cậy nhất của D11 là **hiệu quả (token/chi phí)** — gần
  như tất định và significant qua mọi lần chạy.
- **Một dòng lỗi sinh/chấm** (D04 RAG, `RuntimeException`) được loại như dữ liệu
  thiếu; nếu tính nhầm thành điểm 0 sẽ làm faithfulness RAG xấu đi giả tạo.
- **Baseline bị cắt 50k ký tự**: STUFF thực tế (không cắt) sẽ tốn token/chi phí
  CAO HƠN nữa hoặc vượt context window → chênh lệch thực tế còn lớn hơn báo cáo.
- **Chủ đề suy ra từ tiêu đề tài liệu**: chủ đề càng hẹp, lợi thế RAG càng rõ.
- **Embedding cắt 3072→768 (MRL)**: đánh đổi nhỏ về chất lượng vector để khớp chi
  phí lưu trữ; cosine không cần normalize.
