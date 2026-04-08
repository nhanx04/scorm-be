package com.scorm.generator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public S3StorageService(S3Client s3Client, @Value("${app.r2.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public String uploadFile(String keyPrefix, MultipartFile file) {
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        try {
            return uploadBytes(keyPrefix, file.getOriginalFilename(), file.getBytes(), contentType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read upload file", e);
        }
    }

    public String uploadBytes(String keyPrefix, String originalFilename, byte[] content, String contentType) {
        String key = buildKey(keyPrefix, originalFilename);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType == null ? "application/octet-stream" : contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        return key;
    }

    public void deleteByKey(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    private String buildKey(String keyPrefix, String originalFilename) {
        String safeName = originalFilename == null ? "file" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return keyPrefix + "/" + UUID.randomUUID() + "-" + safeName;
    }
}
