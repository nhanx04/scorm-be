#!/usr/bin/env python3
"""
Tính độ đồng thuận giữa LLM-judge (Gemini Flash) và reviewer thứ 2 trên mẫu
faithfulness RAG (cross-validation cho mục 5.4).

Đọc scoring/rag_faithfulness_crosscheck.csv (đã điền cả gemini_* và claude_*),
tính:
  - % đồng thuận tuyệt đối (exact) và trong khoảng ±1 điểm
  - Mean absolute difference (MAD) của điểm 1-5
  - Quadratic-weighted Cohen's kappa cho điểm 1-5
  - Cohen's kappa (binary) cho cờ hallucination

Không phụ thuộc scipy/numpy.

Usage:
    python3 compute_crosscheck_agreement.py
"""

from __future__ import annotations

import csv
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).parent
SRC = ROOT / "scoring" / "rag_faithfulness_crosscheck.csv"


def cohen_kappa(pairs):
    """Cohen's kappa (unweighted) cho nhãn rời rạc."""
    n = len(pairs)
    if n == 0:
        return 0.0
    labels = sorted({a for a, _ in pairs} | {b for _, b in pairs})
    po = sum(1 for a, b in pairs if a == b) / n
    ca = Counter(a for a, _ in pairs)
    cb = Counter(b for _, b in pairs)
    pe = sum((ca[l] / n) * (cb[l] / n) for l in labels)
    return 1.0 if pe == 1 else (po - pe) / (1 - pe)


def quadratic_weighted_kappa(pairs, lo=1, hi=5):
    """QWK cho điểm thứ tự lo..hi."""
    n = len(pairs)
    if n == 0:
        return 0.0
    k = hi - lo + 1
    O = [[0] * k for _ in range(k)]
    for a, b in pairs:
        O[a - lo][b - lo] += 1
    ra = [sum(O[i]) for i in range(k)]
    cb = [sum(O[i][j] for i in range(k)) for j in range(k)]
    num = den = 0.0
    for i in range(k):
        for j in range(k):
            w = ((i - j) ** 2) / ((k - 1) ** 2)
            e = ra[i] * cb[j] / n
            num += w * O[i][j]
            den += w * e
    return 1.0 if den == 0 else 1 - num / den


def main():
    if not SRC.exists():
        sys.exit(f"Chưa có {SRC} — tạo bằng make_crosscheck_template.py rồi điền claude_*.")
    rows = [r for r in csv.DictReader(open(SRC, encoding="utf-8"))
            if r.get("claude_score", "").strip()]
    if not rows:
        sys.exit("Template chưa được điền cột claude_score.")

    score_pairs = [(int(r["gemini_score"]), int(r["claude_score"])) for r in rows]
    hallu_pairs = [(r["gemini_hallu"].strip().lower(), r["claude_hallu"].strip().lower())
                   for r in rows]

    n = len(score_pairs)
    exact = sum(1 for a, b in score_pairs if a == b) / n * 100
    within1 = sum(1 for a, b in score_pairs if abs(a - b) <= 1) / n * 100
    mad = sum(abs(a - b) for a, b in score_pairs) / n
    qwk = quadratic_weighted_kappa(score_pairs)
    hk = cohen_kappa(hallu_pairs)

    print(f"\nCross-check faithfulness — n = {n} mẫu (Gemini-Flash judge vs reviewer 2)")
    print("-" * 58)
    print(f"  Điểm TB Gemini      : {sum(a for a,_ in score_pairs)/n:.2f}")
    print(f"  Điểm TB reviewer 2  : {sum(b for _,b in score_pairs)/n:.2f}")
    print(f"  Đồng thuận tuyệt đối: {exact:.0f}%")
    print(f"  Đồng thuận ±1 điểm  : {within1:.0f}%")
    print(f"  MAD (1-5)           : {mad:.2f}")
    print(f"  Quadratic-weighted κ: {qwk:.2f}  ({kappa_label(qwk)})")
    print(f"  Cohen's κ (hallu)   : {hk:.2f}  ({kappa_label(hk)})")


def kappa_label(k):
    if k < 0:
        return "kém hơn ngẫu nhiên"
    if k < 0.20:
        return "rất thấp"
    if k < 0.40:
        return "thấp"
    if k < 0.60:
        return "trung bình"
    if k < 0.80:
        return "khá (substantial)"
    return "rất cao (almost perfect)"


if __name__ == "__main__":
    main()
