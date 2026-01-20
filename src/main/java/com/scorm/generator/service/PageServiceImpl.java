package com.scorm.generator.service;

import com.scorm.generator.dto.PageCreateRequest;
import com.scorm.generator.dto.PageResponse;
import com.scorm.generator.dto.PageUpdateRequest;
import com.scorm.generator.entity.Page;
import com.scorm.generator.entity.Section;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.PageRepository;
import com.scorm.generator.repository.SectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;
    private final SectionRepository sectionRepository;

    public PageServiceImpl(PageRepository pageRepository, SectionRepository sectionRepository) {
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
    }

    @Override
    public PageResponse create(Long sectionId, PageCreateRequest request, Authentication authentication) {
        Section section = getOwnedSectionOrThrow(sectionId, authentication);

        Page page = Page.builder()
                .title(request.getTitle())
                .orderIndex(request.getOrderIndex())
                .pageType(request.getPageType())
                .themeOverride(request.getThemeOverride())
                .section(section)
                .build();

        return PageResponse.fromEntity(pageRepository.save(page));
    }

    @Override
    public PageResponse update(Long pageId, PageUpdateRequest request, Authentication authentication) {
        Page page = getOwnedPageOrThrow(pageId, authentication);

        if (request.getTitle() != null) {
            page.setTitle(request.getTitle());
        }
        if (request.getOrderIndex() != null) {
            page.setOrderIndex(request.getOrderIndex());
        }
        if (request.getPageType() != null) {
            page.setPageType(request.getPageType());
        }
        if (request.getThemeOverride() != null) {
            page.setThemeOverride(request.getThemeOverride());
        }

        return PageResponse.fromEntity(pageRepository.save(page));
    }

    @Override
    public void delete(Long pageId, Authentication authentication) {
        Page page = getOwnedPageOrThrow(pageId, authentication);
        pageRepository.delete(page);
    }

    @Override
    public java.util.List<PageResponse> listBySectionId(Long sectionId, Authentication authentication) {
        getOwnedSectionOrThrow(sectionId, authentication);
        return pageRepository.findBySection_SectionIdOrderByOrderIndexAsc(sectionId)
                .stream()
                .map(PageResponse::fromEntity)
                .toList();
    }

    @Override
    public PageResponse getById(Long pageId, Authentication authentication) {
        Page page = getOwnedPageOrThrow(pageId, authentication);
        return PageResponse.fromEntity(page);
    }

    private Section getOwnedSectionOrThrow(Long sectionId, Authentication authentication) {
        if (sectionId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "sectionId is required");
        }

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Section not found"));

        User currentUser = (User) authentication.getPrincipal();
        Long ownerId = section.getCourse() != null && section.getCourse().getUser() != null
                ? section.getCourse().getUser().getUserId()
                : null;

        if (ownerId == null || !ownerId.equals(currentUser.getUserId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to access this section");
        }

        return section;
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
