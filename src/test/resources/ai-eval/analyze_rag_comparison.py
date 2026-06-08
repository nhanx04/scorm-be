#!/usr/bin/env python3
"""
Phân tích kết quả thí nghiệm RAG vs full-document stuffing.

Đọc CSV do RagComparisonEvaluationTest sinh ra (results/rag_comparison_*.csv),
tổng hợp theo `strategy` (STUFF vs RAG) và in bảng so sánh + % thay đổi để
đưa thẳng vào báo cáo mục 5.4.

Usage:
    python3 analyze_rag_comparison.py [path/to/rag_comparison_*.csv]
    # không truyền path -> tự lấy file rag_comparison_*.csv mới nhất trong results/
"""

from __future__ import annotations

import csv
import glob
import statistics
import sys
from pathlib import Path

ROOT = Path(__file__).parent
RESULTS = ROOT / "results"


def latest_csv() -> Path:
    files = sorted(glob.glob(str(RESULTS / "rag_comparison_*.csv")))
    if not files:
        sys.exit("Không tìm thấy results/rag_comparison_*.csv — hãy chạy RagComparisonEvaluationTest trước.")
    return Path(files[-1])


def fnum(row, key):
    try:
        return float(row[key])
    except (KeyError, ValueError):
        return None


def agg(rows, key):
    vals = [v for v in (fnum(r, key) for r in rows) if v is not None]
    return statistics.mean(vals) if vals else 0.0


def pct(stuff, rag):
    if stuff == 0:
        return 0.0
    return (rag - stuff) / stuff * 100.0


def main():
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else latest_csv()
    with open(path, newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))

    stuff = [r for r in rows if r.get("strategy") == "STUFF"]
    rag = [r for r in rows if r.get("strategy") == "RAG"]

    # faithfulness: bỏ điểm 0 (judge lỗi / không có ground truth)
    def faith(rs):
        vals = [int(r["faithfulness_score"]) for r in rs
                if r.get("faithfulness_score", "0").isdigit() and int(r["faithfulness_score"]) > 0]
        return statistics.mean(vals) if vals else 0.0

    def hallu_rate(rs):
        flags = [1 if str(r.get("hallucination", "")).lower() == "true" else 0 for r in rs]
        return sum(flags) / len(flags) * 100 if flags else 0.0

    print(f"\nFile: {path.name}")
    print(f"Số cặp tài liệu (paired): STUFF={len(stuff)}  RAG={len(rag)}\n")

    metrics = [
        ("Context (ký tự)", "context_chars", "{:.0f}"),
        ("Prompt tokens (input)", "prompt_tokens", "{:.0f}"),
        ("Completion tokens (output)", "completion_tokens", "{:.0f}"),
        ("Latency (ms)", "latency_ms", "{:.0f}"),
        ("Chi phí sinh (USD)", "gen_cost_usd", "{:.5f}"),
    ]

    header = f"{'Chỉ số':30} | {'STUFF':>14} | {'RAG':>14} | {'Δ %':>9}"
    print(header)
    print("-" * len(header))
    for label, key, fmt in metrics:
        s, r = agg(stuff, key), agg(rag, key)
        print(f"{label:30} | {fmt.format(s):>14} | {fmt.format(r):>14} | {pct(s, r):>+8.1f}%")

    # faithfulness (cao hơn = tốt hơn)
    fs, fr = faith(stuff), faith(rag)
    print(f"{'Faithfulness (1-5, ↑)':30} | {fs:>14.2f} | {fr:>14.2f} | {pct(fs, fr):>+8.1f}%")
    hs, hr = hallu_rate(stuff), hallu_rate(rag)
    print(f"{'Hallucination rate %(↓)':30} | {hs:>14.1f} | {hr:>14.1f} | {(hr-hs):>+8.1f} pp")

    # Tổng chi phí
    def total(rs, key):
        return sum(v for v in (fnum(r, key) for r in rs) if v is not None)
    tot_stuff = total(stuff, "gen_cost_usd")
    tot_rag = total(rag, "gen_cost_usd")
    print("\nTổng chi phí sinh (USD):  STUFF=${:.4f}  RAG=${:.4f}  (tiết kiệm {:+.1f}%)".format(
        tot_stuff, tot_rag, pct(tot_stuff, tot_rag)))

    # Kết luận gợi ý
    in_save = -pct(agg(stuff, "prompt_tokens"), agg(rag, "prompt_tokens"))
    print("\nGỢI Ý KẾT LUẬN:")
    print(f"  • RAG giảm ~{in_save:.0f}% input token so với nhồi toàn tài liệu.")
    if fr >= fs:
        print(f"  • Faithfulness KHÔNG giảm (RAG {fr:.2f} vs STUFF {fs:.2f}) — bám nguồn tương đương/tốt hơn.")
    else:
        print(f"  • Faithfulness RAG ({fr:.2f}) thấp hơn STUFF ({fs:.2f}) — cân nhắc tăng top-k.")


if __name__ == "__main__":
    main()
