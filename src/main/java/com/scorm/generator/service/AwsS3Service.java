package com.scorm.generator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Service
public class AwsS3Service {

    private final S3Client s3;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public AwsS3Service() {
        // Uses default credentials provider chain:
        // - AWS_PROFILE / ~/.aws/credentials (recommended for local)
        // - env vars AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY
        // - etc.
        this.s3 = S3Client.builder().build();
    }

    /**
     * Upload file bytes to S3 and return a public URL.
     *
     * NOTE: This only works as a "public" URL if:
     * - the object is public (ACL/policy), OR
     * - the bucket is public, OR
     * - you serve it via CloudFront.
     */
    public String uploadFile(byte[] fileData, String key, String contentType) {
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(cleanKey)
                .contentType(contentType)
                .build();

        s3.putObject(putReq, RequestBody.fromBytes(fileData));

        // Build standard S3 virtual-hosted style URL
        // https://{bucket}.s3.{region}.amazonaws.com/{key}
        String resolvedRegion = (region == null || region.isBlank()) ? Region.AWS_GLOBAL.id() : region;
        return "https://" + bucketName + ".s3." + resolvedRegion + ".amazonaws.com/" + cleanKey;
    }

    public void deleteFileFromUrl(String fileUrl) {
        // Accept either full URL or s3 key (best effort)
        String key = fileUrl;
        try {
            if (fileUrl != null && (fileUrl.startsWith("http://") || fileUrl.startsWith("https://"))) {
                URI uri = URI.create(fileUrl);
                String path = uri.getPath();
                if (path != null && path.startsWith("/")) {
                    key = path.substring(1);
                }
            }
        } catch (Exception ignored) {
        }

        if (key == null || key.isBlank())
            return;

        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }
}