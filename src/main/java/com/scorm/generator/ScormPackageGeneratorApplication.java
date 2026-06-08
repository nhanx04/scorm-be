package com.scorm.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // cho phép ingest RAG chạy nền (CourseRagService.ingestAsync)
public class ScormPackageGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScormPackageGeneratorApplication.class, args);
    }

}

