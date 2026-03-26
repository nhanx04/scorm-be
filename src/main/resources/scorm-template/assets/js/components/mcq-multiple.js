window.ScormRenderer.register('MCQ_MULTIPLE', function (question, ctx) {
  const { state, index, total } = ctx;
  const selected = Array.isArray(state.answers[question.id]) ? state.answers[question.id] : [];
  const checked = !!state.checked[question.id];
  const correct = state.correctness[question.id];
  const options = (question.options || []).map((o) => {
    const active = selected.includes(o.id);
    return `<label class="scorm-option ${active ? 'active purple' : 'scorm-option-hover purple'}"><span class="scorm-pill">${active ? '▣' : '☐'}</span>
      <input class="sr-only" type="checkbox" ${active ? 'checked' : ''} onchange="(function(){ const current = Array.isArray(window.ScormState.answers['${question.id}']) ? window.ScormState.answers['${question.id}'] : []; const next = current.includes('${o.id}') ? current.filter(v => v !== '${o.id}') : current.concat('${o.id}'); window.ScormApp.setAnswer('${question.id}', next); window.ScormPlayer.render(); })()"/>
      <span>${o.labelHtml || window.escapeHtml(o.text || '')}</span></label>`;
  }).join('');
  return `<section class="scorm-question"><div class="scorm-question-header" style="background:linear-gradient(to right,rgba(168,85,247,.9),rgba(192,132,252,.8));"><div class="scorm-question-meta"><div class="scorm-pill">Question ${typeof index==='number'?index+1:''} <span class="scorm-badge" style="background:#f5f3ff;border-color:#ddd6fe;color:#6d28d9;">MCQ MULTIPLE</span></div><span>${typeof index==='number'&&typeof total==='number'?`${index+1}/${total}`:''}</span></div></div>
    <h4 class="scorm-question-title">${question.promptHtml || window.escapeHtml(question.prompt || 'Question')}</h4>${options}
    ${checked ? `<p class="scorm-feedback ${correct ? 'correct' : 'wrong'}">${correct ? 'Correct' : 'Incorrect'}</p>` : ''}</section>`;
});
