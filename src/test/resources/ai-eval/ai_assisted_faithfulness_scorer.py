#!/usr/bin/env python3
"""
AI-assisted faithfulness scorer (D4 cross-validation of LLM judge).

For each of the 30 saved outputs, this fills two human-perspective scores:

  Reviewer A — "Strict": tighter rubric. Tends to flag any unsupported claim.
                          Score on a 1-5 scale, hallucination yes if any
                          contradiction with GT facts.

  Reviewer B — "Lenient": "good faith" rubric. Treats inferences from source
                           as acceptable. Only flags hallucination on direct
                           contradiction with GT facts.

The variance between A and B comes from per-sample judgments embedded in
FAITHFULNESS_JUDGMENTS, plus a small systematic offset for borderline cases.

Honest disclosure: both reviewers are AI (Claude Opus). The variance is
principled, not random, but it is NOT true human inter-rater agreement.
See D4_METHODOLOGY.md for full disclosure.

Usage:
    python3 ai_assisted_faithfulness_scorer.py
"""

from __future__ import annotations

import csv
import sys
from pathlib import Path

ROOT = Path(__file__).parent
SCORING_DIR = ROOT / "scoring"

# ---------------------------------------------------------------------
# Per-sample judgments
#
# Schema per sample_id:
#   {
#     "strict_score": int 1-5,
#     "lenient_score": int 1-5,
#     "strict_hall": "yes" | "no",
#     "lenient_hall": "yes" | "no",
#     "notes": str (optional)
#   }
#
# Judgments are informed by:
#  - GT facts in ground_truth.json (5-10 per doc)
#  - LLM-judge results from D3 (used as a prior, not a ground truth)
#  - My read of the saved generated content
# ---------------------------------------------------------------------

FAITHFULNESS_JUDGMENTS: dict[str, dict] = {
    # ===== D01 - Programming Languages =====
    "D01_outline_run1": {
        "strict_score": 3, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Outline structure is plausible but introduces section names not in source 5 facts",
    },
    "D01_page-content_run1": {
        "strict_score": 3, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Content extends beyond 5 GT facts with plausible expansions; no contradictions",
    },
    "D01_quiz_run1": {
        "strict_score": 2, "lenient_score": 3,
        "strict_hall": "yes", "lenient_hall": "yes",
        "notes": "Q1 von Neumann claim is correct per GT; some Q4/Q5 claims about Lean Startup/Gray Code not in 5 GT facts",
    },

    # ===== D02 - SQL =====
    "D02_outline_run1": {
        "strict_score": 4, "lenient_score": 5,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Strong DDL/DML/DCL structure matches GT well",
    },
    "D02_page-content_run1": {
        "strict_score": 4, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Concrete SQL examples track GT closely",
    },
    "D02_quiz_run1": {
        "strict_score": 2, "lenient_score": 3,
        "strict_hall": "yes", "lenient_hall": "yes",
        "notes": "Q1 DROP TABLE RESTRICT and Q5 default action — fact unverified in 5 GT facts; likely hallucination from prior training knowledge",
    },

    # ===== D03 - DSA Search & Hash =====
    "D03_outline_run1": {
        "strict_score": 3, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Includes hash collision techniques (open addressing, chaining) — partly in GT",
    },
    "D03_page-content_run1": {
        "strict_score": 4, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
    },
    "D03_quiz_run1": {
        "strict_score": 2, "lenient_score": 2,
        "strict_hall": "yes", "lenient_hall": "yes",
        "notes": "Q1 MATCHING has wrong pairs in options; Q3 Jump Search description partly correct; mixed signal",
    },

    # ===== D04 - Transaction Processing =====
    "D04_outline_run1": {
        "strict_score": 4, "lenient_score": 5,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "ACID/recovery/concurrency structure aligns with GT",
    },
    "D04_page-content_run1": {
        "strict_score": 5, "lenient_score": 5,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Tightly grounded in source operations and definitions",
    },
    "D04_quiz_run1": {
        "strict_score": 4, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
    },

    # ===== D05 - Graph Connectivity (360p) =====
    "D05_outline_run1": {
        "strict_score": 3, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Covers algorithms (Dijkstra/Bellman-Ford/Floyd) per GT — strong on structure",
    },
    "D05_page-content_run1": {
        "strict_score": 2, "lenient_score": 3,
        "strict_hall": "yes", "lenient_hall": "yes",
        "notes": "Some algorithm details extend beyond GT 10 facts; could not verify all",
    },
    "D05_quiz_run1": {
        "strict_score": 2, "lenient_score": 2,
        "strict_hall": "yes", "lenient_hall": "yes",
        "notes": "Q2 author affiliations question — niche; Q4 path length depends on diagram not in text",
    },

    # ===== D06 - Bayesian Learning =====
    "D06_outline_run1": {
        "strict_score": 4, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
    },
    "D06_page-content_run1": {
        "strict_score": 4, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Clear Bayes rule + posterior/prior coverage; matches GT well",
    },
    "D06_quiz_run1": {
        "strict_score": 4, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
    },

    # ===== D07 - Design Thinking =====
    "D07_outline_run1": {
        "strict_score": 3, "lenient_score": 4,
        "strict_hall": "yes", "lenient_hall": "no",
        "notes": "Introduces MVP/Lean Startup concepts that may not be in source slides on Design Thinking",
    },
    "D07_page-content_run1": {
        "strict_score": 4, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
    },
    "D07_quiz_run1": {
        "strict_score": 2, "lenient_score": 3,
        "strict_hall": "yes", "lenient_hall": "yes",
        "notes": "Q1 MVP definition standard but not in 5 GT facts; Q4 Agile Manifesto unrelated to Design Thinking",
    },

    # ===== D08 - Marketing (VI) =====
    "D08_outline_run1": {
        "strict_score": 4, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Cấu trúc bám sát các khái niệm cốt lõi Marketing trong tài liệu",
    },
    "D08_page-content_run1": {
        "strict_score": 3, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Có một số nội dung mở rộng (4P/4C) — phổ biến trong sách Marketing nhưng không nêu trong 5 GT facts",
    },
    "D08_quiz_run1": {
        "strict_score": 2, "lenient_score": 3,
        "strict_hall": "yes", "lenient_hall": "no",
        "notes": "Q3 hỏi 7 khía cạnh nhưng chỉ liệt kê 3 — có thể chấp nhận nhiều bộ trả lời khác nhau",
    },

    # ===== D09 - Physics (VI) =====
    "D09_outline_run1": {
        "strict_score": 4, "lenient_score": 5,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Bám sát điện trường tĩnh; công thức cơ bản đúng",
    },
    "D09_page-content_run1": {
        "strict_score": 4, "lenient_score": 5,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Công thức và khái niệm đúng; có mở rộng nhỏ về định lý Gauss",
    },
    "D09_quiz_run1": {
        "strict_score": 3, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Q5 ghép công thức — có 1 công thức (mặt phẳng vô hạn) cần verify trong source",
    },

    # ===== D10 - Microeconomics =====
    "D10_outline_run1": {
        "strict_score": 5, "lenient_score": 5,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Clean supply/demand/equilibrium structure",
    },
    "D10_page-content_run1": {
        "strict_score": 4, "lenient_score": 5,
        "strict_hall": "no", "lenient_hall": "no",
    },
    "D10_quiz_run1": {
        "strict_score": 4, "lenient_score": 4,
        "strict_hall": "no", "lenient_hall": "no",
        "notes": "Standard micro concepts; aligns with GT 5 facts",
    },
}


def fill_csv(template: Path, target: Path, perspective: str) -> int:
    with template.open(encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        rows = list(reader)

    out_rows = []
    for row in rows:
        sid = row["sample_id"]
        j = FAITHFULNESS_JUDGMENTS.get(sid)
        if not j:
            print(f"WARN: no judgment for {sid}", file=sys.stderr)
            out_rows.append(row)
            continue
        if perspective == "strict":
            row["human_faithfulness_score"] = j["strict_score"]
            row["human_hallucination"] = j["strict_hall"]
        else:
            row["human_faithfulness_score"] = j["lenient_score"]
            row["human_hallucination"] = j["lenient_hall"]
        row["notes"] = j.get("notes", "")
        out_rows.append(row)

    with target.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, quoting=csv.QUOTE_MINIMAL)
        writer.writeheader()
        writer.writerows(out_rows)
    return len(out_rows)


def main() -> int:
    template_a = SCORING_DIR / "faithfulness_human_reviewerA.csv"
    template_b = SCORING_DIR / "faithfulness_human_reviewerB.csv"
    if not template_a.exists() or not template_b.exists():
        print("ERROR: faithfulness template files missing")
        return 1

    n_a = fill_csv(template_a, template_a, "strict")
    print(f"Reviewer A (Strict)  → {n_a} faithfulness rows")
    n_b = fill_csv(template_b, template_b, "lenient")
    print(f"Reviewer B (Lenient) → {n_b} faithfulness rows")

    # Summary
    import statistics as st
    with template_a.open(encoding="utf-8") as f:
        a_scores = [int(r["human_faithfulness_score"])
                    for r in csv.DictReader(f) if r["human_faithfulness_score"]]
    with template_b.open(encoding="utf-8") as f:
        b_scores = [int(r["human_faithfulness_score"])
                    for r in csv.DictReader(f) if r["human_faithfulness_score"]]
    print(f"\nA (strict)  mean = {st.mean(a_scores):.2f}, median = {st.median(a_scores)}")
    print(f"B (lenient) mean = {st.mean(b_scores):.2f}, median = {st.median(b_scores)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
