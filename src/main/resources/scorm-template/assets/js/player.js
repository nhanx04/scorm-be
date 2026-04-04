window.ScormPlayer = {
  render() {
    const state = window.ScormState;
    const root = document.getElementById('scorm-root');
    if (!root || !state.course || !state.editorState) return;
    const page = state.getCurrentPage();
    const totalPages = state.orderedPages.length || 0;
    const theme = state.theme?.global || {};

    root.innerHTML = `
      <main class="scorm-main" style="background:${theme.background || '#fff'};color:${theme.color || '#111827'};border-radius:${theme.borderRadius || 16}px;font-family:${theme.fontFamily || 'Inter, sans-serif'};">
        <div class="scorm-head-row">
          <div><p class="scorm-muted">Learner Preview</p><h2 class="scorm-page-title">${window.escapeHtml(page?.title || 'No page')}</h2></div>
          <span class="scorm-muted">${totalPages === 0 ? 0 : state.cursor + 1}/${totalPages}</span>
        </div>
        ${state.cursor === 0 ? renderIntro(state) : ''}
        <div class="scorm-muted section-count">${state.editorState.sections?.length ? `Sections: ${state.editorState.sections.length}` : 'No sections'}</div>
        ${page ? renderPage(page, state) : ''}
        ${page?.pageType === 'QUIZ' ? renderQuizFooter(state, page) : ''}
        <div class="scorm-nav-row">
          <button class="scorm-nav-btn" data-action="prev" ${state.cursor <= 0 ? 'disabled' : ''}>Previous</button>
          <button class="scorm-nav-btn primary" data-action="next" style="background:${theme.primaryColor || '#2563eb'}" ${state.cursor >= totalPages - 1 ? 'disabled' : ''}>Next</button>
        </div>
      </main>`;

    bindNav(root, state, totalPages, page);
  }
};

function bindNav(root, state, totalPages, page) {
  root.querySelector('[data-action="prev"]')?.addEventListener('click', () => {
    if (window.ScormApp.checkDurationLimit()) {
      window.ScormPlayer.render();
      return;
    }
    state.cursor = Math.max(0, state.cursor - 1); window.ScormPlayer.render(); window.ScormApp.persistProgress();
  });
  root.querySelector('[data-action="next"]')?.addEventListener('click', () => {
    if (window.ScormApp.checkDurationLimit()) {
      window.ScormPlayer.render();
      return;
    }
    state.cursor = Math.min(totalPages - 1, state.cursor + 1); window.ScormPlayer.render(); window.ScormApp.persistProgress();
  });
  root.querySelector('[data-action="check-answers"]')?.addEventListener('click', () => {
    if (window.ScormApp.checkDurationLimit()) {
      window.ScormPlayer.render();
      return;
    }
    if (window.ScormApp.isAttemptLimitReached()) {
      window.ScormPlayer.render();
      return;
    }
    (window.ScormApp.getCurrentQuestionIds(page) || []).forEach((id) => (state.checked[id] = true));
    state.attemptsUsed = Number(state.attemptsUsed || 0) + 1;
    window.ScormApp.finalizeAttempt();
    window.ScormPlayer.render();
  });
}

function renderIntro(state) {
  return `<section class="scorm-intro"><h1>${window.escapeHtml(state.course.title || 'Untitled course')}</h1>
    <div class="scorm-cover">${state.course.coverImageUrl ? `<img src="${state.course.coverImageUrl}" alt="Course cover"/>` : `<div class="scorm-cover-empty">No cover image</div>`}</div>
    <div class="scorm-description"><p>${window.escapeHtml(state.course.description || 'Add learning objective...')}</p><div class="line"></div></div></section>`;
}

function renderPage(page, state) {
  if (page.pageType === 'QUIZ') return `<div class="quiz-stack">${(page.quizPage?.questions || []).map((q, i, arr) => window.ScormRenderer.renderQuestion(q, { state, index: i, total: arr.length })).join('')}</div>`;
  return `<div class="content-stack">${(page.contentPage?.blocks || []).sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0)).map(renderBlock).join('')}</div>`;
}

function renderBlock(block) {
  if (block.type === 'TEXT') return `<div class="prose">${block.textHtml || ''}</div>`;
  if (block.type === 'IMAGE') return `<img class="img-block" src="${block.imageUrl || ''}" alt="${window.escapeHtml(block.caption || 'image')}"/>`;
  if (block.type === 'VIDEO') return `<iframe class="video-block" src="${block.embedUrl || ''}" title="video block" frameborder="0" allowfullscreen></iframe>`;
  return '';
}

function renderQuizFooter(state, page) {
  const ids = window.ScormApp.getCurrentQuestionIds(page); const checkedIds = ids.filter((id) => state.checked[id]);
  const score = checkedIds.filter((id) => state.correctness[id] === true).length;
  const completion = ids.length === 0 ? 0 : Math.round((checkedIds.length / ids.length) * 100);
  const attemptLimit = window.ScormApp.getAttemptLimit();
  const attemptsUsed = Number(state.attemptsUsed || 0);
  const isLockedByAttempts = window.ScormApp.isAttemptLimitReached();
  const isLockedByTime = !!state.timeLimitReached;
  const disabled = isLockedByAttempts || isLockedByTime;

  const status = isLockedByTime
    ? `<p class="scorm-muted">Time limit reached. Attempt auto-submitted.</p>`
    : isLockedByAttempts
      ? `<p class="scorm-muted">Attempt limit reached (${attemptsUsed}/${attemptLimit}).</p>`
      : `<p class="scorm-muted">Attempts: ${attemptLimit > 0 ? `${attemptsUsed}/${attemptLimit}` : `${attemptsUsed}/∞`}</p>`;

  return `<section class="scorm-quiz"><p class="font">Progress: ${completion}%</p><p>Score: ${score}/${ids.length}</p>${status}<button class="scorm-nav-btn" data-action="check-answers" ${disabled ? 'disabled' : ''}>Check Answers</button></section>`;
}
