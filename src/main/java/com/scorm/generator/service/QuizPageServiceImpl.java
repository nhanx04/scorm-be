package com.scorm.generator.service;

import com.scorm.generator.dto.QuizPageCreateRequest;
import com.scorm.generator.dto.QuizPageResponse;
import com.scorm.generator.dto.QuizPageUpdateRequest;
import com.scorm.generator.entity.Page;
import com.scorm.generator.entity.QuizPage;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.PageRepository;
import com.scorm.generator.repository.QuizPageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class QuizPageServiceImpl implements QuizPageService {

    private final QuizPageRepository quizPageRepository;
    private final PageRepository pageRepository;

    public QuizPageServiceImpl(QuizPageRepository quizPageRepository, PageRepository pageRepository) {
        this.quizPageRepository = quizPageRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    public QuizPageResponse create(Long pageId, QuizPageCreateRequest request, Authentication authentication) {
        Page page = getOwnedPageOrThrow(pageId, authentication);

        if (quizPageRepository.existsById(pageId)) {
            throw new AppException(HttpStatus.CONFLICT, "QuizPage already exists for this page");
        }

        QuizPage quizPage = QuizPage.builder()
                .page(page)
                .passingScore(request.getPassingScore())
                .attemptAllowed(request.getAttemptAllowed())
                .build();

        return QuizPageResponse.fromEntity(quizPageRepository.save(quizPage));
    }

    @Override
    public QuizPageResponse update(Long pageId, QuizPageUpdateRequest request, Authentication authentication) {
        getOwnedPageOrThrow(pageId, authentication);

        QuizPage quizPage = quizPageRepository.findById(pageId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "QuizPage not found"));

        if (request.getPassingScore() != null) {
            quizPage.setPassingScore(request.getPassingScore());
        }
        if (request.getAttemptAllowed() != null) {
            quizPage.setAttemptAllowed(request.getAttemptAllowed());
        }

        return QuizPageResponse.fromEntity(quizPageRepository.save(quizPage));
    }

    @Override
    public QuizPageResponse getByPageId(Long pageId, Authentication authentication) {
        getOwnedPageOrThrow(pageId, authentication);

        QuizPage quizPage = quizPageRepository.findById(pageId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "QuizPage not found"));

        return QuizPageResponse.fromEntity(quizPage);
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
