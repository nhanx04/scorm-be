window.ScormRenderer.register('FILL_IN_THE_BLANK', function (question, ctx) {
  const { state, index, total } = ctx;
  const value = state.answers[question.id] || '';
  const checked = !!state.checked[question.id];
  const correct = state.correctness[question.id];
  return `<section class="scorm-question"><div class="scorm-question-header" style="background:linear-gradient(to right,rgba(20,184,166,.9),rgba(45,212,191,.8));"><div class="scorm-question-meta"><div class="scorm-pill">Question ${typeof index==='number'?index+1:''} <span class="scorm-badge" style="background:#ccfbf1;border-color:#5eead4;color:#0f766e;">FILL IN THE BLANK</span></div><span>${typeof index==='number'&&typeof total==='number'?`${index+1}/${total}`:''}</span></div></div>
    <h4 class="scorm-question-title">${question.promptHtml || window.escapeHtml(question.prompt || 'Question')}</h4>
    <div class="scorm-answer teal">${question.sentenceHtml || ''}</div>
    <input class="scorm-answer teal" placeholder="Fill blank" type="text" value="${window.escapeHtml(value)}" oninput="window.ScormApp.setAnswer('${question.id}',this.value);"/>
    ${checked ? `<p class="scorm-feedback ${correct ? 'correct' : 'wrong'}">${correct ? 'Correct' : 'Incorrect'}</p>` : ''}</section>`;
});
