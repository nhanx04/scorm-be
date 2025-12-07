package com.scorm.generator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URL;

@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Uploads a file to Amazon S3 and returns its public URL.
     *
     * @param fileData    The file content as a byte array.
     * @param fileName    The key (path and name) for the file in the bucket.
     * @param contentType The MIME type of the file (e.g., "application/zip").
     * @return The public URL of the uploaded file.
     */
    public String uploadFile(byte[] fileData, String fileName, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));

        // Return the public URL of the object
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(fileName)).toExternalForm();
    }

    /**
     * Deletes a file from Amazon S3.
     *
     * @param fileUrl The public URL of the file to delete.
     */
    public void deleteFileFromUrl(String fileUrl) {
        try {
            // Extract the key from the URL
            String key = new URL(fileUrl).getPath().substring(1);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            // Log the error, e.g., using a logger
            System.err.println("Error deleting file from S3: " + e.getMessage());
        }
    }
}

