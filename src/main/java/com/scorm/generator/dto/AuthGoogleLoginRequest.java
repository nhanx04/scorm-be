package com.scorm.generator.dto;

public class AuthGoogleLoginRequest {
    private String token; // Token ID nhận được từ Google ở phía Frontend

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
