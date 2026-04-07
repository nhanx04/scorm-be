package com.scorm.generator.repository;

import com.scorm.generator.entity.Membership;
import com.scorm.generator.entity.MembershipId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, MembershipId> {
    Optional<Membership> findByOrganization_OrgIdAndUser_UserId(Long orgId, Long userId);

    boolean existsByOrganization_OrgIdAndUser_UserId(Long orgId, Long userId);

    List<Membership> findByUser_UserId(Long userId);

    List<Membership> findByOrganization_OrgId(Long orgId);

    void deleteByOrganization_OrgId(Long orgId);
}
