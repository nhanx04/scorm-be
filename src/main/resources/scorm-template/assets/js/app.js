window.ScormApp = {
  async init() {
    const [courseData, editorState, theme] = await Promise.all([
      fetch('data/course.json').then((r) => r.json()),
      fetch('data/editor-state.json').then((r) => r.json()),
      fetch('data/theme.json').then((r) => r.json())
    ]);

    const state = window.ScormState;
    state.course = {
      title: editorState.title || courseData.courseTitle || 'Untitled Course',
      description: editorState.description || '',
      coverImageUrl: editorState.coverImageUrl || ''
    };
    state.editorState = editorState;
    state.theme = theme || {};
    state.sessionStartMs = Date.now();
    state.attemptsUsed = 0;
    state.isFinalized = false;
    state.timeLimitReached = false;

    this.buildNavigation();
    state.scormInitialized = window.ScormApi.init();
    this.restoreProgress();
    this.recomputeCorrectness();
    this.syncLearnerState();
    window.ScormPlayer.render();

    window.addEventListener('beforeunload', () => {
      this.persistProgress();
      if (state.scormInitialized) {
        window.ScormApi.setValue('cmi.exit', 'suspend');
        window.ScormApi.terminate('suspend');
      }
    });
  },

  buildNavigation() {
    const state = window.ScormState;
    state.orderedPages = [];
    state.pageToSection = {};
    (state.editorState.sections || []).forEach((section) => {
      (section.pages || []).forEach((page) => {
        state.orderedPages.push(page.id);
        state.pageToSection[page.id] = section.id;
      });
    });
  },

  getCurrentQuestionIds(page) { return (page.quizPage?.questions || []).map((q) => q.id); },

  setAnswer(questionId, value) {
    const state = window.ScormState;
    if (this.checkDurationLimit()) return;
    state.answers[questionId] = value;
    this.recomputeCorrectness();
    this.persistProgress();
  },

  checkAnswer(question, value) {
    if (question.questionType === 'MCQ_SINGLE') return typeof value === 'string' && (question.options || []).some((o) => o.id === value && o.isCorrect);
    if (question.questionType === 'MCQ_MULTIPLE') {
      const selected = Array.isArray(value) ? value.slice().sort() : [];
      const correct = (question.options || []).filter((o) => o.isCorrect).map((o) => o.id).sort();
      return selected.join('|') === correct.join('|');
    }
    if (question.questionType === 'TRUE_FALSE') return typeof value === 'boolean' && value === question.correctAnswer;
    if (question.questionType === 'SHORT_ANSWER') return (question.acceptableAnswers || []).map((a) => String(a).trim().toLowerCase()).includes(String(value || '').trim().toLowerCase());
    if (question.questionType === 'FILL_IN_THE_BLANK') return (question.answers || []).map((a) => String(a).trim().toLowerCase()).includes(String(value || '').trim().toLowerCase());
    if (question.questionType === 'MATCHING' || question.questionType === 'GROUPING') {
      const val = value && typeof value === 'object' && !Array.isArray(value) ? value : {};
      return (question.pairs || []).every((pair) => String(val[pair.id] || '').trim().toLowerCase() === String(pair.right || '').trim().toLowerCase());
    }
    return false;
  },

  recomputeCorrectness() {
    const state = window.ScormState;
    const correctness = {};
    (state.editorState.sections || []).forEach((section) => (section.pages || []).forEach((page) => {
      if (page.pageType !== 'QUIZ') return;
      (page.quizPage?.questions || []).forEach((q) => { correctness[q.id] = state.checked[q.id] ? this.checkAnswer(q, state.answers[q.id]) : null; });
    }));
    state.correctness = correctness;
    this.syncLearnerState();
  },

  calculateScore() {
    const values = Object.values(window.ScormState.correctness || {}).filter((v) => v !== null && v !== undefined);
    const correctCount = values.filter(Boolean).length;
    const totalChecked = values.length;
    const raw = totalChecked ? Math.round((correctCount / totalChecked) * 100) : 0;
    return { raw, scaled: totalChecked ? Number((raw / 100).toFixed(2)) : 0, totalChecked };
  },

  computeCompletionStatus() {
    const state = window.ScormState;
    if (state.orderedPages.length === 0) return 'not attempted';
    if (state.cursor >= state.orderedPages.length - 1) return 'completed';
    if (state.cursor > 0 || Object.keys(state.answers).length > 0) return 'incomplete';
    return 'not attempted';
  },

  computeSessionSeconds() {
    const state = window.ScormState;
    if (!state.sessionStartMs) return state.accumulatedSessionSeconds || 0;
    return Math.floor((Date.now() - state.sessionStartMs) / 1000) + (state.accumulatedSessionSeconds || 0);
  },

  getCoursePassingScore() {
    const score = Number(window.ScormState.editorState?.passingScore ?? 80);
    if (!Number.isFinite(score)) return 80;
    return Math.min(100, Math.max(0, score));
  },

  getAttemptLimit() {
    const limit = Number(window.ScormState.editorState?.attemptLimit ?? 0);
    if (!Number.isFinite(limit)) return 0;
    return Math.max(0, Math.floor(limit));
  },

  getDurationLimitSeconds() {
    const mins = Number(window.ScormState.editorState?.durationMin ?? 0);
    if (!Number.isFinite(mins)) return 0;
    return Math.max(0, Math.floor(mins)) * 60;
  },

  isAttemptLimitReached() {
    const limit = this.getAttemptLimit();
    return limit > 0 && (window.ScormState.attemptsUsed || 0) >= limit;
  },

  checkDurationLimit() {
    const state = window.ScormState;
    if (state.timeLimitReached) return true;
    const limitSeconds = this.getDurationLimitSeconds();
    if (limitSeconds <= 0) return false;
    const elapsed = this.computeSessionSeconds();
    if (elapsed >= limitSeconds) {
      state.timeLimitReached = true;
      this.finalizeAttempt();
      return true;
    }
    return false;
  },

  finalizeAttempt() {
    const state = window.ScormState;
    state.isFinalized = true;
    this.recomputeCorrectness();
    this.persistProgress();
  },

  syncLearnerState() {
    const state = window.ScormState;
    if (!state.scormInitialized) return;
    const { raw, scaled } = this.calculateScore();
    const hasAnyAttempt = Object.keys(state.checked || {}).length > 0 || Object.keys(state.answers || {}).length > 0;
    const completionStatus = hasAnyAttempt ? 'incomplete' : this.computeCompletionStatus();
    const passScore = this.getCoursePassingScore();
    const successStatus = completionStatus === 'not attempted' ? 'unknown' : raw >= passScore ? 'passed' : 'failed';

    window.ScormApi.setValue('cmi.score.raw', String(raw));
    window.ScormApi.setValue('cmi.score.scaled', scaled.toFixed(2));
    window.ScormApi.setValue('cmi.score.min', '0');
    window.ScormApi.setValue('cmi.score.max', '100');
    window.ScormApi.setValue('cmi.success_status', successStatus);
    window.ScormApi.setValue('cmi.completion_status', completionStatus);
  },

  persistProgress() {
    const state = window.ScormState;
    if (!state.scormInitialized) return;

    const suspendData = JSON.stringify({
      cursor: state.cursor,
      answers: state.answers,
      checked: state.checked,
      attemptsUsed: state.attemptsUsed || 0,
      isFinalized: !!state.isFinalized,
      timeLimitReached: !!state.timeLimitReached,
      accumulatedSessionSeconds: this.computeSessionSeconds()
    });

    const currentPageId = state.orderedPages[state.cursor] || 'start';
    window.ScormApi.setValue('cmi.location', `slide:${currentPageId}`);
    window.ScormApi.setValue('cmi.suspend_data', suspendData);
    window.ScormApi.setValue('cmi.session_time', window.ScormApi.formatSessionTime(this.computeSessionSeconds()));
    this.syncLearnerState();
    window.ScormApi.commit();
  },

  restoreProgress() {
    const state = window.ScormState;
    if (!state.scormInitialized) return;
    const raw = window.ScormApi.getValue('cmi.suspend_data');
    if (!raw) return;

    try {
      const parsed = JSON.parse(raw);
      state.cursor = Number.isFinite(parsed.cursor) ? parsed.cursor : 0;
      state.answers = parsed.answers || {};
      state.checked = parsed.checked || {};
      state.accumulatedSessionSeconds = Number(parsed.accumulatedSessionSeconds || 0);
      state.attemptsUsed = Number(parsed.attemptsUsed || 0);
      state.isFinalized = !!parsed.isFinalized;
      state.timeLimitReached = !!parsed.timeLimitReached;
    } catch (_) {}
  }
};

document.addEventListener('DOMContentLoaded', () => window.ScormApp.init());