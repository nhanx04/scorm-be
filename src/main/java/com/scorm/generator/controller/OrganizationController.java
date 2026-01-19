package com.scorm.generator.controller;

import com.scorm.generator.dto.OrganizationCreateRequest;
import com.scorm.generator.dto.OrganizationInviteDecisionRequest;
import com.scorm.generator.dto.OrganizationInviteRequest;
import com.scorm.generator.dto.OrganizationMemberDto;
import com.scorm.generator.dto.OrganizationResponse;
import com.scorm.generator.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(
            @RequestBody OrganizationCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(organizationService.createOrganization(request, authentication));
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrganizationResponse>> myOrganizations(Authentication authentication) {
        return ResponseEntity.ok(organizationService.listMyOrganizations(authentication));
    }

    @GetMapping("/{orgId}/members")
    public ResponseEntity<List<OrganizationMemberDto>> members(
            @PathVariable Long orgId,
            Authentication authentication) {
        return ResponseEntity.ok(organizationService.listMembers(orgId, authentication));
    }

    @PostMapping("/{orgId}/invite")
    public ResponseEntity<Void> invite(
            @PathVariable Long orgId,
            @RequestBody OrganizationInviteRequest request,
            Authentication authentication) {
        organizationService.inviteMember(orgId, request, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orgId}/invite/respond")
    public ResponseEntity<Void> respondToInvite(
            @PathVariable Long orgId,
            @RequestBody OrganizationInviteDecisionRequest request,
            Authentication authentication) {
        boolean accept = request.getAccept() != null && request.getAccept();
        organizationService.respondToInvite(orgId, accept, authentication);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{orgId}/invite/{userId}")
    public ResponseEntity<Void> revokeInvite(
            @PathVariable Long orgId,
            @PathVariable Long userId,
            Authentication authentication) {
        organizationService.revokeInvite(orgId, userId, authentication);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{orgId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long orgId,
            @PathVariable Long userId,
            Authentication authentication) {
        organizationService.removeMember(orgId, userId, authentication);
        return ResponseEntity.ok().build();
    }
}
