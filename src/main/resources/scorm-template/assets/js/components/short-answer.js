window.ScormRenderer.register('SHORT_ANSWER', function (question, ctx) {
  const { state, index, total } = ctx;
  const value = state.answers[question.id] || '';
  const checked = !!state.checked[question.id];
  const correct = state.correctness[question.id];
  return `<section class="scorm-question"><div class="scorm-question-header" style="background:linear-gradient(to right,rgba(249,115,22,.9),rgba(251,146,60,.8));"><div class="scorm-question-meta"><div class="scorm-pill">Question ${typeof index==='number'?index+1:''} <span class="scorm-badge" style="background:#ffedd5;border-color:#fdba74;color:#c2410c;">SHORT ANSWER</span></div><span>${typeof index==='number'&&typeof total==='number'?`${index+1}/${total}`:''}</span></div></div>
    <h4 class="scorm-question-title">${question.promptHtml || window.escapeHtml(question.prompt || 'Question')}</h4>
    <input class="scorm-answer" placeholder="Your answer" type="text" value="${window.escapeHtml(value)}" oninput="window.ScormApp.setAnswer('${question.id}',this.value);"/>
    ${checked ? `<p class="scorm-feedback ${correct ? 'correct' : 'wrong'}">${correct ? 'Correct' : 'Incorrect'}</p>` : ''}</section>`;
});
