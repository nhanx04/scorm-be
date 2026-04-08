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

    const completion = this.getValue('cmi.completion_status');
    if (!completion) this.setValue('cmi.completion_status', 'not attempted');

    const success = this.getValue('cmi.success_status');
    if (!success) this.setValue('cmi.success_status', 'unknown');

    if (!this.getValue('cmi.score.min')) this.setValue('cmi.score.min', '0');
    if (!this.getValue('cmi.score.max')) this.setValue('cmi.score.max', '100');

    this.commit();
    return true;
  },

  terminate(exitMode) {
    if (!this.api) return;
    if (exitMode) this.setValue('cmi.exit', exitMode);
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

  formatSessionTime(totalSeconds) {
    const sec = Math.max(0, Math.floor(totalSeconds));
    const hours = Math.floor(sec / 3600);
    const minutes = Math.floor((sec % 3600) / 60);
    const seconds = sec % 60;
    let out = 'PT';
    if (hours > 0) out += `${hours}H`;
    if (minutes > 0) out += `${minutes}M`;
    if (seconds > 0 || out === 'PT') out += `${seconds}S`;
    return out;
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
