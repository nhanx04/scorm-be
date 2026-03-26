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
    this.buildNavigation();

    state.scormInitialized = window.ScormApi.init();
    this.restoreProgress();
    this.recomputeCorrectness();
    window.ScormPlayer.render();
    window.addEventListener('beforeunload', () => window.ScormApi.terminate());
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
    window.ScormState.answers[questionId] = value;
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
    this.syncScormScore();
  },

  persistProgress() {
    const state = window.ScormState;
    if (!state.scormInitialized) return;
    const suspendData = JSON.stringify({ cursor: state.cursor, answers: state.answers, checked: state.checked });
    window.ScormApi.setValue('cmi.location', String(state.cursor));
    window.ScormApi.setValue('cmi.suspend_data', suspendData);
    window.ScormApi.setValue('cmi.completion_status', state.cursor >= state.orderedPages.length - 1 ? 'completed' : 'incomplete');
    window.ScormApi.commit();
  },

  restoreProgress() {
    const state = window.ScormState;
    const raw = window.ScormApi.getValue('cmi.suspend_data');
    if (!raw) return;
    try {
      const parsed = JSON.parse(raw);
      state.cursor = Number.isFinite(parsed.cursor) ? parsed.cursor : 0;
      state.answers = parsed.answers || {};
      state.checked = parsed.checked || {};
    } catch (_) {}
  },

  syncScormScore() {
    const state = window.ScormState;
    const values = Object.values(state.correctness || {}).filter((v) => v !== null && v !== undefined);
    const raw = values.length ? Math.round((values.filter(Boolean).length / values.length) * 100) : 0;
    if (!state.scormInitialized) return;
    window.ScormApi.setValue('cmi.score.raw', raw);
    window.ScormApi.setValue('cmi.score.scaled', (raw / 100).toFixed(2));
    window.ScormApi.setValue('cmi.success_status', raw >= Number(state.editorState.passingScore || 80) ? 'passed' : 'failed');
    window.ScormApi.commit();
  }
};

document.addEventListener('DOMContentLoaded', () => window.ScormApp.init());
