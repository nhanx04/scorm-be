window.ScormRenderer = {
  byType: {}
};

window.ScormRenderer.register = function (questionType, renderer) {
  this.byType[questionType] = renderer;
};

window.ScormRenderer.renderQuestion = function (question, ctx) {
  const renderer = this.byType[question.questionType];
  if (!renderer) return `<div class="scorm-question">Unsupported question type: ${question.questionType}</div>`;
  return renderer(question, ctx);
};

window.escapeHtml = function (value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
};

