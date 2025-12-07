let scorm;
let currentQuestionIndex = 0;
let userAnswers = [];
let quizStartTime;

// This object will hold the state and data for the quiz
const quiz = {
    data: null,
};

// --- Initialization ---
function initializeQuiz(data) {
    scorm = new ScormAPI();
    scorm.initialize();

    quiz.data = data;
    userAnswers = new Array(quiz.data.questions.length).fill(null);

    applyTheme(quiz.data.theme);

    document.getElementById('start-btn').addEventListener('click', startQuiz);
    document.getElementById('next-btn').addEventListener('click', nextQuestion);
    document.getElementById('prev-btn').addEventListener('click', prevQuestion);
    document.getElementById('submit-btn').addEventListener('click', submitQuiz);
    document.getElementById('review-btn').addEventListener('click', showReview);
    document.getElementById('back-to-results-btn').addEventListener('click', backToResults);
}

function applyTheme(theme) {
    if (!theme || Object.keys(theme).length === 0) return;
    const style = document.createElement('style');
    let css = ':root {';
    if (theme.primaryColor) css += `--primary-color: ${theme.primaryColor};`;
    if (theme.secondaryColor) css += `--secondary-color: ${theme.secondaryColor};`;
    if (theme.backgroundColor) css += `--background-color: ${theme.backgroundColor};`;
    if (theme.textColor) css += `--text-color: ${theme.textColor};`;
    if (theme.fontFamily) css += `--font-family: ${theme.fontFamily};`;
    css += '}';
    style.innerHTML = css;
    document.head.appendChild(style);
}

function startQuiz() {
    quizStartTime = new Date();
    scorm.setCompletionStatus('incomplete');
    document.getElementById('start-screen').classList.remove('active');
    document.getElementById('quiz-screen').classList.add('active');
    currentQuestionIndex = 0; // Reset to first question
    showQuestion(currentQuestionIndex);
}

// --- Question Rendering ---
function showQuestion(index) {
    const question = quiz.data.questions[index];
    const container = document.getElementById('question-container');
    container.innerHTML = '';

    document.getElementById('question-counter').textContent = `Câu hỏi ${index + 1} / ${quiz.data.questions.length}`;

    // Question text and image
    const questionHeader = document.createElement('div');
    const questionText = document.createElement('p');
    questionText.className = 'question-text';
    questionText.textContent = question.text;
    questionHeader.appendChild(questionText);

    if (question.imageUrl) {
        const img = document.createElement('img');
        img.src = question.imageUrl;
        img.className = 'question-image';
        questionHeader.appendChild(img);
    }
    container.appendChild(questionHeader);

    // Render based on type
    switch (question.questionType) {
        case 'MULTIPLE_CHOICE':
            renderMultipleChoice(question, index);
            break;
        case 'TRUE_FALSE':
            renderTrueFalse(question, index);
            break;
        case 'MATCHING':
            renderMatching(question, index);
            break;
        case 'SHORT_ANSWER':
            renderShortAnswer(question, index);
            break;
    }

    updateNavigation();
    updateProgressBar();
}

function renderMultipleChoice(question, index) {
    const answersContainer = document.createElement('div');
    answersContainer.className = 'answers';
    question.answers.forEach((answer, answerIndex) => {
        const answerDiv = document.createElement('div');
        answerDiv.className = 'answer';
        const input = document.createElement('input');
        input.type = 'radio';
        input.name = `q-${index}`;
        input.id = `q${index}-a${answerIndex}`;
        input.value = answerIndex;
        if (userAnswers[index] === answerIndex) input.checked = true;
        input.addEventListener('change', () => userAnswers[index] = answerIndex);

        const label = document.createElement('label');
        label.htmlFor = `q${index}-a${answerIndex}`;

        // Add text content
        if (answer.text) {
            const answerText = document.createElement('span');
            answerText.textContent = answer.text;
            label.appendChild(answerText);
        }

        // Add image if it exists
        if (answer.imageUrl) {
            const img = document.createElement('img');
            img.src = answer.imageUrl;
            img.className = 'answer-image'; // Add a class for styling
            label.appendChild(img);
        }

        answerDiv.appendChild(input);
        answerDiv.appendChild(label);
        answersContainer.appendChild(answerDiv);
    });
    document.getElementById('question-container').appendChild(answersContainer);
}

function renderTrueFalse(_question, index) {
    const answersContainer = document.createElement('div');
    answersContainer.className = 'answers';
    // The correct answer is stored in the first answer object
    const options = [ {text: 'Đúng', value: true}, {text: 'Sai', value: false} ];
    options.forEach((option, optionIndex) => {
        const answerDiv = document.createElement('div');
        answerDiv.className = 'answer';
        const input = document.createElement('input');
        input.type = 'radio';
        input.name = `q-${index}`;
        input.id = `q${index}-a${optionIndex}`;
        input.value = option.value;
        if (userAnswers[index] === option.value) input.checked = true;
        input.addEventListener('change', () => userAnswers[index] = option.value);

        const label = document.createElement('label');
        label.htmlFor = `q${index}-a${optionIndex}`;
        label.textContent = option.text;

        answerDiv.appendChild(input);
        answerDiv.appendChild(label);
        answersContainer.appendChild(answerDiv);
    });
    document.getElementById('question-container').appendChild(answersContainer);
}

function renderShortAnswer(_question, index) {
    const container = document.getElementById('question-container');
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'short-answer-input';
    input.placeholder = 'Nhập câu trả lời của bạn...';
    input.value = userAnswers[index] || '';
    input.addEventListener('input', () => userAnswers[index] = input.value.trim());
    container.appendChild(input);
}

function renderMatching(question, index) {
    const container = document.getElementById('question-container');
    const matchingContainer = document.createElement('div');
    matchingContainer.className = 'matching-container';

    const leftCol = document.createElement('div');
    leftCol.className = 'matching-col';
    const rightCol = document.createElement('div');
    rightCol.className = 'matching-col';

    // Shuffle right column for matching
    const rightItems = question.answers.map(a => a.matchValue).sort(() => Math.random() - 0.5);

    question.answers.forEach((answer, i) => {
        const leftItem = document.createElement('div');
        leftItem.className = 'matching-item';
        leftItem.textContent = answer.text;
        leftItem.dataset.index = i;
        leftCol.appendChild(leftItem);

        const rightItem = document.createElement('div');
        rightItem.className = 'matching-item';
        rightItem.textContent = rightItems[i];
        rightItem.dataset.text = rightItems[i];
        rightCol.appendChild(rightItem);
    });

    matchingContainer.appendChild(leftCol);
    matchingContainer.appendChild(rightCol);
    container.appendChild(matchingContainer);

    // Simple select-based matching logic
    let selectedLeft = null;
    if (!userAnswers[index]) userAnswers[index] = {};

    leftCol.childNodes.forEach(item => {
        item.addEventListener('click', () => {
            if (selectedLeft) selectedLeft.classList.remove('selected');
            selectedLeft = item;
            selectedLeft.classList.add('selected');
        });
    });

    rightCol.childNodes.forEach(item => {
        item.addEventListener('click', () => {
            if (selectedLeft) {
                const leftIndex = selectedLeft.dataset.index;
                const rightText = item.dataset.text;
                userAnswers[index][leftIndex] = rightText;

                // Visual feedback
                document.querySelectorAll('.paired-' + leftIndex).forEach(p => p.classList.remove('paired-' + leftIndex, 'paired'));
                item.classList.add('paired', 'paired-' + leftIndex);
                selectedLeft.classList.add('paired', 'paired-' + leftIndex);

                selectedLeft.classList.remove('selected');
                selectedLeft = null;
            }
        });
    });
}

// --- Navigation and Progress ---
function updateNavigation() {
    document.getElementById('prev-btn').style.display = currentQuestionIndex === 0 ? 'none' : 'inline-block';
    document.getElementById('next-btn').style.display = currentQuestionIndex === quiz.data.questions.length - 1 ? 'none' : 'inline-block';
    document.getElementById('submit-btn').style.display = currentQuestionIndex === quiz.data.questions.length - 1 ? 'inline-block' : 'none';
}

function updateProgressBar() {
    const progress = (currentQuestionIndex + 1) / quiz.data.questions.length * 100;
    document.getElementById('progress').style.width = `${progress}%`;
}

function nextQuestion() {
    if (currentQuestionIndex < quiz.data.questions.length - 1) {
        currentQuestionIndex++;
        showQuestion(currentQuestionIndex);
    }
}

function prevQuestion() {
    if (currentQuestionIndex > 0) {
        currentQuestionIndex--;
        showQuestion(currentQuestionIndex);
    }
}

// --- Submission and Scoring ---
function submitQuiz() {
    const { score } = calculateScore();
    const successStatus = score >= quiz.data.passingScore ? 'passed' : 'failed';

    recordInteractions();

    scorm.setScore(score, 100, 0);
    scorm.setSuccessStatus(successStatus);
    scorm.setCompletionStatus('completed');
    const sessionTime = new Date() - quizStartTime;
    const formattedSessionTime = formatCMITimespan(sessionTime);
    scorm.setValue("cmi.session_time", formattedSessionTime);
    scorm.commit();

    showResults(score, successStatus);
}

function calculateScore() {
    let correctCount = 0;
    quiz.data.questions.forEach((question, index) => {
        const userAnswer = userAnswers[index];
        let isCorrect = false;
        switch (question.questionType) {
            case 'MULTIPLE_CHOICE':
                const correctChoiceIndex = question.answers.findIndex(a => a.correct);
                isCorrect = userAnswer === correctChoiceIndex;
                break;
            case 'TRUE_FALSE':
                const correctBool = question.answers[0].correct;
                isCorrect = userAnswer === correctBool;
                break;
            case 'SHORT_ANSWER':
                const correctAnswers = question.answers.filter(a => a.correct).map(a => a.text.toLowerCase().trim());
                isCorrect = correctAnswers.includes((userAnswer || '').toLowerCase());
                break;
            case 'MATCHING':
                let allMatched = true;
                if (typeof userAnswer !== 'object' || userAnswer === null || Object.keys(userAnswer).length !== question.answers.length) {
                    allMatched = false;
                } else {
                    for (const leftIndex in userAnswer) {
                        const correctMatch = question.answers[leftIndex].matchValue;
                        if (userAnswer[leftIndex] !== correctMatch) {
                            allMatched = false;
                            break;
                        }
                    }
                }
                isCorrect = allMatched;
                break;
        }
        if (isCorrect) correctCount++;
    });
    const score = (correctCount / quiz.data.questions.length) * 100;
    return { score, correctAnswers: correctCount, total: quiz.data.questions.length };
}

function recordInteractions() {
    // This function can be expanded to record detailed interactions for each question type
}

function showResults(score, successStatus) {
    document.getElementById('quiz-screen').classList.remove('active');
    document.getElementById('result-screen').classList.add('active');

    const scoreDisplay = document.getElementById('score-display');
    scoreDisplay.textContent = `Điểm của bạn: ${score.toFixed(2)}%`;

    const resultMessage = document.getElementById('result-message');
    if (successStatus === 'passed') {
        resultMessage.textContent = 'Chúc mừng! Bạn đã vượt qua bài kiểm tra.';
        resultMessage.className = 'result-message success';
    } else {
        resultMessage.textContent = 'Rất tiếc, bạn chưa đạt. Hãy thử lại!';
        resultMessage.className = 'result-message failed';
    }

    // Show review button if review is allowed
    const reviewBtn = document.getElementById('review-btn');
    if (quiz.data.reviewMode && quiz.data.reviewMode !== 'NO_REVIEW') {
        reviewBtn.style.display = 'inline-block';
    } else {
        reviewBtn.style.display = 'none';
    }
}

function showReview() {
    document.getElementById('result-screen').classList.remove('active');
    document.getElementById('review-screen').classList.add('active');
    renderReview();
}

function backToResults() {
    document.getElementById('review-screen').classList.remove('active');
    document.getElementById('result-screen').classList.add('active');
}

function renderReview() {
    const reviewContainer = document.getElementById('review-container');
    reviewContainer.innerHTML = '';
    const showCorrect = quiz.data.reviewMode === 'REVIEW_WITH_ANSWERS';

    quiz.data.questions.forEach((question, index) => {
        const questionEl = document.createElement('div');
        questionEl.className = 'review-question';

        // Create question header
        const questionHeader = document.createElement('div');
        let questionHTML = `<p class="question-text"><strong>Câu ${index + 1}:</strong> ${question.text}</p>`;
        if (question.imageUrl) {
            questionHTML += `<img src="${question.imageUrl}" class="question-image">`;
        }
        questionHeader.innerHTML = questionHTML;
        questionEl.appendChild(questionHeader);

        const answersEl = document.createElement('div');
        answersEl.className = 'review-answers';
        const userAnswer = userAnswers[index];

        switch (question.questionType) {
            case 'MULTIPLE_CHOICE':
                renderMultipleChoiceReview(question, userAnswer, showCorrect, answersEl);
                break;

            case 'TRUE_FALSE':
                renderTrueFalseReview(question, userAnswer, showCorrect, answersEl);
                break;

            case 'SHORT_ANSWER':
                renderShortAnswerReview(question, userAnswer, showCorrect, answersEl);
                break;

            case 'MATCHING':
                renderMatchingReview(question, userAnswer, showCorrect, answersEl);
                break;
        }

        questionEl.appendChild(answersEl);
        reviewContainer.appendChild(questionEl);
    });
}

function renderMultipleChoiceReview(question, userAnswer, showCorrect, answersEl) {
    question.answers.forEach((answer, answerIndex) => {
        const answerEl = document.createElement('div');
        answerEl.className = 'answer-review-item';

        // Add classes for styling
        if (userAnswer === answerIndex) {
            answerEl.classList.add('user-selected');
        }
        if (showCorrect && answer.correct) {
            answerEl.classList.add('correct-answer');
        }

        // Build answer content
        if (answer.text) {
            const textSpan = document.createElement('span');
            textSpan.textContent = answer.text;
            answerEl.appendChild(textSpan);
        }
        if (answer.imageUrl) {
            const img = document.createElement('img');
            img.src = answer.imageUrl;
            img.className = 'answer-image';
            answerEl.appendChild(img);
        }

        answersEl.appendChild(answerEl);
    });
}

function renderTrueFalseReview(question, userAnswer, showCorrect, answersEl) {
    const tfOptions = [{ text: 'Đúng', value: true }, { text: 'Sai', value: false }];
    tfOptions.forEach(option => {
        const answerEl = document.createElement('div');
        answerEl.className = 'answer-review-item';
        const isCorrect = question.answers[0].correct === option.value;

        if (userAnswer === option.value) {
            answerEl.classList.add('user-selected');
        }
        if (showCorrect && isCorrect) {
            answerEl.classList.add('correct-answer');
        }

        const span = document.createElement('span');
        span.textContent = option.text;
        answerEl.appendChild(span);
        answersEl.appendChild(answerEl);
    });
}

function renderShortAnswerReview(question, userAnswer, showCorrect, answersEl) {
    const shortAnswerEl = document.createElement('div');
    shortAnswerEl.className = 'answer-review-item';
    const correctAnswers = question.answers.filter(a => a.correct).map(a => a.text.toLowerCase().trim());
    const isCorrect = correctAnswers.includes((userAnswer || '').toLowerCase());

    // User's answer
    const userAnswerLabel = document.createElement('strong');
    userAnswerLabel.textContent = 'Bạn trả lời: ';
    const userAnswerText = document.createElement('span');
    userAnswerText.textContent = userAnswer || '(Chưa trả lời)';
    shortAnswerEl.appendChild(userAnswerLabel);
    shortAnswerEl.appendChild(userAnswerText);

    if (showCorrect) {
        if (isCorrect) {
            shortAnswerEl.classList.add('correct-answer');
        } else {
            shortAnswerEl.classList.add('incorrect-answer');
            // Add correct answer
            const correctLabel = document.createElement('div');
            correctLabel.style.marginTop = '0.5rem';
            correctLabel.style.paddingTop = '0.5rem';
            correctLabel.style.borderTop = '1px solid rgba(0,0,0,0.1)';
            const correctLabelStrong = document.createElement('strong');
            correctLabelStrong.textContent = 'Đáp án đúng: ';
            const correctAnswerText = document.createElement('span');
            correctAnswerText.textContent = correctAnswers.join(' hoặc ');
            correctLabel.appendChild(correctLabelStrong);
            correctLabel.appendChild(correctAnswerText);
            shortAnswerEl.appendChild(correctLabel);
        }
    }
    answersEl.appendChild(shortAnswerEl);
}

function renderMatchingReview(question, userAnswer, showCorrect, answersEl) {
    const matchingContainer = document.createElement('div');
    matchingContainer.className = 'review-matching-container';

    question.answers.forEach((answer, i) => {
        const userMatch = (userAnswer && userAnswer[i]) ? userAnswer[i] : null;
        const isCorrect = userMatch === answer.matchValue;

        const matchEl = document.createElement('div');
        matchEl.className = 'review-matching-pair';

        // Add correct/incorrect styling
        if (showCorrect) {
            if (userMatch && isCorrect) {
                matchEl.classList.add('correct-answer');
            } else if (userMatch) {
                matchEl.classList.add('incorrect-answer');
            }
        }

        // Create matching item display
        const matchingItem = document.createElement('div');
        matchingItem.className = 'review-matching-item';

        const leftSpan = document.createElement('span');
        leftSpan.textContent = answer.text;
        const arrowSpan = document.createElement('span');
        arrowSpan.textContent = '→';
        arrowSpan.style.fontWeight = 'bold';
        const rightSpan = document.createElement('span');
        rightSpan.textContent = userMatch || '(Chưa trả lời)';

        matchingItem.appendChild(leftSpan);
        matchingItem.appendChild(arrowSpan);
        matchingItem.appendChild(rightSpan);
        matchEl.appendChild(matchingItem);

        // Add correct answer if showing and user's answer is wrong
        if (showCorrect && userMatch && !isCorrect) {
            const correctAnswerDiv = document.createElement('div');
            correctAnswerDiv.className = 'correct-answer-text';
            correctAnswerDiv.innerHTML = `<strong>Đáp án đúng:</strong> ${answer.matchValue}`;
            matchEl.appendChild(correctAnswerDiv);
        }

        matchingContainer.appendChild(matchEl);
    });
    answersEl.appendChild(matchingContainer);
}


function restartQuiz() {
    currentQuestionIndex = 0;
    userAnswers = new Array(quiz.data.questions.length).fill(null);
    scorm.setCompletionStatus('incomplete');
    scorm.setExit('suspend');
    scorm.commit();
    document.getElementById('result-screen').classList.remove('active');
    startQuiz();
}


// --- Utility Functions ---
function formatCMITimespan(milliseconds) {
    const totalSeconds = Math.round(milliseconds / 1000);
    const seconds = totalSeconds % 60;
    const totalMinutes = Math.floor(totalSeconds / 60);
    const minutes = totalMinutes % 60;
    const hours = Math.floor(totalMinutes / 60);

    let result = "PT";
    if (hours > 0) {
        result += hours + "H";
    }
    if (minutes > 0) {
        result += minutes + "M";
    }
    result += seconds + "S";

    return result;
}

// --- SCORM Handling ---
window.addEventListener('beforeunload', function() {
    if (scorm.getCompletionStatus() !== 'completed') {
        scorm.setExit('suspend');
    }
    scorm.commit();
});
