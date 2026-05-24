#!/usr/bin/env python3
"""
Generate empty CSV scoring templates for D4 human evaluation.

Two-sheet workflow (one per reviewer, must score independently):

A. IWF + Pedagogical scoring (50 quiz questions)
   - 12 IWF flaw columns (binary 0/1)
   - 4 pedagogical criteria columns (binary 0/1)
   - 1 Bloom level column (1-6, optional)
   - notes column (free text)
   Output: scoring/iwf_reviewerA.csv, scoring/iwf_reviewerB.csv

B. Faithfulness human verification (30 outputs, used to cross-validate LLM judge)
   - faithfulness_score 1-5
   - hallucination yes/no
   - notes
   Output: scoring/faithfulness_human_reviewerA.csv, scoring/faithfulness_human_reviewerB.csv

The CSVs are pre-populated with the question/output content so reviewers
don't have to dig into JSON files.

Usage:
    python3 generate_scoring_templates.py
"""

from __future__ import annotations

import csv
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).parent
OUTPUTS_DIR = ROOT / "results" / "outputs"
SCORING_DIR = ROOT / "scoring"
GROUND_TRUTH = ROOT / "ground-truth" / "ground_truth.json"

# IWF + pedagogical columns the reviewer fills (0/1)
IWF_FLAGS = [
    "TW-1_length_cue",
    "TW-2_grammatical_cue",
    "TW-3_word_repeat",
    "TW-4_logical_clue",
    "TW-5_absolute_terms",
    "TW-6_vague_terms",
    "ID-1_unclear_stem",
    "ID-2_all_of_the_above",
    "ID-3_negative_unmarked",
    "ID-4_heterogeneous_options",
    "ID-5_implausible_distractor",
    "ID-6_window_dressing",
]
PEDAGOGICAL_FLAGS = [
    "C1_clarity",
    "C2_single_correct",
    "C3_plausible_distractors",
    "C4_source_grounded",
]


def short(s: str, n: int) -> str:
    if s is None:
        return ""
    s = re.sub(r"\s+", " ", str(s)).strip()
    return s if len(s) <= n else s[: n - 1] + "…"


def format_question(q: dict) -> dict:
    """Compact one quiz question into reviewer-friendly text fields."""
    qtype = (q.get("type") or "").upper()
    prompt = q.get("prompt") or ""
    sentence = q.get("sentenceHtml") or ""
    if not prompt and sentence:
        prompt = f"[sentenceHtml]: {sentence}"
    elif sentence and prompt:
        prompt = f"{prompt}\n[sentenceHtml]: {sentence}"

    options = q.get("options")
    options_text = ""
    if options and isinstance(options, list):
        options_text = " | ".join(f"{chr(65+i)}) {o}" for i, o in enumerate(options))

    pairs = q.get("pairs")
    if pairs:
        options_text += " [matching pairs: " + "; ".join(
            f"{p.get('left')} -> {p.get('right')}" for p in pairs) + "]"

    ans = q.get("correctAnswer")
    if isinstance(ans, list):
        ans = " | ".join(str(a) for a in ans)
    correct = "" if ans is None else str(ans)
    return {
        "question_type": qtype,
        "prompt_text": short(prompt, 600),
        "options_text": short(options_text, 400),
        "correct_answer_text": short(correct, 200),
        "explanation_text": short(q.get("explanation") or "", 250),
    }


def build_iwf_csv(target: Path) -> int:
    quiz_files = sorted(OUTPUTS_DIR.glob("*_quiz_run*.json"))
    if not quiz_files:
        print("ERROR: no quiz outputs found - run saveOutputsPass first")
        sys.exit(1)

    headers = [
        "qid", "doc_id", "doc_title", "question_idx",
        "question_type", "prompt_text", "options_text",
        "correct_answer_text", "explanation_text",
    ] + IWF_FLAGS + PEDAGOGICAL_FLAGS + ["bloom_level", "notes"]

    rows_written = 0
    with target.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=headers, quoting=csv.QUOTE_MINIMAL)
        writer.writeheader()
        for qf in quiz_files:
            data = json.loads(qf.read_text(encoding="utf-8"))
            parsed = data.get("parsed") or {}
            questions = parsed.get("questions") or []
            for i, q in enumerate(questions, start=1):
                row = {h: "" for h in headers}
                row["qid"] = f"{data['doc_id']}_Q{i}"
                row["doc_id"] = data["doc_id"]
                row["doc_title"] = data.get("doc_title", "")
                row["question_idx"] = i
                row.update(format_question(q))
                writer.writerow(row)
                rows_written += 1
    return rows_written


def load_ground_truth() -> dict[str, list[str]]:
    root = json.loads(GROUND_TRUTH.read_text(encoding="utf-8"))
    out: dict[str, list[str]] = {}
    for d in root["documents"]:
        out[d["doc_id"]] = [f["fact"] for f in d["key_facts"]]
    return out


def summarize_generated(feature: str, parsed: dict) -> str:
    """Render parsed output as compact text for reviewer."""
    if parsed is None:
        return "(no parsed content)"
    if feature == "outline":
        sb = [f"Title: {parsed.get('title', '')}"]
        if parsed.get("description"):
            sb.append(f"Description: {parsed['description']}")
        sb.append("Sections:")
        for j, s in enumerate(parsed.get("sections") or [], start=1):
            sb.append(f"  {j}. {s.get('title', '')}")
            for t in s.get("topics") or []:
                sb.append(f"     - {t}")
        return "\n".join(sb)
    if feature == "page-content":
        html = parsed.get("htmlContent") or ""
        text = re.sub(r"<[^>]+>", " ", html)
        text = re.sub(r"\s+", " ", text).strip()
        return (f"Page title: {parsed.get('pageTitle', '')}\n"
                f"Summary: {parsed.get('shortSummary', '')}\n"
                f"Content (text): {short(text, 2000)}")
    if feature == "quiz":
        lines = []
        for j, q in enumerate(parsed.get("questions") or [], start=1):
            lines.append(f"{j}. [{q.get('type')}] {q.get('prompt') or '(empty)'}")
            if q.get("options"):
                for k, o in enumerate(q["options"]):
                    lines.append(f"   {chr(65+k)}) {o}")
            if q.get("sentenceHtml"):
                lines.append(f"   sentenceHtml: {q['sentenceHtml']}")
            if q.get("correctAnswer") is not None:
                lines.append(f"   Correct: {q['correctAnswer']}")
        return "\n".join(lines)
    return str(parsed)


def build_faithfulness_csv(target: Path) -> int:
    gt = load_ground_truth()
    files = sorted(OUTPUTS_DIR.glob("*.json"))
    if not files:
        print("ERROR: no output files found")
        sys.exit(1)

    headers = [
        "sample_id", "doc_id", "feature", "run",
        "ground_truth_facts", "generated_content",
        "human_faithfulness_score", "human_hallucination", "notes",
    ]

    rows_written = 0
    with target.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=headers, quoting=csv.QUOTE_MINIMAL)
        writer.writeheader()
        for of in files:
            data = json.loads(of.read_text(encoding="utf-8"))
            doc_id = data["doc_id"]
            facts = gt.get(doc_id, [])
            facts_str = "\n".join(f"- {x}" for x in facts)
            generated = summarize_generated(data["feature"], data.get("parsed"))
            row = {h: "" for h in headers}
            row["sample_id"] = f"{doc_id}_{data['feature']}_run{data['run']}"
            row["doc_id"] = doc_id
            row["feature"] = data["feature"]
            row["run"] = data["run"]
            row["ground_truth_facts"] = facts_str
            row["generated_content"] = short(generated, 3000)
            writer.writerow(row)
            rows_written += 1
    return rows_written


def main() -> int:
    SCORING_DIR.mkdir(parents=True, exist_ok=True)

    iwf_a = SCORING_DIR / "iwf_reviewerA.csv"
    iwf_b = SCORING_DIR / "iwf_reviewerB.csv"
    n = build_iwf_csv(iwf_a)
    build_iwf_csv(iwf_b)
    print(f"  Wrote IWF templates ({n} questions): {iwf_a.name}, {iwf_b.name}")

    fh_a = SCORING_DIR / "faithfulness_human_reviewerA.csv"
    fh_b = SCORING_DIR / "faithfulness_human_reviewerB.csv"
    n = build_faithfulness_csv(fh_a)
    build_faithfulness_csv(fh_b)
    print(f"  Wrote faithfulness templates ({n} samples): {fh_a.name}, {fh_b.name}")

    print("\nNext steps:")
    print(f"  1. Open the 4 CSV files in {SCORING_DIR.relative_to(ROOT)}/")
    print("  2. Reviewer A fills *_reviewerA.csv, Reviewer B fills *_reviewerB.csv")
    print("     INDEPENDENTLY (do not share scores before consensus).")
    print("  3. When both done, run: python3 compute_agreement.py")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
