# Moodle SCORM Validation Checklist

Use this checklist after exporting a SCORM package from the backend to verify parity and LMS compatibility.

## A. Import & Launch

- [ ] Export package from API (`SCORM_12` and `SCORM_2004` tested separately)
- [ ] Upload zip to Moodle as SCORM activity
- [ ] Package imports without manifest errors
- [ ] Launch opens `index.html` and renders first page
- [ ] Course header is visible only on first page (title, cover, objective)

## B. Data Parity (Editor -> SCORM)

- [ ] Course title matches editor
- [ ] Course description/objective matches editor
- [ ] Cover image URL displays correctly
- [ ] Section/page titles render in correct order
- [ ] Content blocks render with correct HTML/image/video
- [ ] Quiz questions appear with correct type/order
- [ ] Question options support HTML labels
- [ ] Question explanation displays after checking answer

## C. Interaction Parity

- [ ] MCQ single selection works
- [ ] MCQ multiple selection works
- [ ] True/False selection works
- [ ] Short answer input works (char limit if configured)
- [ ] Fill in the blank input works
- [ ] Matching interaction works
- [ ] “Check answer” updates Correct/Incorrect feedback
- [ ] Navigation Previous/Next/Finish works

## D. SCORM Runtime Data

### D1. SCORM 2004
- [ ] `cmi.completion_status` transitions to `completed` at finish
- [ ] `cmi.progress_measure` updates as learner moves pages
- [ ] `cmi.score.raw` stored
- [ ] `cmi.success_status` set by passing score
- [ ] `cmi.suspend_data` stores page and answers
- [ ] Resume restores current page + answers + checked state
- [ ] `cmi.session_time` stored on terminate

### D2. SCORM 1.2
- [ ] `cmi.core.lesson_status` updates (incomplete/completed/passed/failed)
- [ ] `cmi.core.score.raw` stored
- [ ] `cmi.core.score.min/max` stored
- [ ] `cmi.suspend_data` stores page and answers
- [ ] Resume restores state
- [ ] `cmi.core.session_time` stored on terminate

## E. Regression Matrix (minimum)

- [ ] Chrome latest + Moodle
- [ ] Edge latest + Moodle
- [ ] Package reopened after browser close (resume scenario)
- [ ] Different passingScore values (50/80/90)
- [ ] Course without quiz pages (completion should still work)

## F. API/DB Validation

- [ ] `course.description` persisted
- [ ] `course.cover_image_url` persisted
- [ ] `editorStateSnapshot` normalized in exported `data/editor-state.json`
- [ ] Manifest includes all runtime assets and data files

## Acceptance Criteria

A package is accepted when all items above are checked for both SCORM 1.2 and SCORM 2004 in Moodle test environment.

