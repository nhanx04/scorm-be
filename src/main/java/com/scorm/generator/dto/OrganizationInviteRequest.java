package com.scorm.generator.dto;

import lombok.Data;

@Data
public class OrganizationInviteRequest {
    private String email;
    private String role;
}

