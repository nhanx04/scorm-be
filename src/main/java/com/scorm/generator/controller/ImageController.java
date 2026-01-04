package com.scorm.generator.controller;

import com.scorm.generator.dto.ImageResourceDTO;
import com.scorm.generator.entity.Image;
import com.scorm.generator.service.AwsS3Service;
import com.scorm.generator.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final AwsS3Service awsS3Service;

    @GetMapping("/images")
    public ResponseEntity<List<ImageResourceDTO>> getAll() {
        List<ImageResourceDTO> list = imageService.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/upload")
    public ResponseEntity<ImageResourceDTO> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        // Generate key và upload
        String key = "media/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        String url = awsS3Service.uploadFile(file.getBytes(), key, file.getContentType());

        Image saved = imageService.save(Image.builder()
                .name(file.getOriginalFilename())
                .url(url)
                .size(file.getSize())
                .build());
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        imageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ImageResourceDTO toDto(Image img) {
        return ImageResourceDTO.builder()
                .id(img.getId())
                .name(img.getName())
                .url(img.getUrl())
                .size(img.getSize())
                .width(img.getWidth())
                .height(img.getHeight())
                .createdAt(img.getCreatedAt())
                .build();
    }
}

