#!/usr/bin/env python3
"""
Analyze LLM-as-judge faithfulness results from LlmJudgeTest.

Produces the tables for thesis section 5.4.5 (Faithfulness):
- Bảng 5.x.4.A — Faithfulness per feature (mean / median / std / dist)
- Bảng 5.x.4.B — Hallucination per feature (rate, average unsupported claims)
- Bảng 5.x.4.C — Faithfulness per language (EN vs VI)
- Bảng 5.x.4.D — Faithfulness per document
- Bảng 5.x.4.E — Judge cost & latency summary

Usage:
    python3 analyze_faithfulness.py
"""

from __future__ import annotations

import csv
import glob
import os
import statistics
import sys
from collections import defaultdict
from pathlib import Path

RESULTS_DIR = Path(__file__).parent / "results"


def find_latest(pattern: str) -> str | None:
    files = sorted(glob.glob(pattern))
    return files[-1] if files else None


def read_csv(path: str) -> list[dict]:
    with open(path, encoding="utf-8") as f:
        return list(csv.DictReader(f))


def doc_language_map() -> dict[str, str]:
    """Load doc_id -> language from any saved outputs JSON file."""
    import json
    outputs_dir = RESULTS_DIR / "outputs"
    if not outputs_dir.is_dir():
        return {}
    out: dict[str, str] = {}
    for p in outputs_dir.glob("*.json"):
        try:
            d = json.loads(p.read_text(encoding="utf-8"))
            out[d["doc_id"]] = d.get("doc_language", "?")
        except Exception:
            pass
    return out


def describe_scores(rows: list[dict]) -> dict:
    scores = [int(r["faithfulness_score"]) for r in rows
              if r.get("judge_parse_ok") == "true"]
    if not scores:
        return {"n": 0, "mean": 0, "median": 0, "std": 0,
                "min": 0, "max": 0, "dist": {}}
    dist = {i: scores.count(i) for i in range(1, 6)}
    std = statistics.stdev(scores) if len(scores) > 1 else 0.0
    return {
        "n": len(scores),
        "mean": statistics.mean(scores),
        "median": statistics.median(scores),
        "std": std,
        "min": min(scores),
        "max": max(scores),
        "dist": dist,
    }


def hallucination_stats(rows: list[dict]) -> dict:
    n = len(rows)
    halls = sum(1 for r in rows if r.get("hallucination") == "true")
    claims = [int(r["unsupported_claims_count"]) for r in rows
              if r.get("judge_parse_ok") == "true"]
    return {
        "n": n,
        "hallucinated_count": halls,
        "hallucination_rate": halls / max(1, n),
        "avg_unsupported_claims": statistics.mean(claims) if claims else 0,
        "total_unsupported_claims": sum(claims),
    }


def per_feature(rows: list[dict]) -> None:
    by_feature: dict[str, list[dict]] = defaultdict(list)
    for r in rows:
        by_feature[r["feature"]].append(r)

    print("\n" + "=" * 88)
    print("BẢNG 5.x.4.A — Faithfulness score per feature (auto, judge=Gemini 2.5 Flash)")
    print("=" * 88)
    fmt = "{:<14} {:>4} {:>8} {:>8} {:>8} {:>20}"
    print(fmt.format("Feature", "N", "Mean", "Median", "Std",
                     "Distribution (1/2/3/4/5)"))
    print("-" * 88)
    for feat in sorted(by_feature):
        s = describe_scores(by_feature[feat])
        dist_str = "/".join(str(s["dist"].get(i, 0)) for i in range(1, 6))
        print(fmt.format(feat, s["n"],
                         f"{s['mean']:.2f}", f"{s['median']:.1f}",
                         f"{s['std']:.2f}", dist_str))

    print("\n" + "=" * 88)
    print("BẢNG 5.x.4.B — Hallucination rate per feature")
    print("=" * 88)
    fmt = "{:<14} {:>4} {:>16} {:>22} {:>22}"
    print(fmt.format("Feature", "N", "Hallucinated",
                     "Avg unsupported claims", "Total unsupported claims"))
    print("-" * 88)
    for feat in sorted(by_feature):
        h = hallucination_stats(by_feature[feat])
        hall_str = f"{h['hallucinated_count']}/{h['n']} ({100*h['hallucination_rate']:.0f}%)"
        print(fmt.format(feat, h["n"], hall_str,
                         f"{h['avg_unsupported_claims']:.2f}",
                         h["total_unsupported_claims"]))


def per_language(rows: list[dict], lang_map: dict[str, str]) -> None:
    by_lang: dict[str, list[dict]] = defaultdict(list)
    for r in rows:
        lang = lang_map.get(r["doc_id"], "?")
        by_lang[lang].append(r)

    print("\n" + "=" * 88)
    print("BẢNG 5.x.4.C — Faithfulness theo ngôn ngữ")
    print("=" * 88)
    fmt = "{:<14} {:>4} {:>8} {:>8} {:>8} {:>22}"
    print(fmt.format("Language", "N", "Mean", "Median", "Std",
                     "Hallucination rate"))
    print("-" * 88)
    for lang in sorted(by_lang):
        sub = by_lang[lang]
        s = describe_scores(sub)
        h = hallucination_stats(sub)
        hall_str = f"{h['hallucinated_count']}/{h['n']} ({100*h['hallucination_rate']:.0f}%)"
        print(fmt.format(lang, s["n"],
                         f"{s['mean']:.2f}", f"{s['median']:.1f}",
                         f"{s['std']:.2f}", hall_str))


def per_document(rows: list[dict]) -> None:
    by_doc: dict[str, list[dict]] = defaultdict(list)
    for r in rows:
        by_doc[r["doc_id"]].append(r)

    print("\n" + "=" * 88)
    print("BẢNG 5.x.4.D — Faithfulness theo từng tài liệu")
    print("=" * 88)
    fmt = "{:<6} {:>4} {:>8} {:>22} {:>16}"
    print(fmt.format("Doc", "N", "Mean", "Distribution (1/2/3/4/5)",
                     "Hallucination"))
    print("-" * 88)
    for doc in sorted(by_doc):
        s = describe_scores(by_doc[doc])
        h = hallucination_stats(by_doc[doc])
        dist_str = "/".join(str(s["dist"].get(i, 0)) for i in range(1, 6))
        hall_str = f"{h['hallucinated_count']}/{h['n']}"
        print(fmt.format(doc, s["n"], f"{s['mean']:.2f}", dist_str, hall_str))


def judge_cost(rows: list[dict]) -> None:
    total_cost = sum(float(r["judge_cost_usd"]) for r in rows)
    lat = [int(r["judge_latency_ms"]) for r in rows]
    print("\n" + "=" * 88)
    print("BẢNG 5.x.4.E — Judge cost & latency")
    print("=" * 88)
    print(f"Total judge calls:    {len(rows)}")
    print(f"Total judge cost:     ${total_cost:.4f}")
    print(f"Avg latency:          {statistics.mean(lat):.0f} ms")
    print(f"Median latency:       {statistics.median(lat):.0f} ms")
    print(f"Parse-OK rate:        "
          f"{sum(1 for r in rows if r.get('judge_parse_ok') == 'true')}/{len(rows)}")


def sample_reasoning(rows: list[dict]) -> None:
    print("\n" + "=" * 88)
    print("MẪU REASONING (vài câu để dùng làm ví dụ trong thesis)")
    print("=" * 88)
    low = [r for r in rows if r.get("judge_parse_ok") == "true"
           and int(r["faithfulness_score"]) <= 3]
    high = [r for r in rows if r.get("judge_parse_ok") == "true"
            and int(r["faithfulness_score"]) == 5]

    print("\n--- Output điểm CAO (5/5):")
    for r in high[:3]:
        print(f"  [{r['doc_id']}/{r['feature']}] {r['reasoning']}")

    print("\n--- Output điểm THẤP (≤ 3/5):")
    for r in low[:5]:
        print(f"  [{r['doc_id']}/{r['feature']}] score={r['faithfulness_score']}, "
              f"claims={r['unsupported_claims_count']}: {r['reasoning']}")


def main() -> int:
    path = find_latest(str(RESULTS_DIR / "faithfulness_auto_*.csv"))
    if not path:
        print("ERROR: no faithfulness_auto_*.csv found. Run LlmJudgeTest first.")
        return 1
    print(f"Reading: {path}")

    rows = read_csv(path)
    print(f"Rows: {len(rows)}")
    if not rows:
        return 1

    lang_map = doc_language_map()
    per_feature(rows)
    per_language(rows, lang_map)
    per_document(rows)
    judge_cost(rows)
    sample_reasoning(rows)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
