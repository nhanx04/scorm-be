function renderMatching(question, ctx) {
  const { state, index, total } = ctx;
  const value = state.answers[question.id] || {};
  const checked = !!state.checked[question.id];
  const correct = state.correctness[question.id];
  const rows = (question.pairs || []).map((pair) => {
    const current = value[pair.id] || '';
    return `<div class="scorm-match-row"><span>${window.escapeHtml(pair.left || '')}</span>
      <input type="text" value="${window.escapeHtml(current)}" oninput="(function(){ const next = Object.assign({}, window.ScormState.answers['${question.id}'] || {}); next['${pair.id}'] = this.value; window.ScormApp.setAnswer('${question.id}', next); }.bind(this))()"/></div>`;
  }).join('');
  return `<section class="scorm-question"><div class="scorm-question-header" style="background:linear-gradient(to right,rgba(236,72,153,.9),rgba(244,114,182,.8));"><div class="scorm-question-meta"><div class="scorm-pill">Question ${typeof index==='number'?index+1:''} <span class="scorm-badge" style="background:#fce7f3;border-color:#f9a8d4;color:#be185d;">MATCHING</span></div><span>${typeof index==='number'&&typeof total==='number'?`${index+1}/${total}`:''}</span></div></div>
    <h4 class="scorm-question-title">${question.promptHtml || window.escapeHtml(question.prompt || 'Question')}</h4>
    <div class="quiz-stack">${rows}</div>
    ${checked ? `<p class="scorm-feedback ${correct ? 'correct' : 'wrong'}">${correct ? 'Correct' : 'Incorrect'}</p>` : ''}</section>`;
}

window.ScormRenderer.register('GROUPING', renderMatching);
window.ScormRenderer.register('MATCHING', renderMatching);

