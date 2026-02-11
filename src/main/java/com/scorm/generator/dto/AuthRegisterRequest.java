package com.scorm.generator.dto;

import lombok.Data;

@Data
public class AuthRegisterRequest {
    private String fname;
    private String lname;
    private String email;
    private String password;
}
