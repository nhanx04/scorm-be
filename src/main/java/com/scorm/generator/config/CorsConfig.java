package com.scorm.generator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // SỬA: Đổi "/api/**" thành "/**" để áp dụng cho mọi controller
                registry.addMapping("/**")
                        // SỬA: Chỉ định rõ Frontend (React/Vite chạy port 3000)
                        .allowedOrigins("http://localhost:3000", "http://localhost:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true) // Cho phép gửi cookie/auth header
                        .maxAge(3600);
            }
        };
    }
}