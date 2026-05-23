#!/usr/bin/env python3
"""
Analyze AI evaluation CSVs and print summary tables for thesis section 5.4.

Usage:
    python3 analyze_results.py [results_dir]

By default reads from `src/test/resources/ai-eval/results/`. Picks the most
recent reliability_*.csv and latency_cost_*.csv pair.

Outputs:
- Bảng 5.x.2 - Reliability per feature
- Bảng 5.x.3 - Latency & cost per feature
- Extra - per-document breakdown
- Extra - scatter data hints for thesis plot
"""

from __future__ import annotations

import csv
import glob
import os
import statistics
import sys
from collections import defaultdict
from pathlib import Path


def find_latest(pattern: str) -> str | None:
    files = sorted(glob.glob(pattern))
    return files[-1] if files else None


def read_csv(path: str) -> list[dict]:
    with open(path, encoding="utf-8") as f:
        return list(csv.DictReader(f))


def pct(num: int, total: int) -> str:
    if total == 0:
        return "n/a"
    return f"{num}/{total} ({100.0 * num / total:.1f}%)"


def percentile(values: list[float], p: float) -> float:
    if not values:
        return 0.0
    s = sorted(values)
    k = (len(s) - 1) * p
    f = int(k)
    c = min(f + 1, len(s) - 1)
    if f == c:
        return s[f]
    return s[f] + (s[c] - s[f]) * (k - f)


def reliability_table(rows: list[dict]) -> None:
    by_feature: dict[str, list[dict]] = defaultdict(list)
    for r in rows:
        by_feature[r["feature"]].append(r)

    print("\n" + "=" * 88)
    print("BẢNG 5.x.2 — Độ tin cậy định dạng đầu ra (Reliability)")
    print("=" * 88)
    fmt = "{:<14} {:>6} {:>16} {:>16} {:>14} {:>14}"
    print(fmt.format("Feature", "N", "API OK", "JSON parse OK",
                     "Schema OK", "Markdown fence"))
    print("-" * 88)
    for feat in sorted(by_feature):
        sub = by_feature[feat]
        n = len(sub)
        api_ok = sum(1 for r in sub if r["api_ok"] == "true")
        parse_ok = sum(1 for r in sub if r["json_parse_ok"] == "true")
        schema_ok = sum(1 for r in sub if r["schema_ok"] == "true")
        fence = sum(1 for r in sub if r["used_markdown_fence"] == "true")
        print(fmt.format(feat, n, pct(api_ok, n), pct(parse_ok, n),
                         pct(schema_ok, n), pct(fence, n)))

    print("\nTỉ lệ trường rỗng (empty_field):")
    for feat in sorted(by_feature):
        sub = by_feature[feat]
        empty = sum(1 for r in sub if r["empty_field"] == "true")
        print(f"  - {feat}: {pct(empty, len(sub))}")

    err_types = defaultdict(int)
    for r in rows:
        et = r.get("error_type", "").strip()
        if et:
            err_types[et] += 1
    if err_types:
        print("\nLỗi quan sát được:")
        for et, n in sorted(err_types.items(), key=lambda x: -x[1]):
            print(f"  - {et}: {n}")


def latency_cost_table(rows: list[dict]) -> None:
    by_feature: dict[str, list[dict]] = defaultdict(list)
    for r in rows:
        by_feature[r["feature"]].append(r)

    print("\n" + "=" * 88)
    print("BẢNG 5.x.3 — Hiệu năng và chi phí (Latency & Cost)")
    print("=" * 88)
    fmt = "{:<14} {:>4} {:>9} {:>9} {:>9} {:>9} {:>9} {:>11}"
    print(fmt.format("Feature", "N", "p50 (s)", "p95 (s)",
                     "max (s)", "tok_in", "tok_out", "$/call"))
    print("-" * 88)
    for feat in sorted(by_feature):
        sub = by_feature[feat]
        n = len(sub)
        lat = sorted(int(r["latency_ms"]) for r in sub)
        p50 = percentile(lat, 0.5) / 1000.0
        p95 = percentile(lat, 0.95) / 1000.0
        mx = max(lat) / 1000.0
        avg_in = statistics.mean(int(r["prompt_tokens"]) for r in sub)
        avg_out = statistics.mean(int(r["completion_tokens"]) for r in sub)
        avg_cost = statistics.mean(float(r["cost_usd"]) for r in sub)
        print(fmt.format(feat, n, f"{p50:.2f}", f"{p95:.2f}", f"{mx:.2f}",
                         f"{avg_in:.0f}", f"{avg_out:.0f}", f"${avg_cost:.4f}"))

    total_cost = sum(float(r["cost_usd"]) for r in rows)
    total_calls = len(rows)
    print(f"\nTổng: {total_calls} calls, tổng chi phí = ${total_cost:.4f}")
    if rows:
        print(f"Cumulative cost cuối = ${float(rows[-1]['cumulative_cost_usd']):.4f}")


def per_document_breakdown(rel_rows: list[dict], lat_rows: list[dict]) -> None:
    by_doc_lat: dict[str, list[dict]] = defaultdict(list)
    for r in lat_rows:
        by_doc_lat[r["doc_id"]].append(r)
    by_doc_rel: dict[str, list[dict]] = defaultdict(list)
    for r in rel_rows:
        by_doc_rel[r["doc_id"]].append(r)

    print("\n" + "=" * 88)
    print("BẢNG 5.x.bonus — Phân tích theo từng tài liệu")
    print("=" * 88)
    fmt = "{:<6} {:>6} {:>5} {:>10} {:>10} {:>10} {:>10}"
    print(fmt.format("Doc", "pages", "calls", "avg_lat(s)",
                     "avg_in_tok", "avg_$/call", "schema_ok%"))
    print("-" * 88)
    for doc in sorted(by_doc_lat):
        sub = by_doc_lat[doc]
        rel = by_doc_rel[doc]
        avg_lat = statistics.mean(int(r["latency_ms"]) for r in sub) / 1000.0
        avg_in = statistics.mean(int(r["prompt_tokens"]) for r in sub)
        avg_cost = statistics.mean(float(r["cost_usd"]) for r in sub)
        ok = sum(1 for r in rel if r["schema_ok"] == "true")
        pages = sub[0].get("doc_pages", "?")
        print(fmt.format(doc, pages, len(sub),
                         f"{avg_lat:.2f}", f"{avg_in:.0f}",
                         f"${avg_cost:.4f}", f"{100.0 * ok / len(rel):.0f}%"))


def scatter_data(lat_rows: list[dict]) -> None:
    print("\n" + "=" * 88)
    print("DỮ LIỆU CHO BIỂU ĐỒ scatter latency-vs-doc-size (outline calls)")
    print("=" * 88)
    print("doc_pages\tlatency_s\tdoc_id")
    for r in lat_rows:
        if r["feature"] == "outline":
            print(f"{r['doc_pages']}\t"
                  f"{int(r['latency_ms']) / 1000.0:.2f}\t{r['doc_id']}")


def main() -> int:
    results_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        os.path.dirname(__file__), "results")
    results_dir = os.path.abspath(results_dir)
    print(f"Reading results from: {results_dir}")

    # Skip smoke_* — they're not from the full eval run.
    rel_path = find_latest(os.path.join(results_dir, "reliability_*.csv"))
    lat_path = find_latest(os.path.join(results_dir, "latency_cost_*.csv"))

    if not rel_path or not lat_path:
        print("ERROR: no reliability_*.csv / latency_cost_*.csv found.")
        print("Run the test first: mvn test -Dai-eval.enabled=true "
              "-Dtest=AiQualityEvaluationTest#fullEvaluation")
        return 1

    print(f"  reliability: {Path(rel_path).name}")
    print(f"  latency:     {Path(lat_path).name}")

    rel_rows = read_csv(rel_path)
    lat_rows = read_csv(lat_path)
    print(f"  rows: reliability={len(rel_rows)}, latency={len(lat_rows)}")

    reliability_table(rel_rows)
    latency_cost_table(lat_rows)
    per_document_breakdown(rel_rows, lat_rows)
    scatter_data(lat_rows)
    return 0


if __name__ == "__main__":
    sys.exit(main())
