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
@Table(name = "my_library")
public class MyLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "libraryid")
    private Long libraryId;

    @Column(name = "library_name")
    private String libraryName;

    @Column(name = "description")
    private String description;

    @Column(name = "scope_type")
    private String scopeType;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lib_ownerid", referencedColumnName = "userid")
    private User owner;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

