package com.scorm.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String type;
    private Long userId;
    private String email;
    private String fullName;

    public AuthResponse(String token, Long userId, String email, String fullName) {
        this.token = token;
        this.type = "Bearer";
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
    }
}

