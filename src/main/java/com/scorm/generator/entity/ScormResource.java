package com.scorm.generator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scorm_resource")
public class ScormResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scorm_resourceid")
    private Long scormResourceId;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "href")
    private String href;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "scorm_type")
    private String scormType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_packageid", referencedColumnName = "scorm_packageid")
    private ScormPackage scormPackage;
}

