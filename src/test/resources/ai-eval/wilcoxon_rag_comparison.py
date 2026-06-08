#!/usr/bin/env python3
"""
Kiểm định Wilcoxon signed-rank (paired) cho thí nghiệm RAG vs STUFF (Nhóm F).

- Gộp nhiều lần lặp (run) theo TRUNG BÌNH per (doc, strategy) trước khi so cặp.
- Mỗi tài liệu là một cặp (STUFF, RAG) → tính delta = RAG − STUFF.
- p-value EXACT bằng liệt kê (n ≤ 18) — không phụ thuộc scipy/numpy.
- Báo cáo: median delta, W+, W−, p (two-sided), effect size (rank-biserial).

Usage:
    python3 wilcoxon_rag_comparison.py path/to/rag_comparison*.csv
"""

from __future__ import annotations

import csv
import statistics
import sys
from collections import defaultdict
from itertools import product

# Chỉ số: (cột, "lower"=thấp hơn tốt | "higher"=cao hơn tốt)
METRICS = [
    ("prompt_tokens", "lower"),
    ("completion_tokens", "lower"),
    ("latency_ms", "lower"),
    ("gen_cost_usd", "lower"),
    ("faithfulness_score", "higher"),
]


def load_pairs(path):
    """-> {metric: [(stuff_mean, rag_mean) per doc]} sau khi gộp run."""
    rows = list(csv.DictReader(open(path, newline="", encoding="utf-8")))
    # (doc, strategy) -> {metric: [values]}
    acc = defaultdict(lambda: defaultdict(list))
    for r in rows:
        key = (r["doc_id"], r["strategy"])
        for m, _ in METRICS:
            try:
                v = float(r[m])
            except (KeyError, ValueError):
                continue
            # faithfulness=0 nghĩa judge lỗi/không có GT -> bỏ
            if m == "faithfulness_score" and v == 0:
                continue
            acc[key][m].append(v)
    docs = sorted({d for (d, _) in acc})
    pairs = {m: [] for m, _ in METRICS}
    for d in docs:
        for m, _ in METRICS:
            s = acc[(d, "STUFF")].get(m, [])
            g = acc[(d, "RAG")].get(m, [])
            if s and g:
                pairs[m].append((statistics.mean(s), statistics.mean(g)))
    return docs, pairs


def avg_ranks(absvals):
    """Hạng trung bình (tie-averaged) cho |d|."""
    order = sorted(range(len(absvals)), key=lambda i: absvals[i])
    ranks = [0.0] * len(absvals)
    i = 0
    while i < len(order):
        j = i
        while j + 1 < len(order) and absvals[order[j + 1]] == absvals[order[i]]:
            j += 1
        r = (i + 1 + j + 1) / 2.0  # hạng trung bình 1-based
        for k in range(i, j + 1):
            ranks[order[k]] = r
        i = j + 1
    return ranks


def wilcoxon(deltas):
    """Trả về (n, W+, W-, p_two_sided, rank_biserial)."""
    d = [x for x in deltas if x != 0]  # bỏ cặp bằng nhau
    n = len(d)
    if n == 0:
        return 0, 0.0, 0.0, 1.0, 0.0
    ranks = avg_ranks([abs(x) for x in d])
    w_plus = sum(r for r, x in zip(ranks, d) if x > 0)
    w_minus = sum(r for r, x in zip(ranks, d) if x < 0)
    total = w_plus + w_minus
    # p-value exact bằng liệt kê 2^n cách gán dấu cho các hạng
    if n <= 18:
        dist = defaultdict(float)
        for signs in product([0, 1], repeat=n):
            s = sum(rk for rk, b in zip(ranks, signs) if b)
            dist[round(s, 6)] += 1
        total_count = 2 ** n
        le = sum(c for v, c in dist.items() if v <= w_plus) / total_count
        ge = sum(c for v, c in dist.items() if v >= w_plus) / total_count
        p = min(1.0, 2 * min(le, ge))
    else:
        # normal approx + continuity correction
        import math
        mu = n * (n + 1) / 4.0
        sigma = math.sqrt(n * (n + 1) * (2 * n + 1) / 24.0)
        z = (w_plus - mu - 0.5 * (1 if w_plus > mu else -1)) / sigma
        p = 2 * (1 - 0.5 * (1 + math.erf(abs(z) / math.sqrt(2))))
        p = min(1.0, p)
    rank_biserial = (w_plus - w_minus) / total if total else 0.0
    return n, w_plus, w_minus, p, rank_biserial


def main():
    if len(sys.argv) < 2:
        sys.exit("Usage: wilcoxon_rag_comparison.py <csv>")
    path = sys.argv[1]
    docs, pairs = load_pairs(path)
    print(f"\nFile: {path}")
    print(f"Số tài liệu ghép cặp: {len(docs)}  (đã gộp run theo trung bình per-doc)\n")
    hdr = f"{'Chỉ số':22} | {'n':>2} | {'median Δ(RAG−STUFF)':>20} | {'W+':>6} | {'W-':>6} | {'p(2-sided)':>10} | {'effect r':>8} | sig"
    print(hdr)
    print("-" * len(hdr))
    for m, direction in METRICS:
        pr = pairs[m]
        if not pr:
            print(f"{m:22} | (không có dữ liệu)")
            continue
        deltas = [g - s for (s, g) in pr]
        n, wp, wm, p, rb = wilcoxon(deltas)
        med = statistics.median(deltas)
        sig = "***" if p < 0.001 else "**" if p < 0.01 else "*" if p < 0.05 else "ns"
        good = ("↓ tốt" if direction == "lower" and med < 0
                else "↑ tốt" if direction == "higher" and med > 0
                else "")
        print(f"{m:22} | {n:>2} | {med:>20.4f} | {wp:>6.1f} | {wm:>6.1f} | {p:>10.4f} | {rb:>+8.2f} | {sig} {good}")
    print("\n(*** p<0.001, ** p<0.01, * p<0.05, ns = không ý nghĩa; effect r = rank-biserial −1..+1)")


if __name__ == "__main__":
    main()
