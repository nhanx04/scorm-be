# Continuous AI Eval — Operator Guide

When and how to re-run the eval harness after a code change so prompt /
schema regressions are caught early.

> All commands assume:
> - `cd /Users/phutuenguyen/HCMUT/252/ĐATN/code/scorm-be`
> - `set -a; source .env; set +a` (loads `GOOGLE_GENAI_API_KEY`)

---

## When to re-run

| Triggering change                                | Recommended pass        | Cost   |
|--------------------------------------------------|-------------------------|--------|
| Quiz prompt template (`QuizPrompts.FROM_TEXT`)   | quiz-only postfix       | ~$0.20 |
| Outline prompt or page-content prompt            | full save-outputs       | ~$1.00 |
| Model swap (Gemini → another)                    | full evaluation × 2     | ~$5    |
| Temperature / config-only change                 | quiz-only postfix       | ~$0.20 |
| Schema validator change (no AI behavior change)  | unit tests only         | $0     |
| Few-shot example added/removed                   | save-outputs + judge    | ~$1    |

---

## The five pass variants

### Smoke — 1 call, ~$0.05
Sanity-check the harness wires to Gemini and writes a CSV. Use after any
infra/config change. Pinned to the smallest doc (D09).
```bash
mvn test -Dai-eval.enabled=true \
  -Dtest='AiQualityEvaluationTest#smokeTest'
```

### Quiz-only postfix — 10 calls, ~$0.20, ~4 min
Verifies a quiz-prompt change without touching outline/page-content.
Writes outputs to `results/outputs/postfix/` (override the subdir if
needed) and CSV with `postfix_quiz_*` prefix.
```bash
mvn test -Dai-eval.enabled=true \
  -Dtest='AiQualityEvaluationTest#quizOnlyPostFixPass' \
  -Dai-eval.outputs-subdir=outputs/postfix
```

### Save-outputs (all 3 features) — 30 calls, ~$1.00, ~12 min
Persists raw + parsed JSON for every (doc, feature) combo. Required
before re-running the LLM judge or the IWF templates.
```bash
mvn test -Dai-eval.enabled=true \
  -Dtest='AiQualityEvaluationTest#saveOutputsPass'
```

### Full evaluation — 60 calls (10 × 3 × 2 runs), ~$2.10, ~40 min
The two-run pass that produces the latency/cost numbers in D2_RESULTS.
Use only when you need variance data; otherwise save-outputs is enough.
```bash
mvn test -Dai-eval.enabled=true \
  -Dtest='AiQualityEvaluationTest#fullEvaluation'
```

### LLM judge — 30 outputs, ~$0.02, ~1 min
Reads the saved JSONs and scores faithfulness via Gemini 2.5 Flash.
Use after any save-outputs or quiz-only postfix run.
```bash
mvn test -Dai-eval.enabled=true \
  -Dtest='LlmJudgeTest#runJudgeOnSavedOutputs' \
  -Dai-eval.outputs-subdir=outputs/postfix \
  -Dai-eval.csv-suffix=postfix
```

---

## Analysis scripts

### Schema empty-field breakdown
```bash
python3 src/test/resources/ai-eval/analyze_quiz_empty.py results/outputs/postfix
```
- Expected goal: 0% empty fields after the D5+ prompt + validator fixes.
- If any type shows > 5%, investigate the specific JSON file under
  `results/outputs/<subdir>/`.

### Reliability + latency + cost
```bash
python3 src/test/resources/ai-eval/analyze_results.py
```
- Reads the most recent `reliability_*.csv` and `latency_cost_*.csv`.
- Expected goals: schema_ok ≥ 95%, p95 latency < 30 s, $/call < $0.10
  (outline can be higher when full doc is fed).

### Faithfulness LLM-judge
```bash
python3 src/test/resources/ai-eval/analyze_faithfulness.py
```
- Reads the most recent `faithfulness_auto*.csv`.
- Expected goal: outline mean ≥ 4.0, page-content mean ≥ 4.0,
  quiz mean ≥ 3.5 (after D5+ fixes). Bimodal distribution = behavior
  shifted; investigate sample reasoning to interpret.

### Inter-rater agreement (D4 reviewers)
```bash
python3 src/test/resources/ai-eval/compute_agreement.py
```
- Run after both IWF CSVs are filled. Look at PABAK if Cohen's κ
  collapses on low-prevalence flags.

---

## Watch-list metrics

If you run a full eval, copy these into a one-page comparison vs D2/D3
baseline. Anything outside the bracket suggests regression:

| Metric                         | Baseline | Action if outside |
|--------------------------------|----------|-------------------|
| Quiz empty_field rate          | 40%      | > 5% → investigate per-type breakdown |
| Quiz hallucination rate (auto) | 60-80%   | > 80% → review prompt or temperature |
| Outline mean faithfulness      | 4.0      | < 3.5 → outline prompt changed? |
| p95 latency (any feature)      | < 30 s   | > 45 s → API issue or huge input |
| Total cost for full eval       | ~$2.10   | > $5 → check input token count |

Production telemetry counter `ai.quiz.schema_issues{attempt}` is the
cheap-but-real-time view of the same signal — alert if `first` counter
trends up.

---

## Anti-patterns

- **Do NOT** overwrite `results/outputs/` with new data before backing
  up baseline. Use `-Dai-eval.outputs-subdir=outputs/<run-name>/` and
  copy baseline to `outputs/baseline/` first.
- **Do NOT** delete `results/*.csv` — the analysis scripts pick the
  latest by timestamp, so old files coexist fine.
- **Do NOT** change `eval-config.yaml` model version between runs you
  intend to compare. Pin to a snapshot, log the model version actually
  used.
