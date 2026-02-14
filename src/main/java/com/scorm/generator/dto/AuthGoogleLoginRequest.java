package com.scorm.generator.dto;

import lombok.Data;

@Data
public class AuthGoogleLoginRequest {
    // Access Token từ Google (Frontend gửi lên)
    private String token;
}