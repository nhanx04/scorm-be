package com.scorm.generator.repository;

import com.scorm.generator.entity.OrganizationActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationActivityRepository extends JpaRepository<OrganizationActivity, Long> {
    List<OrganizationActivity> findByOrganization_OrgIdOrderByCreatedAtDesc(Long orgId);
}
