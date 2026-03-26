window.ScormApi = {
  api: null,

  init() {
    this.api = this.findApi(window);
    if (!this.api) {
      console.warn('SCORM API not found. Running in standalone mode.');
      return false;
    }

    if (this.api.Initialize) this.api.Initialize('');
    else if (this.api.LMSInitialize) this.api.LMSInitialize('');

    return true;
  },

  terminate() {
    if (!this.api) return;
    this.commit();
    if (this.api.Terminate) this.api.Terminate('');
    else if (this.api.LMSFinish) this.api.LMSFinish('');
  },

  setValue(key, value) {
    if (!this.api) return;
    if (this.api.SetValue) this.api.SetValue(key, String(value));
    else if (this.api.LMSSetValue) this.api.LMSSetValue(key, String(value));
  },

  getValue(key) {
    if (!this.api) return '';
    if (this.api.GetValue) return this.api.GetValue(key) || '';
    if (this.api.LMSGetValue) return this.api.LMSGetValue(key) || '';
    return '';
  },

  commit() {
    if (!this.api) return;
    if (this.api.Commit) this.api.Commit('');
    else if (this.api.LMSCommit) this.api.LMSCommit('');
  },

  findApi(win) {
    let current = win;
    for (let i = 0; i < 10; i++) {
      if (current.API_1484_11) return current.API_1484_11;
      if (current.API) return current.API;
      if (!current.parent || current.parent === current) break;
      current = current.parent;
    }
    return null;
  }
};

