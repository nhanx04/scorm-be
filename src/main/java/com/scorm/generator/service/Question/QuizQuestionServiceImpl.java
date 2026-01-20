package com.scorm.generator.service.Question;

import com.scorm.generator.entity.Page;
import com.scorm.generator.entity.User;
import com.scorm.generator.entity.Question.Question;
import com.scorm.generator.entity.Question.QuestionOfQuiz;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.PageRepository;
import com.scorm.generator.repository.Question.QuestionOfQuizRepository;
import com.scorm.generator.repository.Question.QuestionRepository;
import com.scorm.generator.repository.QuizPageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizQuestionServiceImpl implements QuizQuestionService {

    private final PageRepository pageRepository;
    private final QuizPageRepository quizPageRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOfQuizRepository questionOfQuizRepository;

    public QuizQuestionServiceImpl(PageRepository pageRepository, QuizPageRepository quizPageRepository,
            QuestionRepository questionRepository, QuestionOfQuizRepository questionOfQuizRepository) {
        this.pageRepository = pageRepository;
        this.quizPageRepository = quizPageRepository;
        this.questionRepository = questionRepository;
        this.questionOfQuizRepository = questionOfQuizRepository;
    }

    @Override
    @Transactional
    public void attach(Long pageId, Long questionId, Authentication authentication) {
        Page page = getOwnedPageOrThrow(pageId, authentication);

        if (!quizPageRepository.existsById(pageId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "QuizPage not found");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Question not found"));

        if (questionOfQuizRepository.existsById_QuizQuestionIdAndId_QuizPageId(questionId, pageId)) {
            return;
        }

        questionOfQuizRepository.save(QuestionOfQuiz.builder()
                .id(new QuestionOfQuiz.QuestionOfQuizId(question.getQuestionId(), page.getPageId()))
                .build());
    }

    @Override
    @Transactional
    public void detach(Long pageId, Long questionId, Authentication authentication) {
        getOwnedPageOrThrow(pageId, authentication);

        questionOfQuizRepository.deleteById_QuizQuestionIdAndId_QuizPageId(questionId, pageId);
    }

    private Page getOwnedPageOrThrow(Long pageId, Authentication authentication) {
        if (pageId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "pageId is required");
        }

        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Page not found"));

        User currentUser = (User) authentication.getPrincipal();
        Long ownerId = page.getSection() != null && page.getSection().getCourse() != null
                && page.getSection().getCourse().getUser() != null
                        ? page.getSection().getCourse().getUser().getUserId()
                        : null;

        if (ownerId == null || !ownerId.equals(currentUser.getUserId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to access this page");
        }

        return page;
    }
}

