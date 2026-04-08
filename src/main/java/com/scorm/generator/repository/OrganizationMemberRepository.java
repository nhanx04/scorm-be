package com.scorm.generator.repository;

import com.scorm.generator.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    Optional<OrganizationMember> findByOrganization_OrgIdAndUser_UserId(Long orgId, Long userId);

    boolean existsByOrganization_OrgIdAndUser_UserId(Long orgId, Long userId);

    void deleteByOrganization_OrgIdAndUser_UserId(Long orgId, Long userId);

    void deleteByOrganization_OrgId(Long orgId);
}
