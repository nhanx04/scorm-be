package com.scorm.generator.repository;

import com.scorm.generator.entity.OrganizationResource;
import com.scorm.generator.entity.OrganizationResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationResourceRepository extends JpaRepository<OrganizationResource, Long> {

    List<OrganizationResource> findByOrganization_OrgIdOrderByCreatedAtDesc(Long orgId);

    List<OrganizationResource> findByOrganization_OrgIdAndTypeOrderByCreatedAtDesc(Long orgId, OrganizationResourceType type);

    Optional<OrganizationResource> findByIdAndOrganization_OrgId(Long id, Long orgId);
}

