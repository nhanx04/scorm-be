package com.scorm.generator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "membership")
public class Membership {

    @EmbeddedId
    private MembershipId id;

    @MapsId("orgId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orgid", referencedColumnName = "orgid")
    private Organization organization;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userid", referencedColumnName = "userid")
    private User user;

    @Column(name = "org_role")
    private String orgRole;

    @Column(name = "invited_at")
    private OffsetDateTime invitedAt;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

    @Column(name = "status")
    private String status;
}

