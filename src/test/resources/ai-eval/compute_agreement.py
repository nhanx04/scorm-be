#!/usr/bin/env python3
"""
After both reviewers fill the scoring/ CSVs, run this to compute:

1. Per-flaw Cohen's κ (inter-rater agreement) for the 12 IWF flaws
2. Per-criterion Cohen's κ for the 4 pedagogical criteria
3. Disagreement targets - list of (qid, flag) pairs where A and B differ
   (use these as the agenda for the consensus meeting)
4. Aggregate IWF metrics for thesis: flaw_free_rate, severely_flawed_rate,
   mean flaws/item, distribution of each flaw (using consensus-as-mean for
   now, will be replaced by true consensus after the meeting)
5. Faithfulness human ↔ LLM-judge agreement (Pearson + Spearman + Kappa
   on hallucination)

All outputs go to stdout for easy copy into the thesis. Disagreements
also go to scoring/consensus_targets.csv so reviewers can work through
them row by row.

Usage:
    python3 compute_agreement.py
"""

from __future__ import annotations

import csv
import statistics
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).parent
SCORING_DIR = ROOT / "scoring"
RESULTS_DIR = ROOT / "results"

IWF_FLAGS = [
    "TW-1_length_cue", "TW-2_grammatical_cue", "TW-3_word_repeat",
    "TW-4_logical_clue", "TW-5_absolute_terms", "TW-6_vague_terms",
    "ID-1_unclear_stem", "ID-2_all_of_the_above", "ID-3_negative_unmarked",
    "ID-4_heterogeneous_options", "ID-5_implausible_distractor",
    "ID-6_window_dressing",
]
PED_FLAGS = ["C1_clarity", "C2_single_correct",
             "C3_plausible_distractors", "C4_source_grounded"]


# ---------------------------------------------------------------------
# Stats helpers
# ---------------------------------------------------------------------

def cohens_kappa(a: list[int], b: list[int]) -> float:
    """Cohen's kappa for two binary raters."""
    if len(a) != len(b) or not a:
        return float("nan")
    n = len(a)
    po = sum(1 for x, y in zip(a, b) if x == y) / n
    pa1 = sum(a) / n
    pb1 = sum(b) / n
    pe = pa1 * pb1 + (1 - pa1) * (1 - pb1)
    if pe == 1:
        return 1.0 if po == 1 else 0.0
    return (po - pe) / (1 - pe)


def kappa_interp(k: float) -> str:
    if k != k:  # NaN
        return "n/a"
    if k < 0:
        return "worse than chance"
    if k < 0.20:
        return "slight"
    if k < 0.40:
        return "fair"
    if k < 0.60:
        return "moderate"
    if k < 0.80:
        return "substantial"
    return "almost perfect"


def pabak(a: list[int], b: list[int]) -> float:
    """Prevalence-Adjusted Bias-Adjusted Kappa.

    Fixes Cohen's kappa "paradox": when the base rate is extreme (e.g. a
    flag fires in 1/50 questions), κ collapses to 0 even when raw agreement
    is 98%. PABAK = 2·Po − 1 ignores prevalence and reports the linear
    rescaling of observed agreement to the [-1, 1] kappa scale.

    Reported alongside κ so the reader can see both.
    """
    if not a:
        return float("nan")
    po = sum(1 for x, y in zip(a, b) if x == y) / len(a)
    return 2 * po - 1


def pearson(a: list[float], b: list[float]) -> float:
    if len(a) != len(b) or len(a) < 2:
        return float("nan")
    ma, mb = statistics.mean(a), statistics.mean(b)
    num = sum((x - ma) * (y - mb) for x, y in zip(a, b))
    da = sum((x - ma) ** 2 for x in a)
    db = sum((y - mb) ** 2 for y in b)
    if da == 0 or db == 0:
        return float("nan")
    return num / (da ** 0.5 * db ** 0.5)


def spearman(a: list[float], b: list[float]) -> float:
    """Spearman rank correlation."""
    def ranks(xs):
        s = sorted(range(len(xs)), key=lambda i: xs[i])
        r = [0.0] * len(xs)
        i = 0
        while i < len(xs):
            j = i
            while j + 1 < len(xs) and xs[s[j + 1]] == xs[s[i]]:
                j += 1
            avg = (i + j) / 2 + 1
            for k in range(i, j + 1):
                r[s[k]] = avg
            i = j + 1
        return r
    return pearson(ranks(a), ranks(b))


# ---------------------------------------------------------------------
# Loaders
# ---------------------------------------------------------------------

def load_csv(path: Path) -> list[dict]:
    if not path.exists():
        return []
    with path.open(encoding="utf-8") as f:
        return list(csv.DictReader(f))


def parse_bin(s: str) -> int | None:
    if s is None:
        return None
    s = s.strip()
    if s == "":
        return None
    if s in ("0", "1"):
        return int(s)
    raise ValueError(f"Expected 0 or 1, got: {s!r}")


def parse_int(s: str) -> int | None:
    if s is None or s.strip() == "":
        return None
    return int(s.strip())


# ---------------------------------------------------------------------
# IWF agreement
# ---------------------------------------------------------------------

def iwf_agreement() -> None:
    a = load_csv(SCORING_DIR / "iwf_reviewerA.csv")
    b = load_csv(SCORING_DIR / "iwf_reviewerB.csv")
    if not a or not b:
        print("IWF: missing reviewer CSV(s) - skipping")
        return
    if len(a) != len(b):
        print(f"WARNING: row count mismatch: A={len(a)}, B={len(b)}")

    # Index by qid for safety
    by_qid_b = {r["qid"]: r for r in b}

    print("\n" + "=" * 100)
    print("BẢNG 5.x.8 — Cohen's κ giữa 2 reviewer trên 12 IWF + 4 pedagogical")
    print("=" * 100)
    fmt = "{:<32} {:>4} {:>5} {:>5} {:>7} {:>8} {:>8} {:<18}"
    print(fmt.format("Flag", "N", "A=1", "B=1", "Po", "κ", "PABAK", "κ Interp"))
    print("-" * 100)

    consensus_targets: list[tuple[str, str, int, int]] = []
    kappas: list[float] = []
    pabaks: list[float] = []

    for col in IWF_FLAGS + PED_FLAGS:
        avals, bvals = [], []
        for row_a in a:
            row_b = by_qid_b.get(row_a["qid"])
            if not row_b:
                continue
            try:
                va = parse_bin(row_a.get(col, ""))
                vb = parse_bin(row_b.get(col, ""))
            except ValueError as e:
                print(f"  parse error qid={row_a['qid']} {col}: {e}")
                continue
            if va is None or vb is None:
                continue
            avals.append(va)
            bvals.append(vb)
            if va != vb:
                consensus_targets.append((row_a["qid"], col, va, vb))

        if not avals:
            print(fmt.format(col, 0, "-", "-", "-", "-", "-", "no data"))
            continue
        k = cohens_kappa(avals, bvals)
        p = pabak(avals, bvals)
        po = sum(1 for x, y in zip(avals, bvals) if x == y) / len(avals)
        kappas.append(k)
        pabaks.append(p)
        print(fmt.format(col, len(avals), sum(avals), sum(bvals),
                         f"{po:.2f}", f"{k:.3f}", f"{p:.3f}",
                         kappa_interp(k)))

    if kappas:
        mean_k = statistics.mean(kappas)
        mean_pabak = statistics.mean(pabaks)
        print("-" * 100)
        print(f"Mean κ across all flags:     {mean_k:.3f} ({kappa_interp(mean_k)})")
        print(f"Mean PABAK across all flags: {mean_pabak:.3f} ({kappa_interp(mean_pabak)})")
        target_met = sum(1 for k in kappas if k >= 0.60)
        target_met_pabak = sum(1 for p in pabaks if p >= 0.60)
        print(f"Flags meeting κ ≥ 0.60:     {target_met}/{len(kappas)}")
        print(f"Flags meeting PABAK ≥ 0.60: {target_met_pabak}/{len(pabaks)}")
        print("\nNote: Cohen's κ collapses to 0 when base rate is extreme (kappa paradox).")
        print("PABAK = 2·Po − 1 corrects for this — use it for low-prevalence flags.")

    if consensus_targets:
        out = SCORING_DIR / "consensus_targets.csv"
        # Preserve existing consensus values across reruns — only overwrite
        # the (qid, flag) → consensus mapping for new disagreements.
        existing: dict[tuple[str, str], str] = {}
        if out.exists():
            for r in load_csv(out):
                existing[(r["qid"], r["flag"])] = r.get("consensus", "")
        with out.open("w", encoding="utf-8", newline="") as f:
            w = csv.writer(f, quoting=csv.QUOTE_MINIMAL)
            w.writerow(["qid", "flag", "reviewer_A", "reviewer_B", "consensus"])
            for qid, col, va, vb in consensus_targets:
                prev = existing.get((qid, col), "")
                w.writerow([qid, col, va, vb, prev])
        resolved = sum(1 for v in existing.values() if v in ("0", "1"))
        print(f"\n{len(consensus_targets)} disagreements → {out.relative_to(ROOT)} "
              f"(resolved: {resolved}/{len(consensus_targets)})")


def iwf_aggregate() -> None:
    """
    Aggregate metrics using *consensus* if available, falling back to mean
    of the two reviewers for items not yet reconciled.
    """
    a = load_csv(SCORING_DIR / "iwf_reviewerA.csv")
    b = load_csv(SCORING_DIR / "iwf_reviewerB.csv")
    if not a or not b:
        return

    consensus_path = SCORING_DIR / "consensus_targets.csv"
    consensus: dict[tuple[str, str], int] = {}
    if consensus_path.exists():
        for r in load_csv(consensus_path):
            v = parse_bin(r.get("consensus", ""))
            if v is not None:
                consensus[(r["qid"], r["flag"])] = v

    by_qid_b = {r["qid"]: r for r in b}

    def resolved(qid: str, col: str, row_a: dict, row_b: dict) -> int | None:
        if (qid, col) in consensus:
            return consensus[(qid, col)]
        try:
            va = parse_bin(row_a.get(col, ""))
            vb = parse_bin(row_b.get(col, ""))
        except ValueError:
            return None
        if va is None or vb is None:
            return None
        if va == vb:
            return va
        return None  # unresolved disagreement, exclude

    flaw_counts: list[int] = []
    flaw_freq: Counter[str] = Counter()
    n_questions = 0

    for row_a in a:
        row_b = by_qid_b.get(row_a["qid"])
        if not row_b:
            continue
        flaw_count_for_q = 0
        any_data = False
        for col in IWF_FLAGS:
            v = resolved(row_a["qid"], col, row_a, row_b)
            if v is None:
                continue
            any_data = True
            if v == 1:
                flaw_count_for_q += 1
                flaw_freq[col] += 1
        if any_data:
            flaw_counts.append(flaw_count_for_q)
            n_questions += 1

    if not flaw_counts:
        print("\nIWF aggregate: no resolved data yet")
        return

    print("\n" + "=" * 92)
    print("BẢNG 5.x.6 — IWF aggregate (consensus where available, else exclude disagreements)")
    print("=" * 92)
    flaw_free = sum(1 for c in flaw_counts if c == 0)
    one_flaw = sum(1 for c in flaw_counts if c == 1)
    severe = sum(1 for c in flaw_counts if c >= 2)
    mean_flaws = statistics.mean(flaw_counts)
    print(f"  N (resolved questions):              {n_questions}")
    print(f"  Flaw-free rate (0 flaws):            {flaw_free}/{n_questions} "
          f"({100*flaw_free/n_questions:.1f}%)")
    print(f"  Exactly 1 flaw:                      {one_flaw}/{n_questions} "
          f"({100*one_flaw/n_questions:.1f}%)")
    print(f"  Severely flawed (≥ 2 flaws):         {severe}/{n_questions} "
          f"({100*severe/n_questions:.1f}%)")
    print(f"  Mean flaws/item:                     {mean_flaws:.2f}")

    print("\n" + "=" * 92)
    print("BẢNG 5.x.7 — Tần suất 12 IWF flaws")
    print("=" * 92)
    fmt = "{:<32} {:>10} {:>10}"
    print(fmt.format("Flag", "Count", "% of N"))
    print("-" * 92)
    for col in IWF_FLAGS:
        n = flaw_freq.get(col, 0)
        print(fmt.format(col, n, f"{100*n/n_questions:.1f}%"))


# ---------------------------------------------------------------------
# Faithfulness human ↔ LLM-judge agreement
# ---------------------------------------------------------------------

def faithfulness_agreement() -> None:
    a = load_csv(SCORING_DIR / "faithfulness_human_reviewerA.csv")
    b = load_csv(SCORING_DIR / "faithfulness_human_reviewerB.csv")
    if not a:
        print("\nFaithfulness: missing reviewer A CSV - skipping")
        return

    judge_csvs = sorted(RESULTS_DIR.glob("faithfulness_auto_*.csv"))
    if not judge_csvs:
        print("\nFaithfulness: no LLM-judge CSV found - skipping")
        return
    judge = load_csv(judge_csvs[-1])
    judge_by_id = {f"{r['doc_id']}_{r['feature']}_run{r['run']}": r for r in judge}

    by_sid_b = {r["sample_id"]: r for r in b} if b else {}

    pairs_ab: list[tuple[float, float]] = []
    hall_ab: list[tuple[int, int]] = []
    pairs_a_judge: list[tuple[float, float]] = []
    hall_a_judge: list[tuple[int, int]] = []
    pairs_b_judge: list[tuple[float, float]] = []

    for row_a in a:
        sid = row_a["sample_id"]
        sa = parse_int(row_a.get("human_faithfulness_score", ""))
        hb_a = parse_bin(_normalize_yesno(row_a.get("human_hallucination", "")))
        if sa is None:
            continue

        # vs reviewer B
        row_b = by_sid_b.get(sid)
        if row_b:
            sb = parse_int(row_b.get("human_faithfulness_score", ""))
            hb_b = parse_bin(_normalize_yesno(row_b.get("human_hallucination", "")))
            if sb is not None:
                pairs_ab.append((sa, sb))
            if hb_a is not None and hb_b is not None:
                hall_ab.append((hb_a, hb_b))

        # vs LLM judge
        rj = judge_by_id.get(sid)
        if rj:
            try:
                sj = int(rj["faithfulness_score"])
                hj = 1 if rj["hallucination"] == "true" else 0
            except Exception:
                continue
            if sj > 0:
                pairs_a_judge.append((sa, sj))
            if hb_a is not None:
                hall_a_judge.append((hb_a, hj))
            if row_b:
                sb = parse_int(row_b.get("human_faithfulness_score", ""))
                if sb is not None and sb > 0:
                    pairs_b_judge.append((sb, sj))

    print("\n" + "=" * 92)
    print("BẢNG 5.x.4.F — Faithfulness inter-rater + LLM-judge agreement")
    print("=" * 92)
    if pairs_ab:
        a_s = [x[0] for x in pairs_ab]
        b_s = [x[1] for x in pairs_ab]
        print(f"  Reviewer A vs B (faithfulness score, N={len(pairs_ab)}):")
        print(f"    Pearson  r = {pearson(a_s, b_s):.3f}")
        print(f"    Spearman ρ = {spearman(a_s, b_s):.3f}")
        print(f"    Mean A = {statistics.mean(a_s):.2f}, Mean B = {statistics.mean(b_s):.2f}")
    if hall_ab:
        a_h = [x[0] for x in hall_ab]
        b_h = [x[1] for x in hall_ab]
        k = cohens_kappa(a_h, b_h)
        print(f"  Reviewer A vs B (hallucination yes/no, N={len(hall_ab)}):")
        print(f"    Cohen's κ = {k:.3f} ({kappa_interp(k)})")

    if pairs_a_judge:
        a_s = [x[0] for x in pairs_a_judge]
        j_s = [x[1] for x in pairs_a_judge]
        print(f"\n  Reviewer A vs LLM-judge (faithfulness score, N={len(pairs_a_judge)}):")
        print(f"    Pearson  r = {pearson(a_s, j_s):.3f}")
        print(f"    Spearman ρ = {spearman(a_s, j_s):.3f}")
        print(f"    Mean A = {statistics.mean(a_s):.2f}, Mean judge = {statistics.mean(j_s):.2f}")
    if hall_a_judge:
        a_h = [x[0] for x in hall_a_judge]
        j_h = [x[1] for x in hall_a_judge]
        k = cohens_kappa(a_h, j_h)
        print(f"  Reviewer A vs LLM-judge (hallucination yes/no, N={len(hall_a_judge)}):")
        print(f"    Cohen's κ = {k:.3f} ({kappa_interp(k)})")

    if pairs_b_judge:
        b_s = [x[0] for x in pairs_b_judge]
        j_s = [x[1] for x in pairs_b_judge]
        print(f"\n  Reviewer B vs LLM-judge (faithfulness score, N={len(pairs_b_judge)}):")
        print(f"    Pearson  r = {pearson(b_s, j_s):.3f}")
        print(f"    Spearman ρ = {spearman(b_s, j_s):.3f}")


def _normalize_yesno(s: str) -> str:
    if s is None:
        return ""
    s = s.strip().lower()
    if s in ("y", "yes", "true", "1"):
        return "1"
    if s in ("n", "no", "false", "0"):
        return "0"
    return s


# ---------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------

def main() -> int:
    if not SCORING_DIR.exists():
        print(f"ERROR: {SCORING_DIR} doesn't exist. "
              f"Run generate_scoring_templates.py first.")
        return 1

    print(f"Scoring dir: {SCORING_DIR}")
    iwf_agreement()
    iwf_aggregate()
    faithfulness_agreement()

    print("\n" + "=" * 92)
    print("DONE. Next:")
    print("  - If kappa is low for some flag, calibrate (discuss operational")
    print("    definition) before doing the consensus meeting.")
    print("  - Fill scoring/consensus_targets.csv to resolve disagreements.")
    print("  - Re-run this script to get final aggregate numbers.")
    print("=" * 92)
    return 0


if __name__ == "__main__":
    sys.exit(main())
