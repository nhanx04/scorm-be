package com.scorm.generator.controller;

import com.scorm.generator.dto.OrganizationCreateRequest;
import com.scorm.generator.dto.OrganizationInviteDecisionRequest;
import com.scorm.generator.dto.OrganizationInviteRequest;
import com.scorm.generator.dto.OrganizationMemberDto;
import com.scorm.generator.dto.OrganizationResponse;
import com.scorm.generator.dto.ResourceResponse;
import com.scorm.generator.dto.ShareResourceRequest;
import com.scorm.generator.entity.OrganizationResourceType;
import com.scorm.generator.service.OrganizationResourceService;
import com.scorm.generator.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationResourceService organizationResourceService;

    public OrganizationController(
            OrganizationService organizationService,
            OrganizationResourceService organizationResourceService) {
        this.organizationService = organizationService;
        this.organizationResourceService = organizationResourceService;
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

    @PostMapping("/{orgId}/resources")
    public ResponseEntity<ResourceResponse> shareResource(
            @PathVariable Long orgId,
            @RequestBody ShareResourceRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(organizationResourceService.shareResource(orgId, request, authentication));
    }

    @GetMapping("/{orgId}/resources")
    public ResponseEntity<List<ResourceResponse>> getResources(
            @PathVariable Long orgId,
            @RequestParam(required = false) OrganizationResourceType type,
            Authentication authentication) {
        return ResponseEntity.ok(organizationResourceService.getResources(orgId, type, authentication));
    }

    @GetMapping("/{orgId}/activities")
    public ResponseEntity<List<com.scorm.generator.dto.OrganizationActivityResponse>> getActivities(
            @PathVariable Long orgId,
            Authentication authentication) {
        return ResponseEntity.ok(organizationResourceService.getActivities(orgId, authentication));
    }

    @DeleteMapping("/{orgId}/resources/{resourceId}")
    public ResponseEntity<Void> deleteResource(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            Authentication authentication) {
        organizationResourceService.deleteResource(orgId, resourceId, authentication);
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

    @PatchMapping("/{orgId}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @PathVariable Long orgId,
            @RequestBody OrganizationCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(organizationService.updateOrganization(orgId, request, authentication));
    }

    @DeleteMapping({ "/{orgId}", "/{orgId}/" })
    public ResponseEntity<Void> deleteOrganization(
            @PathVariable Long orgId,
            Authentication authentication) {
        organizationService.deleteOrganization(orgId, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orgId}/delete")
    public ResponseEntity<Void> deleteOrganizationByPost(
            @PathVariable Long orgId,
            Authentication authentication) {
        organizationService.deleteOrganization(orgId, authentication);
        return ResponseEntity.ok().build();
    }
}
