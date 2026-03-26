window.ScormRenderer.register('TRUE_FALSE', function (question, ctx) {
  const { state, index, total } = ctx;
  const value = state.answers[question.id];
  const checked = !!state.checked[question.id];
  const correct = state.correctness[question.id];
  return `<section class="scorm-question"><div class="scorm-question-header" style="background:linear-gradient(to right,rgba(34,197,94,.9),rgba(74,222,128,.8));"><div class="scorm-question-meta"><div class="scorm-pill">Question ${typeof index==='number'?index+1:''} <span class="scorm-badge" style="background:#dcfce7;border-color:#86efac;color:#15803d;">TRUE FALSE</span></div><span>${typeof index==='number'&&typeof total==='number'?`${index+1}/${total}`:''}</span></div></div>
    <h4 class="scorm-question-title">${question.promptHtml || window.escapeHtml(question.prompt || 'Question')}</h4>
    <div class="scorm-boolean"><label class="scorm-boolean-btn ${value === true ? 'active' : ''}"><input class="sr-only" type="radio" name="q-${question.id}" ${value === true ? 'checked' : ''} onchange="window.ScormApp.setAnswer('${question.id}',true);window.ScormPlayer.render();"/>✔ True</label>
    <label class="scorm-boolean-btn ${value === false ? 'active' : ''}"><input class="sr-only" type="radio" name="q-${question.id}" ${value === false ? 'checked' : ''} onchange="window.ScormApp.setAnswer('${question.id}',false);window.ScormPlayer.render();"/>✖ False</label></div>
    ${checked ? `<p class="scorm-feedback ${correct ? 'correct' : 'wrong'}">${correct ? 'Correct' : 'Incorrect'}</p>` : ''}</section>`;
});
