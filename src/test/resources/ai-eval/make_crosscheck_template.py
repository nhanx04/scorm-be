#!/usr/bin/env python3
"""
Tạo template cross-check faithfulness cho mẫu RAG (mục 5.4 — human/2nd-reviewer
verify chéo LLM-judge).

Đọc các output đã lưu (results/rag_outputs/*RAG*.json) do RagComparisonEvaluationTest
sinh khi chạy với -Dragcmp.save-outputs=true, lấy điểm của judge (Gemini Flash)
và để TRỐNG cột reviewer thứ 2 (claude_*) cho người/AI khác chấm độc lập.

Usage:
    python3 make_crosscheck_template.py [n_sample]   # mặc định lấy hết RAG page-content
Output:
    scoring/rag_faithfulness_crosscheck.csv
"""

from __future__ import annotations

import csv
import json
import sys
from pathlib import Path

ROOT = Path(__file__).parent
OUTPUTS = ROOT / "results" / "rag_outputs"
DEST = ROOT / "scoring" / "rag_faithfulness_crosscheck.csv"


def main():
    if not OUTPUTS.is_dir():
        sys.exit(f"Không thấy {OUTPUTS} — chạy test với -Dragcmp.save-outputs=true trước.")
    files = sorted(OUTPUTS.glob("*_page-content_RAG_*.json"))
    if not files:
        sys.exit("Không có output RAG page-content nào trong rag_outputs/.")

    # Lấy run1 của mỗi doc để mẫu trải đều theo tài liệu (1 mẫu/doc).
    by_doc = {}
    for f in files:
        d = json.loads(f.read_text(encoding="utf-8"))
        by_doc.setdefault(d["doc_id"], d)  # giữ cái đầu tiên (run1)
    samples = [by_doc[k] for k in sorted(by_doc)]
    if len(sys.argv) > 1:
        samples = samples[: int(sys.argv[1])]

    DEST.parent.mkdir(parents=True, exist_ok=True)
    with open(DEST, "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(["sample_id", "doc_id", "gemini_score", "gemini_hallu",
                    "claude_score", "claude_hallu", "notes"])
        for d in samples:
            w.writerow([d["sample_id"], d["doc_id"],
                        d["judge_faithfulness_score"],
                        "true" if d["judge_hallucination"] else "false",
                        "", "", ""])  # reviewer 2 điền sau
    print(f"Đã tạo template {DEST} với {len(samples)} mẫu (1/doc).")
    print("Reviewer thứ 2 đọc generated_text trong rag_outputs/<sample_id>.json,")
    print("điền claude_score (1-5) và claude_hallu (true/false), rồi chạy compute_crosscheck_agreement.py")


if __name__ == "__main__":
    main()
