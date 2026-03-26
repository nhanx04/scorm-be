window.ScormState = {
  course: null,
  editorState: null,
  theme: null,
  orderedPages: [],
  pageToSection: {},
  cursor: 0,
  answers: {},
  checked: {},
  scormInitialized: false
};

window.ScormState.getCurrentPage = function () {
  const id = this.orderedPages[this.cursor];
  if (!id) return null;
  const sections = this.editorState?.sections || [];
  for (const section of sections) {
    for (const page of section.pages || []) {
      if (page.id === id) return page;
    }
  }
  return null;
};

