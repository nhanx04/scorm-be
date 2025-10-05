/**
 * SCORM 2004 API Wrapper
 */
class ScormAPI {
    constructor() {
        this.api = null;
        this.initialized = false;
        this.findAPI();
    }

    findAPI() {
        let theAPI = null;
        let findAPITries = 0;
        
        while ((theAPI == null) && (findAPITries < 500)) {
            findAPITries++;
            
            if (window.parent && window.parent != window) {
                theAPI = this.findAPIInWindow(window.parent);
            }
            
            if ((theAPI == null) && (window.opener != null)) {
                theAPI = this.findAPIInWindow(window.opener);
            }
        }
        
        if (theAPI == null) {
            console.log("SCORM API not found");
        } else {
            this.api = theAPI;
            console.log("SCORM API found");
        }
        
        return theAPI;
    }

    findAPIInWindow(win) {
        let theAPI = null;
        
        if (win.API_1484_11) {
            theAPI = win.API_1484_11;
        } else {
            let frames = win.frames;
            for (let i = 0; i < frames.length; i++) {
                try {
                    theAPI = this.findAPIInWindow(frames[i]);
                    if (theAPI != null) {
                        break;
                    }
                } catch (e) {
                    // Cross-domain security issue
                }
            }
        }
        
        return theAPI;
    }

    initialize() {
        if (this.api) {
            let result = this.api.Initialize("");
            this.initialized = (result === "true");
            console.log("SCORM Initialize:", result);
            return this.initialized;
        }
        return false;
    }

    terminate() {
        if (this.api && this.initialized) {
            let result = this.api.Terminate("");
            this.initialized = false;
            console.log("SCORM Terminate:", result);
            return result === "true";
        }
        return false;
    }

    getValue(element) {
        if (this.api && this.initialized) {
            let result = this.api.GetValue(element);
            console.log("SCORM GetValue:", element, "=", result);
            return result;
        }
        return "";
    }

    setValue(element, value) {
        if (this.api && this.initialized) {
            let result = this.api.SetValue(element, value);
            console.log("SCORM SetValue:", element, "=", value, "Result:", result);
            return result === "true";
        }
        return false;
    }

    commit() {
        if (this.api && this.initialized) {
            let result = this.api.Commit("");
            console.log("SCORM Commit:", result);
            return result === "true";
        }
        return false;
    }

    getLastError() {
        if (this.api) {
            return this.api.GetLastError();
        }
        return "0";
    }

    getErrorString(errorCode) {
        if (this.api) {
            return this.api.GetErrorString(errorCode);
        }
        return "";
    }

    getDiagnostic(errorCode) {
        if (this.api) {
            return this.api.GetDiagnostic(errorCode);
        }
        return "";
    }

    // Convenience methods
    setScore(score, maxScore = 100) {
        this.setValue("cmi.score.raw", score.toString());
        this.setValue("cmi.score.max", maxScore.toString());
        this.setValue("cmi.score.scaled", (score / maxScore).toString());
    }

    setCompletionStatus(status) {
        // Status can be: "completed", "incomplete", "not attempted", "unknown"
        this.setValue("cmi.completion_status", status);
    }

    setSuccessStatus(status) {
        // Status can be: "passed", "failed", "unknown"
        this.setValue("cmi.success_status", status);
    }

    setProgress(progress) {
        // Progress should be between 0 and 1
        this.setValue("cmi.progress_measure", progress.toString());
    }

    recordInteraction(id, type, response, result, description = "") {
        const interactionIndex = this.getValue("cmi.interactions._count");
        const base = `cmi.interactions.${interactionIndex}`;
        
        this.setValue(`${base}.id`, id);
        this.setValue(`${base}.type`, type);
        this.setValue(`${base}.learner_response`, response);
        this.setValue(`${base}.result`, result);
        this.setValue(`${base}.description`, description);
        this.setValue(`${base}.timestamp`, new Date().toISOString());
    }
}

// Global SCORM API instance
window.scormAPI = new ScormAPI();
