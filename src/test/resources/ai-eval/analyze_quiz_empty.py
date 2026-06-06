#!/usr/bin/env python3
"""
Dive into the 35% quiz empty_field finding from D2.

Reads quiz_*.json outputs from results/outputs/ and figures out *which*
question types or fields are causing the empty_field flag to fire.

This is the diagnostic that turns the binary signal from D2 (yes/no
empty fields exist) into actionable insight (e.g. "MATCHING type omits
type field 80% of the time, FILL_IN_THE_BLANK omits sentenceHtml 60%").

Usage:
    python3 analyze_quiz_empty.py
"""

from __future__ import annotations

import json
import os
import sys
from collections import defaultdict, Counter
from pathlib import Path

# Default is the baseline outputs dir; pass a subdir arg for post-fix analysis.
DEFAULT_OUTPUTS_DIR = Path(__file__).parent / "results" / "outputs"


def load_quiz_files(outputs_dir: Path) -> list[dict]:
    files = sorted(outputs_dir.glob("*_quiz_run*.json"))
    print(f"Found {len(files)} quiz output files in {outputs_dir}")
    return [json.loads(f.read_text(encoding="utf-8")) for f in files]


def check_field_emptiness(question: dict) -> list[str]:
    """Return a list of fields that are missing/null/blank for this question."""
    missing = []
    for f in ("prompt", "type"):
        v = question.get(f)
        if v is None or (isinstance(v, str) and not v.strip()):
            missing.append(f)

    qtype = (question.get("type") or "").upper()
    # Type-specific required fields
    if qtype in ("MCQ_SINGLE", "MCQ_MULTIPLE"):
        opts = question.get("options")
        if not opts or len(opts) < 2:
            missing.append("options")
        if question.get("correctAnswer") is None:
            missing.append("correctAnswer")
    elif qtype == "TRUE_FALSE":
        if question.get("correctAnswer") is None:
            missing.append("correctAnswer")
    elif qtype == "SHORT_ANSWER":
        ans = question.get("correctAnswer")
        if not ans or (isinstance(ans, list) and len(ans) == 0):
            missing.append("correctAnswer")
    elif qtype == "FILL_IN_THE_BLANK":
        sh = question.get("sentenceHtml")
        if sh is None or (isinstance(sh, str) and not sh.strip()):
            missing.append("sentenceHtml")
        ans = question.get("correctAnswer")
        if not ans or (isinstance(ans, list) and len(ans) == 0):
            missing.append("correctAnswer")
    elif qtype == "MATCHING":
        pairs = question.get("pairs")
        if not pairs or len(pairs) < 2:
            missing.append("pairs")

    return missing


def main() -> int:
    outputs_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_OUTPUTS_DIR
    if not outputs_dir.is_absolute():
        outputs_dir = Path(__file__).parent / outputs_dir
    quizzes = load_quiz_files(outputs_dir)
    if not quizzes:
        print(f"ERROR: no quiz JSON files in {outputs_dir}. "
              f"Run saveOutputsPass or quizOnlyPostFixPass first.")
        return 1

    total_questions = 0
    type_counter: Counter[str] = Counter()
    type_with_empty_field: Counter[str] = Counter()
    field_missing_by_type: dict[str, Counter[str]] = defaultdict(Counter)
    per_quiz_empty = 0
    per_quiz_type_distribution: list[tuple[str, list[str]]] = []

    for quiz_file in quizzes:
        parsed = quiz_file.get("parsed")
        if not parsed:
            continue
        questions = parsed.get("questions") or []
        types_in_quiz = [q.get("type", "<missing>") for q in questions]
        per_quiz_type_distribution.append((quiz_file["doc_id"], types_in_quiz))

        quiz_has_empty = False
        for q in questions:
            total_questions += 1
            qtype = (q.get("type") or "<missing>").upper()
            type_counter[qtype] += 1
            missing = check_field_emptiness(q)
            if missing:
                type_with_empty_field[qtype] += 1
                quiz_has_empty = True
                for f in missing:
                    field_missing_by_type[qtype][f] += 1
        if quiz_has_empty:
            per_quiz_empty += 1

    print("\n" + "=" * 88)
    print("PHÂN TÍCH 35% QUIZ EMPTY_FIELD - ROOT CAUSE")
    print("=" * 88)

    print(f"\nTổng số quiz files: {len(quizzes)}")
    print(f"Tổng số câu hỏi:    {total_questions}")
    print(f"Quiz có ≥1 empty:    {per_quiz_empty}/{len(quizzes)} "
          f"({100*per_quiz_empty/max(1, len(quizzes)):.0f}%)")

    print("\n" + "-" * 88)
    print(f"{'Question Type':<22} {'Total':>8} {'With empty field':>20} {'%':>8}")
    print("-" * 88)
    for qtype in sorted(type_counter, key=lambda t: -type_counter[t]):
        total = type_counter[qtype]
        empty = type_with_empty_field[qtype]
        pct = 100 * empty / total if total else 0
        print(f"{qtype:<22} {total:>8} {empty:>20} {pct:>7.1f}%")

    print("\n" + "-" * 88)
    print("FIELD MISSING BY QUESTION TYPE (chi tiết)")
    print("-" * 88)
    for qtype in sorted(field_missing_by_type, key=lambda t: -type_counter[t]):
        if not field_missing_by_type[qtype]:
            continue
        print(f"\n  {qtype} (total: {type_counter[qtype]}):")
        for field, n in field_missing_by_type[qtype].most_common():
            print(f"    - {field}: missing in {n}/{type_counter[qtype]} "
                  f"({100*n/type_counter[qtype]:.0f}%)")

    print("\n" + "-" * 88)
    print("KIỂM TRA PHÂN BỐ LOẠI CÂU HỎI THEO YÊU CẦU")
    print("-" * 88)
    print("Prompt yêu cầu: 6 loại MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE, "
          "SHORT_ANSWER, FILL_IN_THE_BLANK, MATCHING")
    print(f"\nMỗi quiz có 5 câu, expected: 5 loại khác nhau (or close to 5).")
    print()
    print(f"{'Doc':<6} {'Distribution':<70}")
    print("-" * 88)
    for doc_id, types in per_quiz_type_distribution:
        print(f"{doc_id:<6} {', '.join(types)}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
