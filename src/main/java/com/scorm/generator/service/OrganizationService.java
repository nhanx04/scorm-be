package com.scorm.generator.service;

import com.scorm.generator.dto.OrganizationCreateRequest;
import com.scorm.generator.dto.OrganizationInviteRequest;
import com.scorm.generator.dto.OrganizationMemberDto;
import com.scorm.generator.dto.OrganizationResponse;
import com.scorm.generator.entity.Membership;
import com.scorm.generator.entity.MembershipId;
import com.scorm.generator.entity.Organization;
import com.scorm.generator.entity.User;
import com.scorm.generator.repository.MembershipRepository;
import com.scorm.generator.repository.OrganizationRepository;
import com.scorm.generator.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public OrganizationResponse createOrganization(OrganizationCreateRequest request, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        if (request.getOrgName() == null || request.getOrgName().isBlank()) {
            throw new RuntimeException("orgName is required");
        }

        Organization org = Organization.builder()
                .orgName(request.getOrgName())
                .description(request.getDescription())
                .maxAuthors(request.getMaxAuthors())
                .logoMediaId(request.getLogoMediaId())
                .owner(currentUser)
                .orgUser(currentUser)
                .build();

        Organization saved = organizationRepository.save(org);

        // Owner becomes a member
        Membership ownerMembership = Membership.builder()
                .id(new MembershipId(saved.getOrgId(), currentUser.getUserId()))
                .organization(saved)
                .user(currentUser)
                .orgRole("OWNER")
                .joinedAt(OffsetDateTime.now())
                .status("ACTIVE")
                .build();
        membershipRepository.save(ownerMembership);

        return toResponse(saved);
    }

    public void inviteMember(Long orgId, OrganizationInviteRequest request, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        assertOwner(org, currentUser);

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("email is required");
        }

        User invitedUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invited user not found"));

        if (membershipRepository.existsByOrganization_OrgIdAndUser_UserId(orgId, invitedUser.getUserId())) {
            throw new RuntimeException("User already in organization");
        }

        String role = (request.getRole() == null || request.getRole().isBlank()) ? "MEMBER" : request.getRole();

        Membership membership = Membership.builder()
                .id(new MembershipId(orgId, invitedUser.getUserId()))
                .organization(org)
                .user(invitedUser)
                .orgRole(role)
                .invitedAt(OffsetDateTime.now())
                .status("INVITED")
                .build();

        membershipRepository.save(membership);
    }

    public void respondToInvite(Long orgId, boolean accept, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        Membership membership = membershipRepository
                .findByOrganization_OrgIdAndUser_UserId(orgId, currentUser.getUserId())
                .orElseThrow(() -> new RuntimeException("Membership not found"));

        if (!"INVITED".equalsIgnoreCase(membership.getStatus())) {
            throw new RuntimeException("Invite is not pending");
        }

        if (accept) {
            membership.setStatus("ACTIVE");
            membership.setJoinedAt(OffsetDateTime.now());
        } else {
            membership.setStatus("REJECTED");
        }

        membershipRepository.save(membership);
    }

    public List<OrganizationResponse> listMyOrganizations(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        return membershipRepository.findByUser_UserId(currentUser.getUserId()).stream()
                .map(Membership::getOrganization)
                .map(this::toResponse)
                .toList();
    }

    public List<OrganizationMemberDto> listMembers(Long orgId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        Membership myMembership = membershipRepository
                .findByOrganization_OrgIdAndUser_UserId(orgId, currentUser.getUserId())
                .orElseThrow(() -> new RuntimeException("Not a member of this organization"));

        if (!"ACTIVE".equalsIgnoreCase(myMembership.getStatus())
                && !"INVITED".equalsIgnoreCase(myMembership.getStatus())) {
            throw new RuntimeException("Not authorized");
        }

        return membershipRepository.findByOrganization_OrgId(orgId).stream()
                .map(this::toMemberDto)
                .toList();
    }

    public void revokeInvite(Long orgId, Long userId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        assertOwner(org, currentUser);

        Membership membership = membershipRepository.findByOrganization_OrgIdAndUser_UserId(orgId, userId)
                .orElseThrow(() -> new RuntimeException("Membership not found"));

        if (!"INVITED".equalsIgnoreCase(membership.getStatus())) {
            throw new RuntimeException("Can only revoke pending invites");
        }

        membershipRepository.delete(membership);
    }

    public void removeMember(Long orgId, Long userId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        assertOwner(org, currentUser);

        if (org.getOwner() != null && org.getOwner().getUserId().equals(userId)) {
            throw new RuntimeException("Cannot remove organization owner");
        }

        Membership membership = membershipRepository.findByOrganization_OrgIdAndUser_UserId(orgId, userId)
                .orElseThrow(() -> new RuntimeException("Membership not found"));

        if (!"ACTIVE".equalsIgnoreCase(membership.getStatus())) {
            throw new RuntimeException("Can only remove active members");
        }

        membershipRepository.delete(membership);
    }

    private void assertOwner(Organization org, User currentUser) {
        if (org.getOwner() == null || !org.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Only organization owner can perform this action");
        }
    }

    private OrganizationMemberDto toMemberDto(Membership membership) {
        User u = membership.getUser();
        return OrganizationMemberDto.builder()
                .userId(u != null ? u.getUserId() : null)
                .email(u != null ? u.getEmail() : null)
                .fname(u != null ? u.getFname() : null)
                .lname(u != null ? u.getLname() : null)
                .role(membership.getOrgRole())
                .status(membership.getStatus())
                .invitedAt(membership.getInvitedAt())
                .joinedAt(membership.getJoinedAt())
                .build();
    }

    private OrganizationResponse toResponse(Organization org) {
        return OrganizationResponse.builder()
                .orgId(org.getOrgId())
                .orgName(org.getOrgName())
                .description(org.getDescription())
                .maxAuthors(org.getMaxAuthors())
                .logoMediaId(org.getLogoMediaId())
                .ownerId(org.getOwner() != null ? org.getOwner().getUserId() : null)
                .build();
    }
}
