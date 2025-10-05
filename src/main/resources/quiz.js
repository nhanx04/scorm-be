/**
 * Quiz functionality with SCORM integration
 */
class Quiz {
    constructor(data) {
        this.data = data;
        this.currentQuestionIndex = 0;
        this.userAnswers = [];
        this.score = 0;
        this.isCompleted = false;
        this.startTime = null;
        this.endTime = null;
        
        this.initializeElements();
        this.bindEvents();
    }

    initializeElements() {
        this.startScreen = document.getElementById('start-screen');
        this.quizScreen = document.getElementById('quiz-screen');
        this.resultScreen = document.getElementById('result-screen');
        this.questionContainer = document.getElementById('question-container');
        this.questionCounter = document.getElementById('question-counter');
        this.progressBar = document.getElementById('progress');
        
        this.startBtn = document.getElementById('start-btn');
        this.prevBtn = document.getElementById('prev-btn');
        this.nextBtn = document.getElementById('next-btn');
        this.submitBtn = document.getElementById('submit-btn');
        this.restartBtn = document.getElementById('restart-btn');
        
        this.scoreDisplay = document.getElementById('score-display');
        this.resultMessage = document.getElementById('result-message');
    }

    bindEvents() {
        this.startBtn.addEventListener('click', () => this.startQuiz());
        this.prevBtn.addEventListener('click', () => this.previousQuestion());
        this.nextBtn.addEventListener('click', () => this.nextQuestion());
        this.submitBtn.addEventListener('click', () => this.submitQuiz());
        this.restartBtn.addEventListener('click', () => this.restartQuiz());
    }

    startQuiz() {
        this.startTime = new Date();
        this.currentQuestionIndex = 0;
        this.userAnswers = new Array(this.data.questions.length).fill(null);
        this.score = 0;
        this.isCompleted = false;
        
        // Initialize SCORM
        if (window.scormAPI) {
            window.scormAPI.initialize();
            window.scormAPI.setValue("cmi.completion_status", "incomplete");
            window.scormAPI.setValue("cmi.success_status", "unknown");
            window.scormAPI.commit();
        }
        
        this.showScreen('quiz');
        this.displayQuestion();
        this.updateProgress();
    }

    showScreen(screenName) {
        document.querySelectorAll('.screen').forEach(screen => {
            screen.classList.remove('active');
        });
        
        switch(screenName) {
            case 'start':
                this.startScreen.classList.add('active');
                break;
            case 'quiz':
                this.quizScreen.classList.add('active');
                break;
            case 'result':
                this.resultScreen.classList.add('active');
                break;
        }
    }

    displayQuestion() {
        const question = this.data.questions[this.currentQuestionIndex];
        
        this.questionCounter.textContent = `Câu ${this.currentQuestionIndex + 1} / ${this.data.questions.length}`;
        
        let html = `
            <div class="question">
                <h3>${this.escapeHtml(question.text)}</h3>
                <div class="answers">
        `;
        
        question.answers.forEach((answer, index) => {
            const isChecked = this.userAnswers[this.currentQuestionIndex] === index ? 'checked' : '';
            html += `
                <label class="answer-option">
                    <input type="radio" name="question_${question.id}" value="${index}" ${isChecked}
                           onchange="quiz.selectAnswer(${index})">
                    <span class="answer-text">${this.escapeHtml(answer.text)}</span>
                </label>
            `;
        });
        
        html += `
                </div>
            </div>
        `;
        
        this.questionContainer.innerHTML = html;
        this.updateNavigationButtons();
    }

    selectAnswer(answerIndex) {
        this.userAnswers[this.currentQuestionIndex] = answerIndex;
        this.updateNavigationButtons();
    }

    updateNavigationButtons() {
        this.prevBtn.disabled = this.currentQuestionIndex === 0;
        
        const isLastQuestion = this.currentQuestionIndex === this.data.questions.length - 1;
        
        if (isLastQuestion) {
            this.nextBtn.style.display = 'none';
            this.submitBtn.style.display = 'inline-block';
        } else {
            this.nextBtn.style.display = 'inline-block';
            this.submitBtn.style.display = 'none';
        }
    }

    previousQuestion() {
        if (this.currentQuestionIndex > 0) {
            this.currentQuestionIndex--;
            this.displayQuestion();
            this.updateProgress();
        }
    }

    nextQuestion() {
        if (this.currentQuestionIndex < this.data.questions.length - 1) {
            this.currentQuestionIndex++;
            this.displayQuestion();
            this.updateProgress();
        }
    }

    updateProgress() {
        const progress = ((this.currentQuestionIndex + 1) / this.data.questions.length) * 100;
        this.progressBar.style.width = progress + '%';
    }

    submitQuiz() {
        if (!this.validateAllAnswered()) {
            alert('Vui lòng trả lời tất cả các câu hỏi trước khi nộp bài.');
            return;
        }
        
        this.endTime = new Date();
        this.calculateScore();
        this.recordScormData();
        this.showResults();
    }

    validateAllAnswered() {
        return this.userAnswers.every(answer => answer !== null);
    }

    calculateScore() {
        let correctAnswers = 0;
        
        this.data.questions.forEach((question, index) => {
            const userAnswer = this.userAnswers[index];
            const correctAnswer = question.correctAnswerIndex;
            
            if (userAnswer === correctAnswer) {
                correctAnswers++;
            }
            
            // Record interaction in SCORM
            if (window.scormAPI) {
                const result = userAnswer === correctAnswer ? "correct" : "incorrect";
                window.scormAPI.recordInteraction(
                    question.id,
                    "choice",
                    userAnswer.toString(),
                    result,
                    question.text
                );
            }
        });
        
        this.score = Math.round((correctAnswers / this.data.questions.length) * 100);
        this.isCompleted = true;
    }

    recordScormData() {
        if (window.scormAPI) {
            const isPassed = this.score >= this.data.passingScore;
            
            window.scormAPI.setScore(this.score, 100);
            window.scormAPI.setCompletionStatus("completed");
            window.scormAPI.setSuccessStatus(isPassed ? "passed" : "failed");
            window.scormAPI.setProgress(1.0);
            
            // Set session time
            const sessionTime = this.calculateSessionTime();
            window.scormAPI.setValue("cmi.session_time", sessionTime);
            
            window.scormAPI.commit();
        }
    }

    calculateSessionTime() {
        if (this.startTime && this.endTime) {
            const duration = this.endTime - this.startTime;
            const seconds = Math.floor(duration / 1000);
            const minutes = Math.floor(seconds / 60);
            const hours = Math.floor(minutes / 60);
            
            return `PT${hours}H${minutes % 60}M${seconds % 60}S`;
        }
        return "PT0H0M0S";
    }

    showResults() {
        const isPassed = this.score >= this.data.passingScore;
        
        this.scoreDisplay.innerHTML = `
            <div class="score-circle ${isPassed ? 'passed' : 'failed'}">
                <span class="score-number">${this.score}%</span>
            </div>
            <p>Bạn đã trả lời đúng ${this.getCorrectAnswersCount()} / ${this.data.questions.length} câu hỏi</p>
        `;
        
        this.resultMessage.innerHTML = `
            <div class="result-status ${isPassed ? 'passed' : 'failed'}">
                ${isPassed ? '🎉 Chúc mừng! Bạn đã vượt qua bài kiểm tra.' : '😔 Bạn chưa đạt điểm yêu cầu. Hãy thử lại!'}
            </div>
            <p>Điểm yêu cầu: ${this.data.passingScore}%</p>
        `;
        
        this.showScreen('result');
    }

    getCorrectAnswersCount() {
        let count = 0;
        this.data.questions.forEach((question, index) => {
            if (this.userAnswers[index] === question.correctAnswerIndex) {
                count++;
            }
        });
        return count;
    }

    restartQuiz() {
        this.showScreen('start');
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Global quiz instance
let quiz = null;

function initializeQuiz(quizData) {
    quiz = new Quiz(quizData);
    
    // Initialize SCORM on page load
    if (window.scormAPI) {
        window.scormAPI.initialize();
    }
}

// Cleanup when page unloads
window.addEventListener('beforeunload', function() {
    if (window.scormAPI) {
        window.scormAPI.terminate();
    }
});
