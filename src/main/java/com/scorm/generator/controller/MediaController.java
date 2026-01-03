package com.scorm.generator.controller;

import com.scorm.generator.service.AwsS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
// CORS is configured globally in CorsConfig
// @CrossOrigin removed to avoid origins="*" + allowCredentials(true) conflict
public class MediaController {

    private final AwsS3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Generate a unique file name to avoid conflicts
            String fileName = "media/" + UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

            // Upload the file and get its public URL
            String fileUrl = s3Service.uploadFile(file.getBytes(), fileName, file.getContentType());

            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to upload file: " + e.getMessage());
        }
    }
}
