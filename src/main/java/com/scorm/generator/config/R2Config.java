package com.scorm.generator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class R2Config {

        @Bean
        @ConditionalOnProperty(prefix = "app.r2", name = { "access-key-id", "secret-access-key" })
        public S3Client s3Client(
                        @Value("${app.r2.endpoint}") String endpoint,
                        @Value("${app.r2.access-key-id}") String accessKeyId,
                        @Value("${app.r2.secret-access-key}") String secretAccessKey) {
                return S3Client.builder()
                                .endpointOverride(URI.create(endpoint))
                                .region(Region.of("auto"))
                                .credentialsProvider(StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                                .build();
        }
}
