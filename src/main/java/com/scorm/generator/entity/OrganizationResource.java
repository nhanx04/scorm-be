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
@Table(name = "organization_resource")
public class OrganizationResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orgid", referencedColumnName = "orgid", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_by_userid", referencedColumnName = "userid", nullable = false)
    private User sharedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private OrganizationResourceType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mediaid", referencedColumnName = "mediaid")
    private MediaAsset mediaAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courseid", referencedColumnName = "courseid")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "libraryid", referencedColumnName = "libraryid")
    private MyLibrary folder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}

