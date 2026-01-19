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
@Table(name = "organization")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orgid")
    private Long orgId;

    @Column(name = "org_name")
    private String orgName;

    @Column(name = "description")
    private String description;

    @Column(name = "max_authors")
    private Integer maxAuthors;

    @Column(name = "logo_media_id")
    private Long logoMediaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_ownerid", referencedColumnName = "userid")
    private User owner;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_userid", referencedColumnName = "userid")
    private User orgUser;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}

