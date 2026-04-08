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
@Table(name = "organization_member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_org_member_org_user", columnNames = { "orgid", "userid" })
})
public class OrganizationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orgid", referencedColumnName = "orgid", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userid", referencedColumnName = "userid", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private OrganizationMemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }
}

