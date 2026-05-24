#!/usr/bin/env python3
"""
AI-assisted IWF scorer for D4.

Reads the empty iwf_reviewerA.csv / iwf_reviewerB.csv templates produced
by generate_scoring_templates.py and fills in scores from TWO perspectives:

  Reviewer A — "Strict": conservative academic rubric application.
                          Flag on any reasonable doubt; tighter thresholds.

  Reviewer B — "Lenient": pragmatic educational view.
                           Only flag when clear; benefit-of-doubt.

The output is structurally identical to what 2 humans would produce, but
documents the AI methodology so the thesis can disclose it honestly.

The variance between A and B comes from TWO sources:
  1. Objective flags (programmatic detection): different numeric thresholds
     for STRICT vs LENIENT.
  2. Subjective flags: pre-coded per-question judgments in ASSESSMENTS,
     with some flagged 'b' (borderline) - those become 1 for strict and
     0 for lenient.

Usage:
    python3 ai_assisted_iwf_scorer.py
"""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

ROOT = Path(__file__).parent
SCORING_DIR = ROOT / "scoring"

IWF_FLAGS = [
    "TW-1_length_cue", "TW-2_grammatical_cue", "TW-3_word_repeat",
    "TW-4_logical_clue", "TW-5_absolute_terms", "TW-6_vague_terms",
    "ID-1_unclear_stem", "ID-2_all_of_the_above", "ID-3_negative_unmarked",
    "ID-4_heterogeneous_options", "ID-5_implausible_distractor",
    "ID-6_window_dressing",
]
PED_FLAGS = ["C1_clarity", "C2_single_correct",
             "C3_plausible_distractors", "C4_source_grounded"]

# Absolute / vague term lexicons (EN + VI)
ABSOLUTE_TERMS = [
    r"\balways\b", r"\bnever\b", r"\ball\b", r"\bonly\b", r"\bevery\b",
    r"\bno\s+one\b", r"\bnone\b", r"\bcompletely\b", r"\btotally\b",
    r"\bmust\b", r"\bcannot\b", r"\babsolutely\b",
    r"luôn luôn", r"không bao giờ", r"tất cả", r"duy nhất",
    r"hoàn toàn", r"tuyệt đối", r"bắt buộc",
]
VAGUE_TERMS = [
    r"\busually\b", r"\bsometimes\b", r"\boften\b", r"\bmay\b", r"\bmight\b",
    r"\bcan\b", r"\bperhaps\b", r"\bgenerally\b", r"\btypically\b",
    r"thường", r"có thể", r"đôi khi", r"thường xuyên", r"phần lớn",
]
AOTA_PATTERNS = [
    r"all of the above", r"none of the above",
    r"tất cả đều đúng", r"tất cả các đáp án trên",
    r"không có đáp án nào", r"không đáp án nào",
]
NEGATIVE_MARKERS_RAW = ["NOT", "EXCEPT", "FALSE"]  # plus Vietnamese KHÔNG handled specially


# ---------------------------------------------------------------------
# Subjective assessments per qid (manually coded after reading questions)
#
# Schema per qid:
#   {
#     "judgment": { flag_name: 0 | 1 | "b" },   # "b" = borderline (strict=1, lenient=0)
#                                               # for pedagogical: "b" = borderline (strict=0, lenient=1)
#     "bloom": int,                              # 1-6 Bloom Taxonomy level
#     "notes_a": str (optional),                 # strict reviewer note
#     "notes_b": str (optional),                 # lenient reviewer note
#   }
#
# Flags omitted are assumed clean (IWF flags default 0; ped flags default 1).
# ---------------------------------------------------------------------

ASSESSMENTS: dict[str, dict] = {
    # ===== D01 - Programming Languages (EN) =====
    "D01_Q1": {
        "judgment": {"ID-5": "b", "C3": "b"},  # Quantum Architecture borderline plausibility
        "bloom": 2,
        "notes_a": "Quantum Architecture distractor is weak — too obscure to be plausible",
        "notes_b": "Quantum Architecture acceptable as distractor — real concept",
    },
    "D01_Q2": {
        "judgment": {},
        "bloom": 2,
    },
    "D01_Q3": {
        "judgment": {},  # TF: no options/distractors to evaluate
        "bloom": 2,
    },
    "D01_Q4": {
        # FILL_IN_THE_BLANK with empty prompt (per D3 finding)
        "judgment": {"ID-1": 1},  # missing prompt → unclear stem
        "bloom": 2,
        "notes_a": "Prompt field is empty; only sentenceHtml present",
        "notes_b": "Prompt field is empty; only sentenceHtml present",
    },
    "D01_Q5": {
        "judgment": {},
        "bloom": 1,  # Remember (matching pairs)
    },

    # ===== D02 - SQL (EN) =====
    "D02_Q1": {
        "judgment": {"ID-6": "b"},  # stem is technical-heavy
        "bloom": 3,  # Apply
    },
    "D02_Q2": {
        "judgment": {},
        "bloom": 2,
    },
    "D02_Q3": {
        "judgment": {"ID-1": 1},  # empty prompt (D3 finding)
        "bloom": 1,
        "notes_a": "Empty prompt; sentenceHtml only",
    },
    "D02_Q4": {
        "judgment": {},
        "bloom": 1,
    },
    "D02_Q5": {
        # Per D3 LLM-judge: D02 quiz hallucinated 'DROP TABLE RESTRICT default behavior'.
        # This question asks the default action — answer 'RESTRICT' is not directly
        # supported in source slides. Flagging C4 (source grounded) = 0.
        "judgment": {"C4": 0},
        "bloom": 1,
        "notes_a": "Default referential action not stated in source slides; AI inferred",
        "notes_b": "Default referential action not stated in source slides; AI inferred",
    },

    # ===== D03 - DSA Searching & Hash (EN) =====
    "D03_Q1": {
        "judgment": {"C2": "b"},  # MATCHING with intentionally-wrong listed pairs is confusing
        "bloom": 1,
        "notes_a": "Listed pairs in options are intentionally wrong — confusing for students",
    },
    "D03_Q2": {
        "judgment": {},
        "bloom": 2,
    },
    "D03_Q3": {
        "judgment": {},  # TF
        "bloom": 2,
    },
    "D03_Q4": {
        "judgment": {},
        "bloom": 1,
    },
    "D03_Q5": {
        "judgment": {"ID-1": 1},  # empty prompt
        "bloom": 1,
        "notes_a": "Empty prompt; sentenceHtml only",
    },

    # ===== D04 - DB Transaction Processing (EN) =====
    "D04_Q1": {
        "judgment": {},
        "bloom": 4,  # Analyze - identify the problem
    },
    "D04_Q2": {
        "judgment": {},
        "bloom": 2,
    },
    "D04_Q3": {
        "judgment": {},
        "bloom": 1,
    },
    "D04_Q4": {
        "judgment": {},  # has prompt + sentenceHtml both
        "bloom": 1,
    },
    "D04_Q5": {
        "judgment": {},
        "bloom": 2,
    },

    # ===== D05 - Graph Connectivity (EN, 360 pages) =====
    "D05_Q1": {
        "judgment": {},
        "bloom": 1,
    },
    "D05_Q2": {
        # Author affiliation question - "KAIST" is mentioned in source (acknowledgements)
        # so the false option is actually grounded; question is fair.
        "judgment": {"ID-6": "b"},  # checking metadata about authors is unusual question
        "bloom": 1,
        "notes_a": "Question is about author metadata, not subject content — atypical",
    },
    "D05_Q3": {
        "judgment": {},
        "bloom": 1,
    },
    "D05_Q4": {
        "judgment": {},  # path length 4 — visual
        "bloom": 3,  # Apply (reading a diagram)
    },
    "D05_Q5": {
        "judgment": {},  # has prompt + sentence
        "bloom": 1,
    },

    # ===== D06 - Bayesian Learning (EN) =====
    "D06_Q1": {
        "judgment": {},
        "bloom": 2,
    },
    "D06_Q2": {
        "judgment": {},
        "bloom": 2,
    },
    "D06_Q3": {
        "judgment": {},  # TF, distinguishing Learning vs Decision
        "bloom": 4,
    },
    "D06_Q4": {
        "judgment": {},
        "bloom": 1,
    },
    "D06_Q5": {
        "judgment": {"ID-1": 1},  # empty prompt (D3 finding)
        "bloom": 2,
        "notes_a": "Empty prompt; sentenceHtml only",
    },

    # ===== D07 - Design Thinking (EN) =====
    "D07_Q1": {
        # MVP not explicitly defined in source; AI extrapolated from "Lean Startup" context
        "judgment": {"C4": "b"},
        "bloom": 2,
        "notes_a": "MVP definition is mainstream Lean Startup but may not be in source slides",
    },
    "D07_Q2": {
        "judgment": {},
        "bloom": 2,
    },
    "D07_Q3": {
        "judgment": {},
        "bloom": 1,
    },
    "D07_Q4": {
        # Agile Manifesto: per source check, this content is about Lean Startup section,
        # may not have Agile Manifesto explicitly. Source-grounding borderline.
        "judgment": {"C4": "b"},
        "bloom": 1,
    },
    "D07_Q5": {
        "judgment": {},
        "bloom": 1,
    },

    # ===== D08 - Marketing (VI) =====
    "D08_Q1": {
        "judgment": {},
        "bloom": 3,  # Apply - identify the orientation
    },
    "D08_Q2": {
        # TF mixing Needs/Wants definitions - good distinction question
        "judgment": {},
        "bloom": 4,
    },
    "D08_Q3": {
        # Multiple acceptable answers - tests memorization of specific list
        "judgment": {"C2": "b"},  # many possible 3-of-7 combinations
        "bloom": 1,
        "notes_a": "Question allows multiple equally-correct combinations of 3 of 7 items",
    },
    "D08_Q4": {
        "judgment": {"ID-1": 1},  # empty prompt (D3 finding)
        "bloom": 1,
        "notes_a": "Empty prompt; sentenceHtml only",
    },
    "D08_Q5": {
        # MATCHING swaps definitions - the proposed pairs in options are INTENTIONALLY wrong
        # so students must spot the swap. Same as D03_Q1.
        "judgment": {"C2": "b"},
        "bloom": 2,
        "notes_a": "Pairs in options are intentionally swapped - may confuse students",
    },

    # ===== D09 - Physics (VI) =====
    "D09_Q1": {
        "judgment": {},
        "bloom": 3,  # Apply Gauss law
    },
    "D09_Q2": {
        "judgment": {},  # TF on field line closure - good
        "bloom": 2,
    },
    "D09_Q3": {
        "judgment": {},  # has prompt + sentence
        "bloom": 1,
    },
    "D09_Q4": {
        "judgment": {},
        "bloom": 2,
    },
    "D09_Q5": {
        "judgment": {},
        "bloom": 2,
    },

    # ===== D10 - Microeconomics (EN) =====
    "D10_Q1": {
        "judgment": {},
        "bloom": 4,  # Analyze - chain of inference
    },
    "D10_Q2": {
        "judgment": {},  # TF on conceptual misconception
        "bloom": 4,
    },
    "D10_Q3": {
        "judgment": {},
        "bloom": 1,
    },
    "D10_Q4": {
        "judgment": {},
        "bloom": 1,
    },
    "D10_Q5": {
        "judgment": {},
        "bloom": 2,
    },
}


# ---------------------------------------------------------------------
# Programmatic flag detection (with threshold differences)
# ---------------------------------------------------------------------

def word_count(s: str) -> int:
    return len(re.findall(r"\S+", s or ""))


def parse_options(options_text: str) -> list[str]:
    """Parse 'A) opt1 | B) opt2 | ...' or '[matching pairs: ...]' into list."""
    if not options_text:
        return []
    if "[matching pairs:" in options_text:
        # MATCHING - return the right-hand sides
        m = re.search(r"\[matching pairs:\s*(.*?)\]", options_text)
        if not m:
            return []
        return [p.split("->")[-1].strip() for p in m.group(1).split(";") if "->" in p]
    parts = re.split(r"\s*\|\s*", options_text)
    cleaned = []
    for p in parts:
        # Strip "A) " "B) " prefix
        m = re.match(r"^[A-Z]\)\s*(.*)", p.strip())
        cleaned.append(m.group(1).strip() if m else p.strip())
    return [c for c in cleaned if c]


def detect_length_cue(correct: str, distractors: list[str], strict: bool) -> int:
    if not distractors or not correct:
        return 0
    wc = word_count(correct)
    if wc < 2:
        return 0
    dist_wcs = [word_count(d) for d in distractors if word_count(d) > 0]
    if not dist_wcs:
        return 0
    mean_d = sum(dist_wcs) / len(dist_wcs)
    if mean_d == 0:
        return 0
    ratio = wc / mean_d
    threshold = 1.5 if strict else 2.0
    return 1 if ratio >= threshold else 0


def detect_word_repeat(stem: str, correct: str, distractors: list[str],
                       strict: bool) -> int:
    """Flag when a content word in stem appears in correct but not in any distractor."""
    if not correct or not stem:
        return 0
    # Tokenize, keep content words ≥ 5 chars (drop stopwords by length proxy)
    stem_words = set(w.lower() for w in re.findall(r"[\w]+", stem) if len(w) >= 5)
    correct_words = set(w.lower() for w in re.findall(r"[\w]+", correct) if len(w) >= 5)
    distractor_text = " ".join(distractors).lower()
    shared = stem_words & correct_words
    # Repeat counts only if not also in any distractor
    repeat_unique_to_correct = [w for w in shared
                                if w not in re.findall(r"[\w]+", distractor_text)]
    if strict:
        return 1 if len(repeat_unique_to_correct) >= 1 else 0
    # Lenient: only flag if 2+ distinct content words repeat
    return 1 if len(repeat_unique_to_correct) >= 2 else 0


def detect_absolute_terms(distractors: list[str], strict: bool) -> int:
    text = " ".join(distractors).lower()
    hits = sum(1 for pat in ABSOLUTE_TERMS if re.search(pat, text))
    if strict:
        return 1 if hits >= 1 else 0
    return 1 if hits >= 2 else 0


def detect_vague_terms(correct: str, strict: bool) -> int:
    if not correct:
        return 0
    text = correct.lower()
    hits = sum(1 for pat in VAGUE_TERMS if re.search(pat, text))
    if strict:
        return 1 if hits >= 1 else 0
    return 1 if hits >= 2 else 0


def detect_aota(options_text: str) -> int:
    t = (options_text or "").lower()
    return 1 if any(re.search(p, t) for p in AOTA_PATTERNS) else 0


def detect_negative_unmarked(stem: str, strict: bool) -> int:
    if not stem:
        return 0
    # English NOT/EXCEPT must be ALL CAPS (already emphasized) to be considered marked
    en_neg = re.search(r"\bnot\b", stem, re.IGNORECASE)
    if en_neg and not re.search(r"\bNOT\b", stem):
        # Has lowercase "not" — potentially unmarked
        # Strict: flag; Lenient: only flag if "not" is the main negation
        if strict:
            return 1
        # Lenient: check if there are multiple "not"s or pivotal use
        if stem.lower().count(" not ") >= 1 and "?" in stem:
            return 1
        return 0
    if re.search(r"\bexcept\b", stem) and not re.search(r"\bEXCEPT\b", stem):
        return 1 if strict else 0
    # Vietnamese KHÔNG - same idea
    if "không" in stem and "KHÔNG" not in stem:
        # Be more conservative for Vietnamese (KHÔNG is very common as auxiliary)
        if strict and re.search(r"\bnào (?:không|chưa)\b|không phải|cái nào không", stem, re.IGNORECASE):
            return 1
    return 0


def detect_heterogeneous_options(options: list[str], strict: bool) -> int:
    if len(options) < 3:
        return 0
    wcs = [word_count(o) for o in options if o]
    if not wcs or min(wcs) == 0:
        return 0
    ratio = max(wcs) / min(wcs)
    threshold = 2.0 if strict else 3.0
    return 1 if ratio >= threshold else 0


# ---------------------------------------------------------------------
# Score one question
# ---------------------------------------------------------------------

def score_question(row: dict, perspective: str) -> dict:
    """Return a dict of flag_name -> 0/1, bloom, notes."""
    strict = (perspective == "strict")
    qtype = (row.get("question_type") or "").upper()
    stem = row.get("prompt_text", "")
    options_text = row.get("options_text", "")
    correct = row.get("correct_answer_text", "")

    options = parse_options(options_text)
    # For non-MCQ types (TF, SHORT_ANSWER, FILL_IN_THE_BLANK), options is empty
    distractors = [o for o in options if o.strip() != correct.strip()] if options else []

    # Start with all 0 (IWF flaws) / 1 (pedagogical)
    scores = {f: 0 for f in IWF_FLAGS}
    for f in PED_FLAGS:
        scores[f] = 1

    # ---- Objective flag detection (only applies to types with options) ----
    if options and qtype in ("MCQ_SINGLE", "MCQ_MULTIPLE"):
        scores["TW-1_length_cue"] = detect_length_cue(correct, distractors, strict)
        scores["TW-3_word_repeat"] = detect_word_repeat(stem, correct, distractors, strict)
        scores["TW-5_absolute_terms"] = detect_absolute_terms(distractors, strict)
        scores["TW-6_vague_terms"] = detect_vague_terms(correct, strict)
        scores["ID-2_all_of_the_above"] = detect_aota(options_text)
        scores["ID-3_negative_unmarked"] = detect_negative_unmarked(stem, strict)
        scores["ID-4_heterogeneous_options"] = detect_heterogeneous_options(options, strict)
    else:
        # For TF / SHORT / FILL / MATCHING, still check stem negative marking
        scores["ID-3_negative_unmarked"] = detect_negative_unmarked(stem, strict)

    # Window dressing: stem length heuristic. Technical questions legitimately
    # need context, so thresholds are generous; flag only when the stem clearly
    # over-specifies.
    stem_wc = word_count(stem)
    if stem_wc > (40 if strict else 70):
        scores["ID-6_window_dressing"] = 1

    # ---- Apply hand-coded judgments from ASSESSMENTS ----
    qid = row["qid"]
    a = ASSESSMENTS.get(qid, {"judgment": {}, "bloom": 0})
    for flag_short, val in a["judgment"].items():
        # Map short name (e.g. "TW-1") to full column name
        full = next((f for f in IWF_FLAGS + PED_FLAGS if f.startswith(flag_short + "_")), None)
        if not full:
            print(f"WARN: unknown flag {flag_short} in qid {qid}", file=sys.stderr)
            continue
        if val == 0 or val == 1:
            scores[full] = val
        elif val == "b":  # borderline
            if full in IWF_FLAGS:
                scores[full] = 1 if strict else 0
            else:  # pedagogical
                scores[full] = 0 if strict else 1

    scores["bloom_level"] = a.get("bloom", "")
    if strict:
        scores["notes"] = a.get("notes_a", "")
    else:
        scores["notes"] = a.get("notes_b", a.get("notes_a", ""))
    return scores


# ---------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------

def write_csv(template: Path, target: Path, perspective: str) -> tuple[int, dict]:
    with template.open(encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        rows = list(reader)

    flag_totals = {f: 0 for f in IWF_FLAGS + PED_FLAGS}
    out_rows = []
    for row in rows:
        scores = score_question(row, perspective)
        for f in IWF_FLAGS + PED_FLAGS:
            row[f] = scores[f]
            if scores[f] == 1:
                flag_totals[f] += 1
        row["bloom_level"] = scores["bloom_level"]
        row["notes"] = scores["notes"]
        out_rows.append(row)

    with target.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, quoting=csv.QUOTE_MINIMAL)
        writer.writeheader()
        writer.writerows(out_rows)
    return len(out_rows), flag_totals


def main() -> int:
    template_a = SCORING_DIR / "iwf_reviewerA.csv"
    template_b = SCORING_DIR / "iwf_reviewerB.csv"
    if not template_a.exists() or not template_b.exists():
        print("ERROR: template files missing - run generate_scoring_templates.py first")
        return 1

    # Reviewer A = Strict perspective
    n_a, totals_a = write_csv(template_a, template_a, "strict")
    print(f"Reviewer A (Strict)  → {n_a} rows filled into {template_a.name}")

    # Reviewer B = Lenient perspective
    n_b, totals_b = write_csv(template_b, template_b, "lenient")
    print(f"Reviewer B (Lenient) → {n_b} rows filled into {template_b.name}")

    # Brief summary
    print("\nFlag totals (A=strict / B=lenient):")
    for f in IWF_FLAGS + PED_FLAGS:
        a, b = totals_a[f], totals_b[f]
        diff = "" if a == b else f"  Δ={a-b}"
        print(f"  {f:32s}  A={a:>2d}  B={b:>2d}{diff}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
