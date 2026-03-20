package com.scorm.generator.service;

import com.scorm.generator.dto.ContentPageCreateRequest;
import com.scorm.generator.dto.ContentPageResponse;
import com.scorm.generator.dto.ContentPageUpdateRequest;
import com.scorm.generator.entity.ContentPage;
import com.scorm.generator.entity.Page;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.ContentPageRepository;
import com.scorm.generator.repository.PageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ContentPageServiceImpl implements ContentPageService {

    private final ContentPageRepository contentPageRepository;
    private final PageRepository pageRepository;

    public ContentPageServiceImpl(ContentPageRepository contentPageRepository, PageRepository pageRepository) {
        this.contentPageRepository = contentPageRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    public ContentPageResponse create(Long pageId, ContentPageCreateRequest request, Authentication authentication) {
        Page page = getOwnedPageOrThrow(pageId, authentication);

        if (contentPageRepository.existsById(pageId)) {
            throw new AppException(HttpStatus.CONFLICT, "ContentPage already exists for this page");
        }

        ContentPage contentPage = ContentPage.builder()
                .page(page)
                .layoutMode(request.getLayoutMode())
                .layoutType(request.getLayoutType())
                .layoutMeta(request.getLayoutMeta())
                .build();

        return ContentPageResponse.fromEntity(contentPageRepository.save(contentPage));
    }

    @Override
    public ContentPageResponse update(Long pageId, ContentPageUpdateRequest request, Authentication authentication) {
        getOwnedPageOrThrow(pageId, authentication);

        ContentPage contentPage = contentPageRepository.findById(pageId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "ContentPage not found"));

        if (request.getLayoutMode() != null) {
            contentPage.setLayoutMode(request.getLayoutMode());
        }
        if (request.getLayoutType() != null) {
            contentPage.setLayoutType(request.getLayoutType());
        }
        if (request.getLayoutMeta() != null) {
            contentPage.setLayoutMeta(request.getLayoutMeta());
        }

        return ContentPageResponse.fromEntity(contentPageRepository.save(contentPage));
    }

    @Override
    public ContentPageResponse getByPageId(Long pageId, Authentication authentication) {
        getOwnedPageOrThrow(pageId, authentication);

        ContentPage contentPage = contentPageRepository.findById(pageId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "ContentPage not found"));

        return ContentPageResponse.fromEntity(contentPage);
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
