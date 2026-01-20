package com.scorm.generator.service;

import com.scorm.generator.dto.ContentBlockCreateRequest;
import com.scorm.generator.dto.ContentBlockResponse;
import com.scorm.generator.dto.ContentBlockUpdateRequest;
import com.scorm.generator.entity.ContentBlock;
import com.scorm.generator.entity.ContentPage;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.ContentBlockRepository;
import com.scorm.generator.repository.ContentPageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentBlockServiceImpl implements ContentBlockService {

    private final ContentBlockRepository contentBlockRepository;
    private final ContentPageRepository contentPageRepository;

    public ContentBlockServiceImpl(ContentBlockRepository contentBlockRepository,
            ContentPageRepository contentPageRepository) {
        this.contentBlockRepository = contentBlockRepository;
        this.contentPageRepository = contentPageRepository;
    }

    @Override
    public ContentBlockResponse create(Long contentPageId, ContentBlockCreateRequest request,
            Authentication authentication) {
        ContentPage contentPage = getOwnedContentPageOrThrow(contentPageId, authentication);

        ContentBlock block = ContentBlock.builder()
                .contentPage(contentPage)
                .orderIndex(request.getOrderIndex())
                .textHtml(request.getTextHtml())
                .build();

        return ContentBlockResponse.fromEntity(contentBlockRepository.save(block));
    }

    @Override
    public ContentBlockResponse update(Long blockId, ContentBlockUpdateRequest request, Authentication authentication) {
        ContentBlock block = getOwnedBlockOrThrow(blockId, authentication);

        if (request.getOrderIndex() != null) {
            block.setOrderIndex(request.getOrderIndex());
        }
        if (request.getTextHtml() != null) {
            block.setTextHtml(request.getTextHtml());
        }

        return ContentBlockResponse.fromEntity(contentBlockRepository.save(block));
    }

    @Override
    public void delete(Long blockId, Authentication authentication) {
        ContentBlock block = getOwnedBlockOrThrow(blockId, authentication);
        contentBlockRepository.delete(block);
    }

    @Override
    public ContentBlockResponse getById(Long blockId, Authentication authentication) {
        ContentBlock block = getOwnedBlockOrThrow(blockId, authentication);
        return ContentBlockResponse.fromEntity(block);
    }

    @Override
    public List<ContentBlockResponse> getByContentPageId(Long contentPageId, Authentication authentication) {
        getOwnedContentPageOrThrow(contentPageId, authentication);

        return contentBlockRepository.findAll().stream()
                .filter(b -> b.getContentPage() != null && contentPageId.equals(b.getContentPage().getPageId()))
                .map(ContentBlockResponse::fromEntity)
                .toList();
    }

    private ContentPage getOwnedContentPageOrThrow(Long contentPageId, Authentication authentication) {
        if (contentPageId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "contentPageId is required");
        }

        ContentPage contentPage = contentPageRepository.findById(contentPageId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "ContentPage not found"));

        User currentUser = (User) authentication.getPrincipal();
        Long ownerId = contentPage.getPage() != null
                && contentPage.getPage().getSection() != null
                && contentPage.getPage().getSection().getCourse() != null
                && contentPage.getPage().getSection().getCourse().getUser() != null
                        ? contentPage.getPage().getSection().getCourse().getUser().getUserId()
                        : null;

        if (ownerId == null || !ownerId.equals(currentUser.getUserId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to access this content page");
        }

        return contentPage;
    }

    private ContentBlock getOwnedBlockOrThrow(Long blockId, Authentication authentication) {
        if (blockId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "blockId is required");
        }

        ContentBlock block = contentBlockRepository.findById(blockId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "ContentBlock not found"));

        User currentUser = (User) authentication.getPrincipal();
        Long ownerId = block.getContentPage() != null
                && block.getContentPage().getPage() != null
                && block.getContentPage().getPage().getSection() != null
                && block.getContentPage().getPage().getSection().getCourse() != null
                && block.getContentPage().getPage().getSection().getCourse().getUser() != null
                        ? block.getContentPage().getPage().getSection().getCourse().getUser().getUserId()
                        : null;

        if (ownerId == null || !ownerId.equals(currentUser.getUserId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to access this content block");
        }

        return block;
    }
}
