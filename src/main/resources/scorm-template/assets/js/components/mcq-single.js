window.ScormRenderer.register('MCQ_SINGLE', function (question, ctx) {
  const { state, index, total } = ctx;
  const selected = state.answers[question.id] || '';
  const checked = !!state.checked[question.id];
  const correct = state.correctness[question.id];
  const options = (question.options || []).map((o) => {
    const active = selected === o.id;
    return `<label class="scorm-option ${active ? 'active blue' : 'scorm-option-hover blue'}"><span class="scorm-pill">${active ? '◉' : '○'}</span>
      <input class="sr-only" type="radio" name="q-${question.id}" ${active ? 'checked' : ''} onchange="window.ScormApp.setAnswer('${question.id}','${o.id}');window.ScormPlayer.render();"/>
      <span>${o.labelHtml || window.escapeHtml(o.text || '')}</span></label>`;
  }).join('');
  return `<section class="scorm-question"><div class="scorm-question-header" style="background:linear-gradient(to right,rgba(59,130,246,.9),rgba(96,165,250,.8));"><div class="scorm-question-meta"><div class="scorm-pill">Question ${typeof index==='number'?index+1:''} <span class="scorm-badge">MCQ SINGLE</span></div><span>${typeof index==='number'&&typeof total==='number'?`${index+1}/${total}`:''}</span></div></div>
    <h4 class="scorm-question-title">${question.promptHtml || window.escapeHtml(question.prompt || 'Question')}</h4>${options}
    ${checked ? `<p class="scorm-feedback ${correct ? 'correct' : 'wrong'}">${correct ? 'Correct' : 'Incorrect'}</p>` : ''}</section>`;
});
