package com.scorm.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberDto {
    private Long userId;
    private String email;
    private String fname;
    private String minit;
    private String lname;

    private String role;
    private String status;
    private OffsetDateTime invitedAt;
    private OffsetDateTime joinedAt;
}

