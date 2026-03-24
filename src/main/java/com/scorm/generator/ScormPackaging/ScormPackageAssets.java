package com.scorm.generator.ScormPackaging;

import org.springframework.stereotype.Component;

@Component
public class ScormPackageAssets {

  public String baseCss() {
    return """
        :root{--bg:#f5f7fb;--text:#0f172a;--card:#ffffff;--border:rgba(15,23,42,.12);--muted:#6b7280;--accent:#2563eb}
        *{box-sizing:border-box}body{margin:0;font-family:Inter,Arial,sans-serif;background:var(--bg);color:var(--text);line-height:1.6}
        h1,h2,h3,h4{margin:0 0 8px 0;font-weight:700}p{margin:0 0 8px 0}
        #app{max-width:1200px;margin:0 auto;padding:24px}
        .course-shell{background:var(--card);border-radius:16px;padding:20px;box-shadow:0 8px 24px rgba(15,23,42,.08)}
        .course-header{margin-bottom:20px;display:flex;flex-direction:column;gap:12px;text-align:center}
        .course-header h1{font-size:32px;letter-spacing:.06em;text-transform:uppercase}
        .course-cover{border-radius:14px;border:1px dashed var(--border);overflow:hidden;background:#f1f5f9}
        .course-cover img{display:block;width:100%;height:240px;object-fit:cover}
        .course-objective{background:#f1f5f9;padding:12px 16px;border-radius:14px;text-align:left;font-size:14px;color:#374151}
        .section{margin-top:16px;padding:16px;border-radius:12px;border:1px solid var(--border);background:var(--card)}
        .page{margin-top:12px;padding:14px;border-radius:10px;border:1px solid var(--border);background:var(--card);position:relative}
        .content-area{display:grid;gap:12px;position:relative}
        .block{padding:12px;border-radius:10px;border:1px solid var(--border);background:#fff;position:relative}
        .quiz-area{display:grid;gap:12px}
        .question{border-radius:16px;border:1px solid var(--border);overflow:hidden;background:#fff}
        .q-head{padding:16px;border-bottom:1px solid var(--border)}
        .q-title{font-size:20px;font-weight:700;margin:0 0 8px 0}
        .q-body{padding:16px}
        .q-feedback{margin-top:12px;padding:10px 12px;border-radius:10px;font-size:13px;font-weight:600}
        .q-feedback.correct{background:#ecfdf5;color:#047857}
        .q-feedback.incorrect{background:#fef2f2;color:#b91c1c}
        .q-explain{margin-top:8px;font-size:13px;color:#475569;background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:10px}
        .q-actions{display:flex;justify-content:flex-end;margin-top:10px}
        .q-opt{display:flex;gap:8px;align-items:center;margin-top:8px;padding:12px;border-radius:12px;border:1px solid #cbd5e1;background:#fff;cursor:pointer;transition:.15s}
        .q-opt.selected{border-color:var(--accent);background:#eff6ff}
        .q-opt.dragging{opacity:.6}
        .q-input{width:100%;border:1px solid #cbd5e1;border-radius:10px;padding:10px}
        .q-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}
        .q-pill{border-radius:999px;padding:12px;font-weight:700;text-align:center;cursor:pointer}
        .group-column{border:1px dashed #cbd5e1;border-radius:12px;padding:10px;min-height:80px}
        .badge{font-size:11px;color:var(--muted)}
        .muted{color:var(--muted);font-size:12px}
        .player-nav{display:flex;justify-content:space-between;align-items:center;margin-top:16px}
        .btn{border:1px solid var(--border);background:#fff;padding:10px 16px;border-radius:999px;font-weight:600;cursor:pointer}
        .btn.primary{background:var(--accent);color:#fff;border-color:var(--accent)}
        .video-container{display:flex;justify-content:center;align-items:center;margin:12px 0}
        .video-container video{max-width:100%;height:auto;border-radius:12px;border:1px solid var(--border)}
        """;
  }

  public String appJs() {
    return """
        import { initState } from './state.js';
        import { createRenderer } from './renderer.js';
        import { createPlayer } from './player.js';
        import { createScormApi } from './scorm.js';

        const loadJson = async (path) => {
          const res = await fetch(path, { cache: 'no-cache' });
          if (!res.ok) throw new Error(`Failed to load ${path}`);
          return res.json();
        };

        const boot = async () => {
          const [courseData, themeData] = await Promise.all([
            loadJson('data/course.json'),
            loadJson('data/theme.json')
          ]);
          const course = courseData?.editorStateSnapshot || {};
          const theme = themeData || {};
          const scorm = createScormApi(courseData?.scormVersion || '2004');
          scorm.initialize();

          const state = initState(course, scorm);
          const renderer = createRenderer(document.getElementById('app'), theme, state);
          const player = createPlayer(course, renderer, state, scorm);
          player.render();

          window.addEventListener('beforeunload', () => {
            scorm.commit();
            scorm.terminate();
          });
        };

        boot().catch((err) => {
          console.error('SCORM boot error', err);
        });
        """;
  }

  public String stateJs() {
    return """
        export const initState = (course, scorm) => {
          const initial = { currentPage: 0, answers: {}, score: 0, checked: {} };
          const suspend = scorm.getSuspendData();
          if (suspend) {
            try {
              const parsed = JSON.parse(suspend);
              Object.assign(initial, parsed);
            } catch (e) {
              console.warn('Failed to parse suspend_data', e);
            }
          }
          const persist = () => {
            scorm.setSuspendData(JSON.stringify(initial));
            scorm.commit();
          };
          return {
            state: initial,
            setAnswer: (id, value) => {
              initial.answers[id] = value;
              persist();
            },
            setCurrentPage: (index) => {
              initial.currentPage = index;
              persist();
            },
            setChecked: (id, value) => {
              initial.checked[id] = value;
              persist();
            },
            setScore: (score, passingScore) => {
              initial.score = score;
              scorm.setScore(score, passingScore);
              persist();
            }
          };
        };
        """;
  }

  public String scormJs() {
    return """
        const findApi = () => window.API_1484_11 || window.API || null;

        export const createScormApi = (version) => {
          const api = findApi();
          const is2004 = !!window.API_1484_11 || version === '2004';
          const safe = (fn) => {
            try { return fn(); } catch (_) { return ''; }
          };
          const startTime = Date.now();

          const toScorm12Time = (ms) => {
            const total = Math.max(0, Math.floor(ms / 1000));
            const h = String(Math.floor(total / 3600)).padStart(2, '0');
            const m = String(Math.floor((total % 3600) / 60)).padStart(2, '0');
            const s = String(total % 60).padStart(2, '0');
            return `${h}:${m}:${s}`;
          };

          const toScorm2004Time = (ms) => {
            const total = Math.max(0, Math.floor(ms / 1000));
            const h = Math.floor(total / 3600);
            const m = Math.floor((total % 3600) / 60);
            const s = total % 60;
            return `PT${h}H${m}M${s}S`;
          };

          return {
            initialize: () => {
              if (!api) return;
              safe(() => (is2004 ? api.Initialize('') : api.LMSInitialize('')));
              if (is2004) safe(() => api.SetValue('cmi.completion_status', 'incomplete'));
              else safe(() => api.LMSSetValue('cmi.core.lesson_status', 'incomplete'));
            },
            terminate: () => {
              if (!api) return;
              const durationMs = Date.now() - startTime;
              if (is2004) {
                safe(() => api.SetValue('cmi.session_time', toScorm2004Time(durationMs)));
                safe(() => api.Terminate(''));
              } else {
                safe(() => api.LMSSetValue('cmi.core.session_time', toScorm12Time(durationMs)));
                safe(() => api.LMSFinish(''));
              }
            },
            commit: () => api && safe(() => (is2004 ? api.Commit('') : api.LMSCommit(''))),
            setValue: (key, value) => api && safe(() => (is2004 ? api.SetValue(key, value) : api.LMSSetValue(key, value))),
            getValue: (key) => api ? safe(() => (is2004 ? api.GetValue(key) : api.LMSGetValue(key))) : '',
            setProgress: (progress) => {
              if (!api) return;
              if (is2004) safe(() => api.SetValue('cmi.progress_measure', String(progress)));
            },
            setScore: (score, passingScore = 80) => {
              if (!api) return;
              const passed = Number(score) >= Number(passingScore);
              if (is2004) {
                safe(() => api.SetValue('cmi.score.min', '0'));
                safe(() => api.SetValue('cmi.score.max', '100'));
                safe(() => api.SetValue('cmi.score.raw', String(score)));
                safe(() => api.SetValue('cmi.success_status', passed ? 'passed' : 'failed'));
              } else {
                safe(() => api.LMSSetValue('cmi.core.score.min', '0'));
                safe(() => api.LMSSetValue('cmi.core.score.max', '100'));
                safe(() => api.LMSSetValue('cmi.core.score.raw', String(score)));
                safe(() => api.LMSSetValue('cmi.core.lesson_status', passed ? 'passed' : 'failed'));
              }
            },
            setCompletion: (completed) => {
              if (!api) return;
              if (is2004) {
                safe(() => api.SetValue('cmi.completion_status', completed ? 'completed' : 'incomplete'));
              } else {
                safe(() => api.LMSSetValue('cmi.core.lesson_status', completed ? 'completed' : 'incomplete'));
              }
            },
            setSuspendData: (value) => {
              if (!api) return;
              if (is2004) safe(() => api.SetValue('cmi.suspend_data', value));
              else safe(() => api.LMSSetValue('cmi.suspend_data', value));
            },
            getSuspendData: () => {
              if (!api) return '';
              return safe(() => (is2004 ? api.GetValue('cmi.suspend_data') : api.LMSGetValue('cmi.suspend_data'))) || '';
            }
          };
        };
        """;
  }

  public String playerJs() {
    return """
        export const createPlayer = (course, renderer, state, scorm) => {
          const pages = [];
          (course.sections || []).forEach((section) => {
            (section.pages || []).forEach((page) => pages.push({ section, page }));
          });

          const allQuestions = pages.flatMap((p) => (p.page?.quizPage?.questions || []));

          const normalize = (v) => typeof v === 'string' ? v.trim().toLowerCase() : String(v).trim().toLowerCase();
          const arrEq = (a, b) => a.length === b.length && a.every((x) => b.includes(x));

          const evaluateQuestion = (q, answer) => {
            const td = q.templateData || {};
            if (q.questionType === 'MCQ_SINGLE') {
              const options = q.options || td.options || [];
              const correctByOptions = options.find((opt) => opt?.isCorrect);
              const correct = correctByOptions?.id || correctByOptions?.value || correctByOptions?.label || td.correctOption || td.correctAnswer || q.correctAnswer;
              return normalize(answer || '') === normalize(correct || '');
            }
            if (q.questionType === 'MCQ_MULTIPLE' || q.questionType === 'MCQ_MULTI') {
              const options = q.options || td.options || [];
              const correctByOptions = options
                .filter((opt) => opt?.isCorrect)
                .map((opt) => opt?.id || opt?.value || opt?.label)
                .filter(Boolean);
              const correct = (correctByOptions.length ? correctByOptions : (td.correctOptions || q.correctAnswers || [])).map(normalize).sort();
              const got = (Array.isArray(answer) ? answer : []).map(normalize).sort();
              return arrEq(got, correct);
            }
            if (q.questionType === 'TRUE_FALSE') {
              const correct = td.correctAnswer ?? q.correctAnswer;
              return String(Boolean(answer)) === String(Boolean(correct));
            }
            if (q.questionType === 'SHORT_ANSWER') {
              const accepted = (td.acceptedAnswers || q.acceptableAnswers || [td.correctAnswer || q.correctAnswer || '']).map(normalize);
              return accepted.includes(normalize(answer || ''));
            }
            if (q.questionType === 'FILL_IN_THE_BLANK' || q.questionType === 'FILL_BLANK') {
              const correct = (q.answers || td.correctAnswers || []).map(normalize);
              const got = (Array.isArray(answer) ? answer : []).map(normalize);
              if (!correct.length) return false;
              return correct.every((c, i) => c === (got[i] || ''));
            }
            return false;
          };

          const calculateScore = () => {
            if (!allQuestions.length) return 100;
            let correctCount = 0;
            allQuestions.forEach((q) => {
              if (evaluateQuestion(q, state.state.answers[q.id])) correctCount += 1;
            });
            return Math.round((correctCount / allQuestions.length) * 100);
          };

          const resolvePassingScore = () => {
            const current = pages[state.state.currentPage];
            const pagePassing = current?.page?.quizPage?.passingScore ?? current?.page?.passingScore;
            const coursePassing = course?.passingScore;
            return Number(pagePassing ?? coursePassing ?? 80);
          };

          const updateCompletion = () => {
            const progress = pages.length ? ((state.state.currentPage + 1) / pages.length) : 1;
            scorm.setProgress(progress);
            scorm.setCompletion(progress >= 1);
            const score = calculateScore();
            state.setScore(score, resolvePassingScore());
          };

          const renderNav = () => {
            const nav = document.createElement('div');
            nav.className = 'player-nav';
            const prev = document.createElement('button');
            prev.className = 'btn';
            prev.textContent = 'Previous';
            prev.disabled = state.state.currentPage === 0;
            prev.onclick = () => {
              if (state.state.currentPage > 0) {
                state.setCurrentPage(state.state.currentPage - 1);
                render();
              }
            };
            const next = document.createElement('button');
            next.className = 'btn primary';
            next.textContent = state.state.currentPage >= pages.length - 1 ? 'Finish' : 'Next';
            next.onclick = () => {
              if (state.state.currentPage < pages.length - 1) {
                state.setCurrentPage(state.state.currentPage + 1);
                render();
              } else {
                updateCompletion();
                scorm.setCompletion(true);
                scorm.commit();
              }
            };
            nav.append(prev, next);
            return nav;
          };

          const render = () => {
            renderer.renderCourse(course, pages[state.state.currentPage], state.state.currentPage);
            renderer.attachNavigation(renderNav());
            updateCompletion();
          };

          return { render };
        };
        """;
  }

  public String rendererJs() {
    return """
        import { renderMcqSingle } from './components/mcq-single.js';
        import { renderMcqMultiple } from './components/mcq-multiple.js';
        import { renderTrueFalse } from './components/true-false.js';
        import { renderShortAnswer } from './components/short-answer.js';
        import { renderFillBlank } from './components/fill-blank.js';
        import { renderGrouping } from './components/grouping.js';

        const toPx = (v) => typeof v === 'number' ? `${v}px` : undefined;
        const spacing = (s) => s ? `${s.top || 0}px ${s.right || 0}px ${s.bottom || 0}px ${s.left || 0}px` : undefined;

        const deepMerge = (a, b) => {
          const out = { ...(a || {}) };
          Object.entries(b || {}).forEach(([k, v]) => {
            if (v && typeof v === 'object' && !Array.isArray(v)) out[k] = deepMerge(out[k], v);
            else out[k] = v;
          });
          return out;
        };

        const applyTokens = (el, tokens) => {
          if (!tokens) return;
          if (tokens.gradient && tokens.gradient.from && tokens.gradient.to) {
            el.style.backgroundImage = `linear-gradient(${tokens.gradient.direction || 'to right'}, ${tokens.gradient.from}, ${tokens.gradient.to})`;
          } else if (tokens.background) {
            el.style.background = tokens.background;
          }
          if (tokens.textColor) el.style.color = tokens.textColor;
          if (tokens.fontFamily) el.style.fontFamily = tokens.fontFamily;
          if (typeof tokens.fontSize === 'number') el.style.fontSize = toPx(tokens.fontSize);
          if (typeof tokens.fontWeight === 'number') el.style.fontWeight = String(tokens.fontWeight);
          if (typeof tokens.lineHeight === 'number') el.style.lineHeight = String(tokens.lineHeight);
          if (typeof tokens.letterSpacing === 'number') el.style.letterSpacing = toPx(tokens.letterSpacing);
          const p = spacing(tokens.padding); if (p) el.style.padding = p;
          const m = spacing(tokens.margin); if (m) el.style.margin = m;
          if (typeof tokens.borderRadius === 'number') el.style.borderRadius = toPx(tokens.borderRadius);
          if (typeof tokens.borderWidth === 'number') el.style.borderWidth = toPx(tokens.borderWidth);
          if (tokens.borderColor) el.style.borderColor = tokens.borderColor;
        };

        const applyLayout = (el, mode, meta) => {
          if (mode !== 'absolute' || !meta) return;
          el.style.position = 'absolute';
          if (typeof meta.x === 'number') el.style.left = toPx(meta.x);
          if (typeof meta.y === 'number') el.style.top = toPx(meta.y);
          if (typeof meta.width === 'number') el.style.width = toPx(meta.width);
          if (typeof meta.height === 'number') el.style.height = toPx(meta.height);
          if (typeof meta.zIndex === 'number') el.style.zIndex = String(meta.zIndex);
          if (typeof meta.rotation === 'number') el.style.transform = `rotate(${meta.rotation}deg)`;
          if (meta.textAlign) el.style.textAlign = meta.textAlign;
        };

        export const createRenderer = (root, theme, state) => {
          const globalTokens = theme?.tokens || {};

          const mergedTokens = (...locals) => locals.reduce((acc, tokens) => deepMerge(acc, tokens || {}), globalTokens);
          const normalize = (v) => typeof v === 'string' ? v.trim().toLowerCase() : String(v).trim().toLowerCase();
          const arrEq = (a, b) => a.length === b.length && a.every((x) => b.includes(x));
          const evaluateQuestion = (q, answer) => {
            const td = q.templateData || {};
            if (q.questionType === 'MCQ_SINGLE') {
              const options = q.options || td.options || [];
              const correctByOptions = options.find((opt) => opt?.isCorrect);
              const correct = correctByOptions?.id || correctByOptions?.value || correctByOptions?.label || td.correctOption || td.correctAnswer || q.correctAnswer;
              return normalize(answer || '') === normalize(correct || '');
            }
            if (q.questionType === 'MCQ_MULTIPLE' || q.questionType === 'MCQ_MULTI') {
              const options = q.options || td.options || [];
              const correctByOptions = options
                .filter((opt) => opt?.isCorrect)
                .map((opt) => opt?.id || opt?.value || opt?.label)
                .filter(Boolean);
              const correct = (correctByOptions.length ? correctByOptions : (td.correctOptions || q.correctAnswers || [])).map(normalize).sort();
              const got = (Array.isArray(answer) ? answer : []).map(normalize).sort();
              return arrEq(got, correct);
            }
            if (q.questionType === 'TRUE_FALSE') {
              const correct = td.correctAnswer ?? q.correctAnswer;
              return String(Boolean(answer)) === String(Boolean(correct));
            }
            if (q.questionType === 'SHORT_ANSWER') {
              const accepted = (td.acceptedAnswers || q.acceptableAnswers || [td.correctAnswer || q.correctAnswer || '']).map(normalize);
              return accepted.includes(normalize(answer || ''));
            }
            if (q.questionType === 'FILL_IN_THE_BLANK' || q.questionType === 'FILL_BLANK') {
              const correct = (q.answers || td.correctAnswers || []).map(normalize);
              const got = (Array.isArray(answer) ? answer : []).map(normalize);
              if (!correct.length) return false;
              return correct.every((c, i) => c === (got[i] || ''));
            }
            return false;
          };

          const renderQuestion = (q) => {
            const container = document.createElement('div');
            container.className = 'question';
            applyTokens(container, mergedTokens(q.themeOverride?.tokens));
            applyLayout(container, q.layoutMode, q.layoutMeta);

            const head = document.createElement('div');
            head.className = 'q-head';
            head.innerHTML = `<div class='badge'>${(q.questionType || '').replaceAll('_', ' ')}</div><h4 class='q-title'>${q.title || ''}</h4>${q.promptHtml || q.textHtml || ''}`;
            container.appendChild(head);

            const body = document.createElement('div');
            body.className = 'q-body';
            if (q.questionType === 'MCQ_SINGLE') body.appendChild(renderMcqSingle(q, state));
            else if (q.questionType === 'MCQ_MULTIPLE' || q.questionType === 'MCQ_MULTI') body.appendChild(renderMcqMultiple(q, state));
            else if (q.questionType === 'TRUE_FALSE') body.appendChild(renderTrueFalse(q, state));
            else if (q.questionType === 'SHORT_ANSWER') body.appendChild(renderShortAnswer(q, state));
            else if (q.questionType === 'FILL_IN_THE_BLANK' || q.questionType === 'FILL_BLANK') body.appendChild(renderFillBlank(q, state));
            else body.appendChild(renderGrouping(q, state));

            const actions = document.createElement('div');
            actions.className = 'q-actions';
            const checkBtn = document.createElement('button');
            checkBtn.className = 'btn';
            checkBtn.textContent = 'Check answer';
            actions.appendChild(checkBtn);

            const feedback = document.createElement('div');
            const explanation = document.createElement('div');

            const renderFeedback = () => {
              const checked = Boolean(state.state.checked?.[q.id]);
              if (!checked) {
                feedback.remove();
                explanation.remove();
                return;
              }
              const isCorrect = evaluateQuestion(q, state.state.answers[q.id]);
              feedback.className = `q-feedback ${isCorrect ? 'correct' : 'incorrect'}`;
              feedback.textContent = isCorrect ? 'Correct' : 'Incorrect';
              body.appendChild(feedback);
              if (q.explanationHtml) {
                explanation.className = 'q-explain';
                explanation.innerHTML = q.explanationHtml;
                body.appendChild(explanation);
              }
            };

            checkBtn.onclick = () => {
              state.setChecked(q.id, true);
              renderFeedback();
            };

            body.appendChild(actions);
            renderFeedback();
            container.appendChild(body);
            return container;
          };

          const renderBlocks = (page, pageTokens) => {
            const area = document.createElement('div');
            area.className = 'content-area';
            (page?.contentPage?.blocks || []).forEach((block) => {
              const blockEl = document.createElement('div');
              blockEl.className = 'block';
              applyTokens(blockEl, mergedTokens(pageTokens, block.themeOverride?.tokens));
              applyLayout(blockEl, block.layoutMode, block.layoutMeta);
              if (block.textHtml) blockEl.insertAdjacentHTML('beforeend', block.textHtml);
              if (block.imageUrl) {
                const img = document.createElement('img');
                img.src = block.imageUrl;
                img.style.maxWidth = '100%';
                img.style.borderRadius = '12px';
                blockEl.appendChild(img);
              }
              area.appendChild(blockEl);
            });
            return area;
          };

          const renderVideo = (page) => {
            const html = page?.contentPage?.textHtml || page?.textHtml || '';
            if (!html || !html.includes('<video')) return null;
            const wrapper = document.createElement('div');
            wrapper.innerHTML = html;
            const video = wrapper.querySelector('video');
            if (!video) return null;
            video.setAttribute('controls', 'controls');
            video.style.maxWidth = '100%';
            video.style.height = 'auto';
            const container = document.createElement('div');
            container.className = 'video-container';
            container.appendChild(video);
            return container;
          };

          const renderPage = (course, payload, currentPageIndex = 0) => {
            root.innerHTML = '';
            const shell = document.createElement('div');
            shell.className = 'course-shell';
            const sectionTokens = payload?.section?.themeOverride?.tokens;
            applyTokens(shell, mergedTokens(sectionTokens));

            if (currentPageIndex === 0) {
              const header = document.createElement('section');
              header.className = 'course-header';

              const headerTitle = document.createElement('h1');
              headerTitle.textContent = course?.title || course?.courseTitle || 'Untitled Course';
              header.appendChild(headerTitle);

              const coverWrap = document.createElement('div');
              coverWrap.className = 'course-cover';
              if (course?.coverImageUrl) {
                const img = document.createElement('img');
                img.src = course.coverImageUrl;
                img.alt = 'Course cover';
                coverWrap.appendChild(img);
              } else {
                const noCover = document.createElement('div');
                noCover.style.height = '240px';
                noCover.style.display = 'flex';
                noCover.style.alignItems = 'center';
                noCover.style.justifyContent = 'center';
                noCover.style.color = '#94a3b8';
                noCover.textContent = 'No cover image';
                coverWrap.appendChild(noCover);
              }
              header.appendChild(coverWrap);

              const objective = document.createElement('div');
              objective.className = 'course-objective';
              objective.textContent = course?.description || 'Add learning objective...';
              header.appendChild(objective);

              shell.appendChild(header);
            }

            const title = document.createElement('h1');
            title.textContent = payload?.section?.title || payload?.page?.title || '';
            shell.appendChild(title);

            if (payload?.page) {
              const pageTokens = payload.page.themeOverride?.tokens;
              const page = document.createElement('div');
              page.className = 'page';
              applyTokens(page, mergedTokens(sectionTokens, pageTokens));
              applyLayout(page, payload.page.layoutMode, payload.page.layoutMeta);
              page.insertAdjacentHTML('beforeend', `<h3>${payload.page.title || ''}</h3><div class='badge'>${payload.page.pageType || ''}</div>`);
              if (payload.page.textHtml) page.insertAdjacentHTML('beforeend', payload.page.textHtml);
              if (payload.page.pageType === 'CONTENT') {
                const video = renderVideo(payload.page);
                if (video) page.appendChild(video);
                page.appendChild(renderBlocks(payload.page, pageTokens));
              }
              if (payload.page.pageType === 'QUIZ') {
                const quizArea = document.createElement('div');
                quizArea.className = 'quiz-area';
                (payload.page.quizPage?.questions || []).forEach((q) => quizArea.appendChild(renderQuestion(q)));
                page.appendChild(quizArea);
              }
              shell.appendChild(page);
            }
            root.appendChild(shell);
          };

          return {
            renderCourse: (course, payload, currentPageIndex) => renderPage(course, payload, currentPageIndex),
            renderPage,
            attachNavigation: (nav) => root.appendChild(nav)
          };
        };
        """;
  }

  public String mcqSingleJs() {
    return """
        export const renderMcqSingle = (q, state) => {
          const container = document.createElement('div');
          const options = q.templateData?.options || q.options || [];
          const selected = state.state.answers[q.id];
          options.forEach((opt) => {
            const key = opt.id || opt.value || opt.label || opt.labelHtml;
            const row = document.createElement('div');
            row.className = 'q-opt';
            row.innerHTML = opt.labelHtml || opt.label || opt.value || '';
            if (selected === key) row.classList.add('selected');
            row.onclick = () => {
              state.setAnswer(q.id, key);
              container.querySelectorAll('.q-opt').forEach((el) => el.classList.remove('selected'));
              row.classList.add('selected');
            };
            container.appendChild(row);
          });
          return container;
        };
        """;
  }

  public String mcqMultipleJs() {
    return """
        export const renderMcqMultiple = (q, state) => {
          const container = document.createElement('div');
          const options = q.templateData?.options || q.options || [];
          const selected = new Set(state.state.answers[q.id] || []);
          options.forEach((opt) => {
            const row = document.createElement('div');
            row.className = 'q-opt';
            const key = opt.id || opt.value || opt.label || opt.labelHtml;
            row.innerHTML = opt.labelHtml || opt.label || opt.value || '';
            if (selected.has(key)) row.classList.add('selected');
            row.onclick = () => {
              if (selected.has(key)) {
                selected.delete(key);
                row.classList.remove('selected');
              } else {
                selected.add(key);
                row.classList.add('selected');
              }
              state.setAnswer(q.id, Array.from(selected));
            };
            container.appendChild(row);
          });
          return container;
        };
        """;
  }

  public String trueFalseJs() {
    return """
        export const renderTrueFalse = (q, state) => {
          const container = document.createElement('div');
          container.className = 'q-grid';
          const selected = state.state.answers[q.id];
          ['TRUE', 'FALSE'].forEach((label) => {
            const pill = document.createElement('div');
            pill.className = 'q-pill';
            pill.textContent = label;
            pill.style.background = label === 'TRUE' ? 'linear-gradient(to right,#10b981,#22c55e)' : 'linear-gradient(to right,#f43f5e,#ef4444)';
            pill.style.color = '#fff';
            if (selected === (label === 'TRUE')) pill.style.outline = '3px solid rgba(37,99,235,.35)';
            pill.onclick = () => {
              state.setAnswer(q.id, label === 'TRUE');
              container.querySelectorAll('.q-pill').forEach((el) => el.style.outline = 'none');
              pill.style.outline = '3px solid rgba(37,99,235,.35)';
            };
            container.appendChild(pill);
          });
          return container;
        };
        """;
  }

  public String shortAnswerJs() {
    return """
        export const renderShortAnswer = (q, state) => {
          const container = document.createElement('div');
          const input = document.createElement('input');
          input.className = 'q-input';
          input.placeholder = 'Type answer';
          if (q.charLimit && Number.isFinite(q.charLimit)) input.maxLength = q.charLimit;
          input.value = state.state.answers[q.id] || '';
          input.oninput = (e) => state.setAnswer(q.id, e.target.value);
          container.appendChild(input);
          return container;
        };
        """;
  }

  public String fillBlankJs() {
    return """
        export const renderFillBlank = (q, state) => {
          const container = document.createElement('div');
          const sentence = q.templateData?.sentence || q.templateData?.prompt || '';
          const parts = sentence.split('____');
          const answers = state.state.answers[q.id] || [];
          parts.forEach((part, idx) => {
            const span = document.createElement('span');
            span.textContent = part;
            span.style.marginRight = '6px';
            container.appendChild(span);
            if (idx < parts.length - 1) {
              const input = document.createElement('input');
              input.className = 'q-input';
              input.style.display = 'inline-block';
              input.style.width = '160px';
              input.value = answers[idx] || '';
              input.oninput = (e) => {
                const next = [...answers];
                next[idx] = e.target.value;
                state.setAnswer(q.id, next);
              };
              container.appendChild(input);
            }
          });
          return container;
        };
        """;
  }

  public String groupingJs() {
    return """
        export const renderGrouping = (q, state) => {
          const container = document.createElement('div');
          container.className = 'q-grid';
          const groups = q.groups || [];
          const items = q.items || [];
          const current = { ...(state.state.answers[q.id] || {}) };

          groups.forEach((group) => {
            const col = document.createElement('div');
            col.className = 'group-column';
            const title = document.createElement('div');
            title.className = 'muted';
            title.textContent = group.title;
            col.appendChild(title);
            col.ondragover = (e) => e.preventDefault();
            col.ondrop = (e) => {
              const itemId = e.dataTransfer.getData('text/plain');
              current[itemId] = group.id;
              state.setAnswer(q.id, { ...current });
              const itemEl = container.querySelector(`[data-item-id="${itemId}"]`);
              if (itemEl) col.appendChild(itemEl);
            };
            container.appendChild(col);
          });

          const pool = document.createElement('div');
          pool.className = 'group-column';
          const poolTitle = document.createElement('div');
          poolTitle.className = 'muted';
          poolTitle.textContent = 'Item pool';
          pool.appendChild(poolTitle);
          pool.ondragover = (e) => e.preventDefault();
          pool.ondrop = (e) => {
            const itemId = e.dataTransfer.getData('text/plain');
            delete current[itemId];
            state.setAnswer(q.id, { ...current });
            const itemEl = container.querySelector(`[data-item-id="${itemId}"]`);
            if (itemEl) pool.appendChild(itemEl);
          };

          items.forEach((item) => {
            const tag = document.createElement('div');
            tag.className = 'q-opt';
            tag.setAttribute('data-item-id', item.id);
            tag.draggable = true;
            tag.textContent = item.label;
            tag.ondragstart = (e) => {
              e.dataTransfer.setData('text/plain', item.id);
              tag.classList.add('dragging');
            };
            tag.ondragend = () => tag.classList.remove('dragging');

            const owner = current[item.id];
            const groupColumn = owner ? container.querySelectorAll('.group-column')[groups.findIndex((g) => g.id === owner)] : null;
            if (groupColumn) groupColumn.appendChild(tag);
            else pool.appendChild(tag);
          });

          container.appendChild(pool);
          return container;
        };
        """;
  }
}
