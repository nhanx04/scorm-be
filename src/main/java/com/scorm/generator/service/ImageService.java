package com.scorm.generator.service;

import com.scorm.generator.entity.Image;
import com.scorm.generator.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;

    @Transactional
    public Image save(Image image) {
        return imageRepository.save(image);
    }

    @Transactional(readOnly = true)
    public List<Image> findAll() {
        return imageRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void delete(Long id) {
        imageRepository.deleteById(id);
    }
}

